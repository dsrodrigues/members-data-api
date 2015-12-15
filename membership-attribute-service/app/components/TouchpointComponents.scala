package components

import com.gu.config
import com.gu.config.ProductFamily
import com.gu.memsub.services.{PaymentService, SubscriptionService}
import com.gu.monitoring.ServiceMetrics
import com.gu.salesforce.SimpleContactRepository
import com.gu.stripe.StripeService
import com.gu.touchpoint.TouchpointBackendConfig
import com.gu.zuora.{rest, soap}
import configuration.Config
import models.{DigitalPack, Membership, ProductFamilyName}
import play.api.Play.current
import play.api.libs.concurrent.Akka
import repositories.MembershipAttributesSerializer
import services.{AttributeService, DynamoAttributeService}
import play.api.libs.concurrent.Execution.Implicits._

case class TouchpointComponents(stage: String) {
  lazy val conf = Config.config.getConfig("touchpoint.backend")
  lazy val environmentConf = conf.getConfig(s"environments.$stage")

  lazy val digitalPackConf = environmentConf.getConfig(s"zuora.ratePlanIds")
  lazy val membershipConf = environmentConf.getConfig(s"zuora.ratePlanIds.membership")
  lazy val sfOrganisationId = environmentConf.getString("salesforce.organization-id")
  lazy val sfSecret = environmentConf.getString("salesforce.hook-secret")
  lazy val dynamoTable = environmentConf.getString("dynamodb.table")

  lazy val digitalPackPlans = config.DigitalPack.fromConfig(digitalPackConf)
  lazy val membershipPlans = config.Membership.fromConfig(membershipConf)

  lazy val tpConfig = TouchpointBackendConfig.byEnv(stage, conf)
  lazy val metrics = new ServiceMetrics(tpConfig.zuoraRest.envName, Config.applicationName,_: String)

  lazy val stripeService = new StripeService(tpConfig.stripe, metrics("stripe"))
  lazy val soapClient = new soap.Client(tpConfig.zuoraSoap, metrics("zuora-soap"), Akka.system)
  lazy val restClient = new rest.Client(tpConfig.zuoraRest, metrics("zuora-rest"))

  lazy val contactRepo = new SimpleContactRepository(tpConfig.salesforce, Akka.system.scheduler, Config.applicationName)
  lazy val attrService: AttributeService = DynamoAttributeService(MembershipAttributesSerializer(dynamoTable))
  lazy val subService = new SubscriptionService(soapClient, restClient, stripeService)
  lazy val paymentService = new PaymentService(stripeService, subService)

  def productRatePlanIds(familyName: ProductFamilyName): ProductFamily = familyName match {
    case DigitalPack => digitalPackPlans
    case Membership => membershipPlans
  }
}
