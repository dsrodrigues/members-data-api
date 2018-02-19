package actions
import akka.stream.Materializer
import components.{TouchpointBackends, TouchpointComponents}
import configuration.Config
import controllers.NoCache
import play.api.http.HeaderNames.{ACCESS_CONTROL_ALLOW_CREDENTIALS, ACCESS_CONTROL_ALLOW_ORIGIN, ORIGIN}
import play.api.http.{DefaultHttpErrorHandler, ParserConfiguration}
import play.api.libs.Files.SingletonTemporaryFileCreator
import play.api.mvc._
import play.filters.cors.CORSConfig.Origins

import scala.concurrent.{ExecutionContext, Future}
import play.filters.cors.{CORSActionBuilder, CORSConfig}

class CommonActions(touchpointBackends: TouchpointBackends, bodyParser: BodyParser[AnyContent])(implicit ex: ExecutionContext, mat:Materializer) {
  def noCache(result: Result): Result = NoCache(result)

  val NoCacheAction = resultModifier(noCache)
  val BackendFromCookieAction = NoCacheAction andThen new WithBackendFromCookieAction(touchpointBackends, ex)

  private def resultModifier(f: Result => Result) = new ActionBuilder[Request, AnyContent] {
    override val parser = bodyParser
    override val executionContext = ex
    def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = block(request).map(f)
  }

  //TODO JUST A HACK TO SEE IF IT WORKS!!
  def CORSFilter(allowedOrigins: Origins) = // CORSActionBuilder(corsConfig, DefaultHttpErrorHandler, ParserConfiguration() , SingletonTemporaryFileCreator)

   new ActionBuilder[Request, AnyContent] {

    override def parser = bodyParser

    override protected  def executionContext: ExecutionContext = ex
    def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = {
      block(request).map { result =>
        (for (originHeader <- request.headers.get(ORIGIN) if allowedOrigins(originHeader)) yield {
          result.withHeaders(
            ACCESS_CONTROL_ALLOW_ORIGIN -> originHeader,
            ACCESS_CONTROL_ALLOW_CREDENTIALS -> "true")
        }).getOrElse{
          result
        }
      }
    }
  }
}

class BackendRequest[A](val touchpoint: TouchpointComponents, request: Request[A]) extends WrappedRequest[A](request)
