package components
import configuration.Config
import framework.AllComponentTraits

class TestTouchpointComponents(components: AllComponentTraits) extends TouchpointComponents(components) {
  override lazy val stage = Config.testTouchpointBackendStage
}
