package components

import com.gu.config
import com.gu.memsub.services.{CatalogService, PaymentService, SubscriptionService}
import com.gu.monitoring.ServiceMetrics
import com.gu.salesforce.SimpleContactRepository
import com.gu.stripe.StripeService
import com.gu.touchpoint.TouchpointBackendConfig
import com.gu.zuora.soap.ClientWithFeatureSupplier
import com.gu.zuora.{ZuoraService, rest}
import configuration.{SalesforceSecret, SalesforceOrganisationId, Config}
import framework.AllComponentTraits
import repositories.MembershipAttributesSerializer
import services.{AttributeService, DynamoAttributeService}
import play.api.libs.concurrent.Execution.Implicits._

abstract class TouchpointComponents(common: AllComponentTraits) {

  import common._
  protected val stage: String

  lazy val conf = Config.config.getConfig("touchpoint.backend")
  lazy val environmentConf = conf.getConfig(s"environments.$stage")
  lazy implicit val system = actorSystem

  lazy val digitalPackConf = environmentConf.getConfig(s"zuora.ratePlanIds.digitalpack")
  lazy val membershipConf = environmentConf.getConfig(s"zuora.ratePlanIds.membership")
  lazy val sfOrganisationId = SalesforceOrganisationId(environmentConf.getString("salesforce.organization-id"))
  lazy val sfSecret = SalesforceSecret(environmentConf.getString("salesforce.hook-secret"))
  protected lazy val dynamoTable = environmentConf.getString("dynamodb.table")

  lazy val digitalPackPlans = config.DigitalPackRatePlanIds.fromConfig(digitalPackConf)
  lazy val membershipPlans = config.MembershipRatePlanIds.fromConfig(membershipConf)

  lazy val tpConfig = TouchpointBackendConfig.byEnv(stage, conf)
  implicit lazy val _bt = tpConfig
  lazy val metrics = new ServiceMetrics(tpConfig.zuoraRest.envName, Config.applicationName,_: String)

  lazy val stripeService = new StripeService(tpConfig.stripe, metrics("stripe"))
  lazy val soapClient = new ClientWithFeatureSupplier(Set.empty, tpConfig.zuoraSoap, metrics("zuora-soap"))
  lazy val restClient = new rest.Client(tpConfig.zuoraRest, metrics("zuora-rest"))

  lazy val contactRepo = new SimpleContactRepository(tpConfig.salesforce, system.scheduler, Config.applicationName)
  lazy val attrService: AttributeService = DynamoAttributeService(MembershipAttributesSerializer(dynamoTable))
  lazy val zuoraService = new ZuoraService(soapClient, restClient, membershipPlans)
  lazy val catalogService = CatalogService(restClient, membershipPlans, digitalPackPlans, stage)
  lazy val subscriptionService = new SubscriptionService(zuoraService, stripeService, catalogService)
  lazy val paymentService = new PaymentService(stripeService, subscriptionService, zuoraService, catalogService)
}
