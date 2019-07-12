package controllers

import javax.inject._

import scala.concurrent._
import ExecutionContext.Implicits.global
import play.api.mvc.{Action, Controller}
import play.api.libs.json._
import controllers.authentication.Authentication
import models.{HelpPage, User}
import dataAccess.ResourceController

/**
 * Controller dealing with help pages
 */
class HelpPages @Inject
  (authentication: Authentication) extends Controller {

  /**
   * Table of contents view
   */
  def tableOfContents = Action {
    implicit request =>
      implicit val user = request.session.get("userId").flatMap(id => User.findById(id.toLong))
      Ok(Json.toJson(HelpPage.list.map(_.toJson)))
  }

  /**
   * View a particular help page
   * @param id The ID of the help page
   */
  def view(id: Long) = Action {
    implicit request =>
      implicit val user = request.session.get("userId").flatMap(id => User.findById(id.toLong))
      HelpPage.findById(id).map( helpPage =>
        Ok(helpPage.toJson)
      ).getOrElse(Errors.notFound)
  }

  /**
   * Delete a particular help page
   * @param id The ID of the help page
   */
  def delete(id: Long) = authentication.authenticatedAction() {
    implicit request =>
      implicit user =>
        authentication.enforcePermission("admin") {
          HelpPage.findById(id).map(_.delete())
          Future {
            Ok(Json.obj("info" -> "Help page deleted."))
          }
        }
  }

  /**
   * Save/update a particular help page
   * @param id The ID of the help page
   */
  def save(id: Long) = authentication.authenticatedAction(parse.urlFormEncoded) {
    implicit request =>
      implicit user =>
        authentication.enforcePermission("admin") {

          val helpPage = HelpPage.findById(id)
          val title = request.body("title")(0)
          val contents = request.body("contents")(0)
          val category = request.body("category")(0)

          Future {
            if (helpPage.isDefined) {
              helpPage.get.copy(title = title, contents = contents, category = category).save
                Ok(Json.obj("info" -> "Help page saved."))
            } else {
              // Create new
              val newHelpPage = HelpPage(None, title, contents, category).save
                Ok(Json.obj("info" -> "Help page created."))
            }
          }
        }
  }

}
