package services

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsync
import com.gu.scanamo._
import com.gu.scanamo.error.DynamoReadError
import com.gu.scanamo.syntax.{set => scanamoSet, _}
import com.typesafe.scalalogging.LazyLogging
import models.FeatureToggle

import scala.concurrent.{ExecutionContext, Future}
import scalaz.\/
import scalaz.syntax.std.either._
import scalaz.syntax.std.option._

class ScanamoFeatureToggleService(client: AmazonDynamoDBAsync, table: String)(implicit executionContext: ExecutionContext) extends FeatureToggleService with LazyLogging {

  def checkHealth: Boolean = client.describeTable(table).getTable.getTableStatus == "ACTIVE"

  private val scanamo = Table[FeatureToggle](table)
  def run[T] = ScanamoAsync.exec[T](client) _

  def get(featureName: String): Future[\/[String, FeatureToggle]] =
    run(scanamo.get('FeatureName -> featureName).map { maybeFeatureToggle =>
      val featureToggleDisjunction = maybeFeatureToggle map { featureToggleResult =>
        featureToggleResult.disjunction.leftMap(DynamoReadError.describe)
      }

      val featureToggle = featureToggleDisjunction \/> "Feature toggle not found"

      featureToggle.flatMap(identity)
    }
    )
}
