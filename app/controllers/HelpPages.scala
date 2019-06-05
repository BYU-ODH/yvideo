package controllers

import scala.concurrent._
import ExecutionContext.Implicits.global
import play.api.mvc.{Action, Controller}
import controllers.authentication.Authentication
import models.{HelpPage, User}
import dataAccess.ResourceController
import play.api.libs.json._

/**
 * Controller dealing with help pages
 */
trait HelpPages {
  this: Controller =>

  /**
   * Delete a particular help page
   * @param id The ID of the help page
   */
  def delete(id: Long) = Authentication.secureAPIAction() {
    implicit request =>
      implicit user =>
        Authentication.enforcePermissionAPI("admin") {
          HelpPage.findById(id).map(_.delete())
              Ok(Json.obj("message" -> "Help page deleted."))
        }
  }

  /**
   * Save/update a particular help page
   * @param id The ID of the help page
   */
  def save(id: Long) = Authentication.secureAPIAction(parse.urlFormEncoded) {
    implicit request =>
      implicit user =>
        Authentication.enforcePermissionAPI("admin") {

          val helpPage = HelpPage.findById(id)
          val title = request.body("title")(0)
          val contents = request.body("contents")(0)
          val category = request.body("category")(0)

          if (helpPage.isDefined) {
            helpPage.get.copy(title = title, contents = contents, category = category).save
            Ok(Json.obj("message" -> "Help page saved."))
          } else {
            // Create new
            val newHelpPage = HelpPage(None, title, contents, category).save
            Ok(Json.obj("info" -> "Help page created."))
          }
        }
  }

}

object HelpPages extends Controller with HelpPages