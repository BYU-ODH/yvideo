package controllers

import play.api.mvc.{Action, Controller, Result, ResponseHeader}
import service.{TimeTools, MobileDetection}
import play.core.parsers.FormUrlEncodedParser
import controllers.authentication.Authentication
import play.api.Play
import play.api.Play.current
import scala.concurrent._
import ExecutionContext.Implicits.global
import models.ContentListing
import dataAccess.ResourceController
import play.api.libs.iteratee.Enumerator

/**
 * A controller which deals with content in the context of a collection
 */
object CollectionContent extends Controller {

  /**
   * Content view in collection page
   */
  def viewInCollection(id: Long, collectionId: Long) = Authentication.authenticatedAction() {
    implicit request =>
      implicit user =>
        ContentController.getContent(id) { content =>
          Collections.getCollection(collectionId) { collection =>
            Future {
              // Check that the user can view the content
              if (content isVisibleBy user) Ok(
                if (MobileDetection.isMobile()) {
                  views.html.content.viewMobile(content, ResourceController.baseUrl, Some(user), Some(collection))
                } else {
                  views.html.content.view(content, ResourceController.baseUrl, Some(user), Some(collection))
                }
              ) else
                Errors.forbidden
            }
          }
        }
  }

  /**
   * Adds a particular content object to a collection
   * @param id The ID of the content
   */
  def addToCollection(id: Long) = Authentication.authenticatedAction(parse.urlFormEncoded) {
    implicit request =>
      implicit user =>
        val collectionId = request.body("collection")(0).toLong
        Collections.getCollection(collectionId) { collection =>
          if (user.hasCollectionPermission(collection, "addContent")) {
            ContentController.getContent(id) { content =>
              collection.addContent(content)
              val collectionLink = "<a href=\"" + routes.Collections.view(collection.id.get).toString() + "\">" + collection.name + "</a>"
              Future {
                Redirect(routes.CollectionContent.viewInCollection(content.id.get, collection.id.get))
                  .flashing("success" -> ("Content added to collection " + collectionLink))
              }
            }
          } else
            Future(Errors.forbidden)
        }
  }

  /**
   * Removes a particular content object from a collection
   * @param id The ID of the content
   * @param collectionId The ID of the collection
   */
  def removeFromCollection(id: Long, collectionId: Long) = Authentication.authenticatedAction() {
    implicit request =>
      implicit user =>
        Collections.getCollection(collectionId) { collection =>
          if (user.hasCollectionPermission(collection, "removeContent")) {
            ContentController.getContent(id) { content =>
              ContentListing.listByContent(content).find(_.collectionId == collectionId).map(_.delete())
              Future {
                Redirect(routes.Collections.view(collectionId))
                  .flashing("info" -> "Content removed")
              }
            }
          } else
              Future(Errors.forbidden)
        }
  }
}
