package controllers

import actions._
import com.typesafe.scalalogging.LazyLogging
import configuration.Config
import models.Behaviour
import monitoring.Metrics
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContent, Controller}
import play.filters.cors.CORSActionBuilder
import services.IdentityService.IdentityId
import services.{AuthenticationService, IdentityAuthService, SQSAbandonedCartEmailService}

class BehaviourController extends Controller with LazyLogging {

  lazy val corsFilter = CORSActionBuilder(Config.corsConfig)
  lazy val backendAction = corsFilter andThen BackendFromCookieAction
  lazy val authenticationService: AuthenticationService = IdentityAuthService
  lazy val metrics = Metrics("BehaviourController")

  def capture() = BackendFromCookieAction.async { implicit request =>
    awsAction(request, "upsert")
  }

  def remove = BackendFromCookieAction.async { implicit request =>
    awsAction(request, "delete")
  }

  def sendCartReminderEmail = backendAction.async { implicit request =>
    val receivedBehaviour = behaviourFromBody(request.body.asJson)
    for {
      paidTier <- request.touchpoint.attrService.get(receivedBehaviour.userId).map(_.exists(_.isPaidTier))
      user <- request.touchpoint.identityService.user(IdentityId(receivedBehaviour.userId))
      emailAddress = (user \ "user" \ "primaryEmailAddress").asOpt[String]
      displayName = (user \ "user" \ "publicFields" \ "displayName").asOpt[String]
      gnmMarketingPrefs = true // TODO - needs an Identity API PR to send statusFields.receiveGnmMarketing in above user <- ... call
    } yield {
        val msg = if (paidTier || !gnmMarketingPrefs) {
          val reason = "user " + (if (paidTier) "has become a paying member" else "is not accepting marketing emails")
          logger.info(s"NOT queuing an email because $reason")
          request.touchpoint.behaviourService.delete(receivedBehaviour.userId)
          logger.info(s"deleted reminder record")
          reason
        } else {
          emailAddress.map{ addr =>
            val queueResult = queueAbandonedCartEmail(addr, displayName)
            request.touchpoint.behaviourService.set(receivedBehaviour.copy(emailed = Some(queueResult)))
            "email " + (if (queueResult) "queued" else "queue failed")
          }.getOrElse("No email sent - email address not available")
        }
      logger.info(s"### $msg")
      Ok(msg)
    }
  }

  private def queueAbandonedCartEmail(emailAddress: String, displayName: Option[String]) = {
    logger.info(s"queuing email to ${emailAddress.take(emailAddress.indexOf("@")-1)}...")
    val testEmailAddress = "justin.pinner@theguardian.com"
    val recipient = Json.obj(
      "Address" -> testEmailAddress,
      "FirstName" -> displayName.flatMap{_.split(" ").headOption}.getOrElse[String](""),
      "LastName" -> displayName.flatMap{_.split(" ").tail.headOption}.getOrElse[String]("")
    )
    val completionLink = Json.obj(
      "CompletionLink" -> "https://membership.theguardian.com/supporter"
    )
    // TODO: remove this completely and use emailAddress in place of testEmailAddress in recipient when live!
    val ignoredTestData = Json.obj(
      "DisplayName" -> displayName.getOrElse[String](""),
      "EmailAddress" -> emailAddress
    )
    val msg = Json.obj(
      "To" -> recipient,
      "Message" -> completionLink,
      "TestData" -> ignoredTestData,
      "DataExtensionName" -> "supporter-abandoned-checkout-email"
    )

    try {
      SQSAbandonedCartEmailService.sendMessage(msg.toString())
      true
    } catch {
      case e: Exception => logger.warn(s"email queue operation failed: ${e}")
      false
    }
  }

  private def awsAction(request: BackendRequest[AnyContent], action: String) = {
    val receivedBehaviour = behaviourFromBody(request.body.asJson)
    action match {
      case "upsert" =>
        val updateResult = for {
          current <- request.touchpoint.behaviourService.get(receivedBehaviour.userId)
          upserted = current.map { bhv =>
            bhv.copy(
              activity = receivedBehaviour.activity.orElse(bhv.activity),
              lastObserved = receivedBehaviour.lastObserved.orElse(bhv.lastObserved),
              note = receivedBehaviour.note.orElse(bhv.note),
              emailed = receivedBehaviour.emailed.orElse(bhv.emailed))
          }.getOrElse(receivedBehaviour)
          res <- request.touchpoint.behaviourService.set(upserted)
        } yield res
        updateResult.map { r =>
          logger.info(s"upserted ${receivedBehaviour.userId}")
          Ok(Behaviour.asEmptyJson)
        }
      case _ =>
        val deleteResult = for {
          deleteItemResult <- request.touchpoint.behaviourService.delete(receivedBehaviour.userId)
        } yield deleteItemResult
        deleteResult.map { r =>
          logger.info(s"removed ${receivedBehaviour.userId}")
          Ok(Behaviour.asEmptyJson)
        }
    }
  }

  private def behaviourFromBody(requestBodyJson: Option[JsValue]): Behaviour = {
    requestBodyJson.map { jval =>
      val id = (jval \ "userId").as[String]
      val activity = (jval \ "activity").asOpt[String]
      val lastObserved = (jval \ "lastObserved").asOpt[String]
      val note = (jval \ "note").asOpt[String]
      val emailed = (jval \ "emailed").asOpt[Boolean]
      Behaviour(id, activity, lastObserved, note, emailed)
    }.getOrElse(Behaviour.empty)
  }

}
