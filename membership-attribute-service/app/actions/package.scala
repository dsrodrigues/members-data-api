import components.TouchpointComponents
import play.api.mvc.{WrappedRequest, Request, Action}

package object actions {
  class BackendRequest[A](val touchpoint: TouchpointComponents, request: Request[A]) extends WrappedRequest[A](request)
}
