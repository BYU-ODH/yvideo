package controllers.ajax

import scala.concurrent._
import ExecutionContext.Implicits.global
import play.api.mvc.Controller
import controllers.authentication.Authentication
import play.api.libs.json.{JsObject, JsArray}
import models.Content

/**
 * Controller for listing out content the user can see. For AJAX calls. These need to be cross-domain so as to work with
 * the PlayGraph editor.
 */
class ContentLister @Inject (authentication: Authentication) extends Controller {

  /**
   * Lists the collections the user is in and the content under each collection.
   */
  def collection = authentication.authenticatedAction() {
    implicit request =>
      implicit user =>
        val collections = user.getEnrollment
        val content = collections.map(collection => (collection.name, JsArray(collection.getContent.map(_.toJson))))
        val origin = request.headers.get("Origin").getOrElse("*")
        Future {
          Ok(JsObject(content)).withHeaders(
            "Access-Control-Allow-Origin" -> origin,
            "Access-Control-Allow-Credentials" -> "true"
          )
        }
  }

  /**
   * Returns a particular content
   * @param id The ID of the content
   */
  def get(id: Long) = authentication.authenticatedAction() {
    implicit request =>
      implicit user =>
        Future {
          Content.findById(id).map { content =>
            val origin = request.headers.get("Origin").getOrElse("*")
            Ok(content.toJson).withHeaders(
              "Access-Control-Allow-Origin" -> origin,
              "Access-Control-Allow-Credentials" -> "true"
            )
          }.getOrElse(Forbidden)
        }
  }
}
