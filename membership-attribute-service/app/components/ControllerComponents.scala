package components
import com.softwaremill.macwire._
import controllers.{SalesforceHookController, HealthCheckController, AttributeController}
import framework.AllComponentTraits
import play.api.routing.Router
import router.Routes

class ControllerComponents(touchpoint: TouchpointComponents, framework: AllComponentTraits) {
  import touchpoint._, framework._

  lazy val a = wire[AttributeController]
  lazy val c = wire[HealthCheckController]
  lazy val s = wire[SalesforceHookController]

  lazy val prefix = "/"
  lazy val router: Router = wire[Routes]
}
