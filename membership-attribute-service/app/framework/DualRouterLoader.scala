package framework
import components._
import configuration.SalesforceConfig
import monitoring.SentryLogging
import play.api.ApplicationLoader.Context
import play.api._
import play.api.routing.Router

class DualRouterLoader extends ApplicationLoader {

  def load(context: Context) = {
    Logger.configure(context.environment)
    SentryLogging.init()

    new BuiltInComponentsFromContext(context) with AllComponentTraits {
      override lazy val httpErrorHandler = new JsonHttpErrorHandler()
      lazy val testServices = new TestTouchpointComponents(this)
      lazy val prodServices = new NormalTouchpointComponents(this)
      lazy val testRouter = new ControllerComponents(testServices, this).router
      lazy val prodRouter = new ControllerComponents(prodServices, this).router
      lazy val routerSwitcher = new RouterSwitcher(SalesforceConfig(prodServices.sfSecret, testServices.sfSecret), prodRouter, testRouter)
      lazy val router: Router = new DualRouter(routerSwitcher)
    }.application
  }
}

