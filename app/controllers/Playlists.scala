package controllers

import javax.inject._

import scala.concurrent._
import ExecutionContext.Implicits.global
import play.api.mvc.Controller
import controllers.authentication.Authentication
import dataAccess.{ResourceController, PlayGraph}

/**
 * Controller dealing with playlists (playgraphs)
 */
class Playlists @Inject
  (authentication: Authentication, ContentController: ContentController) extends Controller {

  /**
   * The about page. View information/description of the playlist
   * @param id The ID of the playlist
   */
  def about(id: Long) = authentication.authenticatedAction() {
    implicit request =>
      implicit user =>
        Future {
          ContentController.getContentCollection(id) { (content, collection) =>
            // Check the content type
            if (content.contentType == 'playlist) {
              // Check that the user can view the content
              if (collection.userCanViewContent(user)) {
                Ok(views.html.playlists.about(content))
              } else {
                Errors.forbidden
              }
            } else {
              Redirect(routes.ContentController.view(id))
            }
          }
        }
  }

  /**
   * View (play) a particular playlist
   * @param id The ID of the playlist
   */
  def view(id: Long) = authentication.authenticatedAction() {
    implicit request =>
      implicit user =>
        Future {
          ContentController.getContentCollection(id) { (content, collection) =>
            // Check the content type
            if (content.contentType == 'playlist) {
              // Check that the user can view the content
              if (collection.userCanViewContent(user)) {
                Ok(views.html.playlists.view(content, ResourceController.baseUrl))
              } else {
                Errors.forbidden
              }
            } else {
              Redirect(routes.ContentController.view(id))
            }
          }
        }
  }
}
