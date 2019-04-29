import play.api.http.HttpErrorHandler
import play.api.mvc._
import play.api.mvc.Results._
import play.api.libs.json.Json
import scala.concurrent._

  /**
   * custom error responses to Return json instead of html
   */
class ErrorHandler extends HttpErrorHandler {
  def onClientError(request: RequestHeader, statusCode: Int, message: String) = {
    Future.successful(
      Status(statusCode)(Json.toJson(s"Client Error: ${if(message.length > 0) message else "Bad Request Format or URL"}"))
    )
  }

  def onServerError(request: RequestHeader, exception: Throwable) = {
    Future.successful(
      InternalServerError(Json.toJson(s"Server Error: ${exception.getMessage}"))
    )
  }
}

