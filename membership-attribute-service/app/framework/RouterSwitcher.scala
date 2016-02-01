package framework
import configuration.{SalesforceConfig, Config}
import play.api.mvc.RequestHeader
import play.api.routing.Router
import services.IdentityAuthService
import scalaz.syntax.std.option._

class RouterSwitcher(salesforceConfig: SalesforceConfig, val prodRouter: Router, val testRouter: Router)
{
  private val salesforceSecretParam = "secret"

  /**
   * Pick a router based either on the request cookie
   * or if from salesforce then the salesforce secret param
   */
  def selectRouter(request: RequestHeader): Router = {
    val isTestUser = IdentityAuthService.username(request).exists(Config.testUsernames.isValid)
    (request.getQueryString(salesforceSecretParam), isTestUser) match {
      case (Some(salesforceConfig.normalSecret.get), _) => prodRouter
      case (Some(salesforceConfig.testSecret.get), _) => testRouter
      case (_, true) => testRouter
      case (_, false) => prodRouter
    }
  }
}
