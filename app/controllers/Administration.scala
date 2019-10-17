package controllers

import authentication.Authentication
import authentication.Authentication.result2futureresult
import play.api.mvc._
import models._
import service.FileUploader
import scala.concurrent._
import scala.concurrent.duration._
import ExecutionContext.Implicits.global
import play.api.Logger
import dataAccess.ResourceController
import play.api.libs.json._

/**
 * Controller for Administration pages and actions
 */
trait Administration { this: Controller =>

  /**
   * Get the number of all current users
   * @return the total number of current users
   */
  def userCount() = Authentication.secureAPIAction() {
    implicit request =>
      implicit user =>
      Authentication.enforcePermissionAPI("admin") {
        Ok(Json.toJson(User.count))
      }
  }


  /**
   * Get the users that match the given search criteria
   * @return a list of users based on the given search criteria
   */
   def searchUsers(columnName: String, searchValue: String) = Authentication.secureAPIAction() {
    implicit request =>
      implicit user =>
      val allowedColumns = List("username", "name", "email")
      Authentication.enforcePermissionAPI("admin") {
        if (allowedColumns.contains(columnName)) {
          if (searchValue.length > 3) {
              Ok(Json.toJson(User.search(columnName, searchValue).map(_.toJson)))
          } else { Forbidden(JsObject(Seq("message" -> JsString("Search value was too short"))))}
        } else { Forbidden(JsObject(Seq("message" -> JsString("Search column is not allowed"))))}
      }
  }
  /**
   * Helper function for finding user accounts
   * @param id The ID of the user account
   * @param f The function which will be called with the user
   */
  private def getUser(id: Long)(f: User => Result)(implicit request: RequestHeader): Result = {
    User.findById(id).map { user =>
      f(user)
    }.getOrElse(Errors.api.notFound())
  }

  /**
   * Sends an email notification to a user
   */
  def sendNotification() = Authentication.secureAPIAction(parse.json) {
    implicit request =>
      implicit user =>
        Authentication.enforcePermissionAPI("admin") {
          val id = (request.body \ "userId").as[Long]
          getUser(id) { targetUser =>
            // Send a notification email to the user
            val message = (request.body \ "message").as[String]
            targetUser.sendNotification(message)
            Ok(Json.obj("message" -> JsString("Notification sent to "+targetUser.username)))
          }
        }
  }

  /**
   * Deletes a user
   * @param id The ID of the user
   */
  def delete(id: Long) = Authentication.secureAPIAction() {
    implicit request =>
      implicit user =>
        Authentication.enforcePermissionAPI("admin") {
          getUser(id) { targetUser =>
            targetUser.delete()
            Ok(Json.obj("message" -> JsString("User deleted")))
          }
        }
  }

  /**
   * Deletes a collection
   * @param id The ID of the collection to delete
   */
  def deleteCollection(id: Long) = Authentication.secureAPIAction() {
    implicit request =>
      implicit user =>
        Authentication.enforcePermissionAPI("admin") {
          Collections.getCollection(id) { collection =>
              collection.delete()
              Ok(Json.obj("message" -> JsString("Collection deleted")))
          }
        }
  }

  /**
   * Saves and updates the site settings
   */
  def saveSiteSettings = Authentication.secureAPIAction(parse.json) {
    implicit request =>
      implicit user =>
        Authentication.enforcePermissionAPI("admin") {
          request.body.as[Map[String, String]].foreach { data =>
            Setting.findByName(data._1).get.copy(value = data._2).save
          }
          Ok(Json.obj("message" -> JsString("Site settings updated")))
        }
  }

  /**
   * Proxies in as a different user
   * @param id The ID of the user to be proxied in as
   */
  def proxy(id: Long) = Authentication.secureAPIAction() {
    implicit request =>
      implicit user =>
        Authentication.enforcePermissionAPI("admin") {
          User.findById(id) match {
            case Some(proxyUser) =>
              Ok(Json.obj("message" -> JsString(s"Now proxying as user ${proxyUser.username}")))
                .withSession("userId" -> proxyUser.id.get.toString)
            case _ =>
              Forbidden(JsObject(Seq("message" -> JsString("Requested Proxy User Not Found"))))
          }
        }
  }
}

object Administration extends Controller with Administration
