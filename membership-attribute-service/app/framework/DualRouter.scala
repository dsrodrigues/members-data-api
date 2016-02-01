package framework

import play.api.mvc.{Handler, RequestHeader}
import play.api.routing.Router
import play.api.routing.Router.Routes

class DualRouter(routerSwitcher: RouterSwitcher) extends Router {
  override def routes: Routes = routerSwitcher.prodRouter.routes
  override def withPrefix(prefix: String): Router = routerSwitcher.prodRouter.withPrefix(prefix)
  override def documentation: Seq[(String, String, String)] = routerSwitcher.prodRouter.documentation

  override def handlerFor(request: RequestHeader): Option[Handler] =
    routerSwitcher.selectRouter(request).routes.lift(request)
}