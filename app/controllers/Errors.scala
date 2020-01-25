package controllers

import play.api.mvc.Results._
import play.api.libs.json.Json

/**
 * Commonly used redirect errors.
 */
object Errors {
  val forbidden = Redirect(routes.Application.home()).flashing("error" -> "You cannot do that.")
  val notFound = Redirect(routes.Application.home()).flashing("error" -> "We couldn't find what you were looking for.")

  object api {
    def badRequest(msg: String = "Request missing required data.") = BadRequest(Json.obj("message" -> msg))
    def notFound(msg: String = "Not found.") = NotFound(Json.obj("message" -> msg))
    def forbidden(msg: String = "You cannot do that.") = Forbidden(Json.obj("message" -> msg))
    def serverError(msg: String = "Woops.") = InternalServerError(Json.obj("message" -> msg))
    // det unprocessableEntity(msg: String = "Can't be processed.") = UnprocessableEntity(Json.obj("message" -> msg))
  }
}
