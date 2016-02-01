package components
import configuration.Config
import framework.AllComponentTraits

class NormalTouchpointComponents(components: AllComponentTraits) extends TouchpointComponents(components) {
  override lazy val stage = Config.defaultTouchpointBackendStage
}
