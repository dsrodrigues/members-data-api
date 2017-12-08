import sbt._
import play.sbt.PlayImport

object Dependencies {

  //versions
  val awsClientVersion = "1.11.240"
  //libraries
  val sentryRavenLogback = "com.getsentry.raven" % "raven-logback" % "8.0.3"
  val identityCookie = "com.gu.identity" %% "identity-cookie" % "3.79"
  val identityPlayAuth = "com.gu.identity" %% "identity-play-auth" % "1.3"
  val identityTestUsers =  "com.gu" %% "identity-test-users" % "0.6"
  val scalaUri = "com.netaporter" %% "scala-uri" % "0.4.16"
  val playWS = PlayImport.ws
  val playCache = PlayImport.cache
  val playFilters = PlayImport.filters
  val specs2 = PlayImport.specs2 % "test"
  val scanamo = "com.gu" %% "scanamo" % "0.9.5"
  val awsWrap = "com.github.dwhjames" %% "aws-wrap" % "0.8.0"
  val awsDynamo = "com.amazonaws" % "aws-java-sdk-dynamodb" % awsClientVersion
  val awsSNS = "com.amazonaws" % "aws-java-sdk-sns" % awsClientVersion
  val awsCloudWatch = "com.amazonaws" % "aws-java-sdk-cloudwatch" % awsClientVersion
  val membershipCommon = "com.gu" %% "membership-common" % "0.490"
  val scalaz = "org.scalaz" %% "scalaz-core" % "7.1.3"
  val kinesis = "com.gu" % "kinesis-logback-appender" % "1.4.2"
  val logstash = "net.logstash.logback" % "logstash-logback-encoder" % "4.11"
  val jacksonCbor = "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor" % "2.9.2"

  //projects

  val apiDependencies = Seq(sentryRavenLogback, identityCookie, identityPlayAuth, identityTestUsers, scalaUri,
    playWS, playCache, playFilters, scanamo, awsWrap, awsDynamo, awsSNS, awsCloudWatch, scalaz, membershipCommon,
    specs2, kinesis, logstash, jacksonCbor)

}
