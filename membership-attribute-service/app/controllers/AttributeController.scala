package controllers

import com.gu.memsub._
import com.gu.memsub.services.{PaymentService, SubscriptionService}
import com.gu.salesforce.ContactRepository
import configuration.Config
import models.ApiError._
import models.ApiErrors._
import models.Features._
import actions._
import models._
import monitoring.CloudWatch
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.api.mvc.{Action, Result}
import play.filters.cors.CORSActionBuilder
import _root_.services.{AttributeService, AuthenticationService, IdentityAuthService}
import models.AccountDetails._
import scala.concurrent.Future
import scalaz.{Monad, MonadTrans, OptionT}
import scalaz.std.scalaFuture._
import play.api.mvc.Results.{Ok, Forbidden}
import json.PaymentCardUpdateResultWriters._

class AttributeController(attrService: AttributeService,
                          contactRepo: ContactRepository,
                          subscriptionService: SubscriptionService,
                          paymentService: PaymentService) {
  
  lazy val authenticationService: AuthenticationService = IdentityAuthService
  lazy val mmaCorsFilter = CORSActionBuilder(Config.mmaCorsConfig)

  lazy val corsFilter = CORSActionBuilder(Config.corsConfig)
  lazy val corsCardFilter = CORSActionBuilder(Config.mmaCardCorsConfig)
  lazy val backendAction = corsFilter andThen Action
  lazy val mmaAction = mmaCorsFilter andThen Action
  lazy val mmaCardAction = corsCardFilter andThen Action
  lazy val metrics = CloudWatch("AttributesController")
  type FutureOption[A] = OptionT[Future, A]
  implicit val monadTransformer = Monad[FutureOption]
  import scalaz.syntax.monad._

  private def lookup(endpointDescription: String, onSuccess: Attributes => Result, onNotFound: Result = notFound) = backendAction.async { request =>
      authenticationService.userId(request).map[Future[Result]] { id =>
        attrService.get(id).map {
          case Some(attrs) =>
            metrics.put(s"$endpointDescription-lookup-successful", 1)
            onSuccess(attrs)
          case None =>
            metrics.put(s"$endpointDescription-user-not-found", 1)
            onNotFound
        }
      }.getOrElse {
        metrics.put(s"$endpointDescription-cookie-auth-failed", 1)
        Future(unauthorized)
      }
    }

  def membership = lookup("membership", identity[Attributes])

  def features = lookup("features",
    onSuccess = Features.fromAttributes,
    onNotFound = Features.unauthenticated
  )

  def membershipUpdateCard = updateCard(Membership)
  def digitalPackUpdateCard = updateCard(Digipack)

  def updateCard(implicit product: ProductFamily) = mmaCardAction.async { implicit request =>
    val updateForm = Form { single("stripeToken" -> nonEmptyText) }

    (for {
      user <- authenticationService.userId.point[FutureOption]
      sfUser <- OptionT(contactRepo.get(user))
      subscription <- OptionT(subscriptionService.get(sfUser))
      stripeCardToken <- OptionT(Future.successful(updateForm.bindFromRequest().value))
      updateResult <- OptionT(paymentService.setPaymentCardWithStripeToken(subscription.accountId, stripeCardToken))
    } yield updateResult match {
      case success: CardUpdateSuccess => Ok(Json.toJson(success))
      case failure: CardUpdateFailure => Forbidden(Json.toJson(failure))
    }).run.map(_.getOrElse(notFound))
  }

  def membershipDetails = paymentDetails(Membership)
  def digitalPackDetails = paymentDetails(Digipack)

  def paymentDetails(implicit product: ProductFamily) = mmaAction.async { implicit request =>
    (for {
      user <- OptionT(Future.successful(authenticationService.userId))
      contact <- OptionT(contactRepo.get(user))
      details <- OptionT(paymentService.paymentDetails(contact))
    } yield (contact, details).toResult).run.map(_ getOrElse Ok(Json.obj()))
  }
}
