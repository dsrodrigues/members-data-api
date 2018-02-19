package configuration

import java.time.Duration

import com.amazonaws.regions.Regions
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder
import com.getsentry.raven.dsn.Dsn
import com.gu.aws.CredentialsProvider
import com.gu.identity.cookie.{PreProductionKeys, ProductionKeys}
import com.gu.identity.testing.usernames.{Encoder, TestUsernames}
import com.typesafe.config.ConfigFactory
import scala.collection.JavaConversions._


import scala.util.Try

object Config {

  val config = ConfigFactory.load()
  val applicationName = "members-data-api"

  val stage = config.getString("stage")

  val idKeys = if (config.getBoolean("identity.production.keys")) new ProductionKeys else new PreProductionKeys
  val useFixtures = config.getBoolean("use-fixtures")
  lazy val sentryDsn = Try(new Dsn(config.getString("sentry.dsn")))

  object AWS {
    val region = Regions.EU_WEST_1
  }

  lazy val sqsClient = AmazonSQSAsyncClientBuilder
      .standard
      .withCredentials(CredentialsProvider)
      .withRegion(AWS.region)
      .build()


  lazy val testUsernames = TestUsernames(Encoder.withSecret(config.getString("identity.test.users.secret")), Duration.ofDays(2))

  val defaultTouchpointBackendStage = config.getString("touchpoint.backend.default")
  val testTouchpointBackendStage = config.getString("touchpoint.backend.test")

  val CORSAllowedOrigins: List[String]  = config.getStringList("default.cors.allowedOrigins").toList
  val mmaCORSAllowedOrigins: List[String]  = config.getStringList("mma.cors.allowedOrigins").toList
  val abandonedCartEmailQueue = config.getString("abandoned.cart.email.queue")

  object Logstash {
    private val param = Try{config.getConfig("param.logstash")}.toOption
    val stream = Try{param.map(_.getString("stream"))}.toOption.flatten
    val streamRegion = Try{param.map(_.getString("streamRegion"))}.toOption.flatten
    val enabled = Try{config.getBoolean("logstash.enabled")}.toOption.contains(true)
  }

}
