package controllers

import authentication.Authentication
import play.api.mvc._
import models._
import service.{FileUploader, ImageTools, HashTools}
import scala.util.{Try, Success, Failure}
import javax.imageio.ImageIO
import scala.concurrent._
import play.api.libs.json._
import ExecutionContext.Implicits.global

/**
 * Controller dealing with users
 */
trait Users {

  // https://coderwall.com/p/t_rapw/cake-pattern-in-scala-self-type-annotations-explicitly-typed-self-references-explained
  this: Controller =>

  /**
   * Get the collections that a user belongs to
   * @return Result[json array] of collections
   */
  def collectionsPreview(max: Int = 100) = Authentication.secureAPIAction() {
    implicit request =>
      implicit user =>
        Future(Ok(Json.toJson(user.getEnrollment.take(max).map(coll =>
          Json.obj(
            "contentCount" -> coll.getContent.length,
            "name" -> coll.name,
            "url" -> Json.toJson(coll.getContent.map(_.thumbnail).find(_.nonEmpty).getOrElse("")),
            "id" -> coll.id.get)))))
  }

  def roles = Authentication.secureAPIAction() {
    implicit request =>
      implicit user =>
        Future{
          val perms = SitePermissions.listByUser(user)
          Ok(Json.obj(
            "data" -> Json.obj(
              "authenticated" -> true,
              "permissions" -> Json.toJson(perms),
              "roles" -> Json.toJson(
                SitePermissions.permissionsToRoles(perms)))))}
  }

  def getAsJson = Authentication.secureAPIAction() {
    implicit request =>
      implicit user =>
        Future(Ok(user.toJson))
  }

  /**
   * Get the user's 4 most recently viewed contents
   * @return Result[json array] of content with id, name, thumbnail, and collection name
   */
  def recentContent = Authentication.secureAPIAction() {
    implicit request =>
      implicit user =>
        Future(Ok(Json.toJson(ViewingHistory.getUserViews(user.id.get).map { recentContent =>
          Content.findById(recentContent.contentId).map { content =>
            Json.obj(
              "contentId" -> recentContent.contentId,
              "name" -> content.name,
              "thumbnail" -> content.thumbnail,
              "collection" -> Collection.findById(content.collectionId).get.name)
          }.getOrElse(JsNull)
        }.filter(_ match {
          case JsNull => false
          case _ => false
        }))))
  }

  /**
   * The account settings view
   */
  def accountSettings = Authentication.authenticatedAction() {
    implicit request =>
      implicit user =>
        Future(Ok(views.html.users.accountSettings()))
  }

  /**
   * Saves the user account information
   */
  def saveSettings = Authentication.authenticatedAction(parse.urlFormEncoded) {
    implicit request =>
      implicit user =>

        // Change the user information
        val name = request.body("name")(0)
        val email = request.body("email")(0)
        user.copy(name = Some(name), email = Some(email)).save

        Future {
          Redirect(routes.Users.accountSettings())
            .flashing("info" -> "Personal information updated.")
        }
  }

  /**
   * Changes the user's password
   */
  def changePassword = Authentication.authenticatedAction(parse.urlFormEncoded) {
    implicit request =>
      implicit user =>

        val password1 = request.body("password1")(0)
        val password2 = request.body("password2")(0)
        val redirect = Redirect(routes.Users.accountSettings())

        // Make sure the passwords match
        Future {
          if (password1 == password2) {
            user.copy(authId = HashTools.sha256Base64(password1)).save
            redirect.flashing("info" -> "Password changed.")
          } else
            redirect.flashing("alert" -> "Passwords don't match.")
        }
  }

  /**
   * Updates the user's profile picture
   */
  def uploadProfilePicture = Authentication.authenticatedAction(parse.multipartFormData) {
    implicit request =>
      implicit user =>
        val redirect = Redirect(routes.Users.accountSettings())

        // Load the image from the file and make it into a thumbnail
        request.body.file("file") match {
        case None =>
          Future(redirect.flashing("error" -> "Missing File"))
        case Some(picture) =>
          Try(Option(ImageIO.read(picture.ref.file))) match {
          case Failure(_) =>
            Future(redirect.flashing("error" -> "Could not read image"))
          case Success(imgOpt) =>
            imgOpt match {
            case None =>
              Future(redirect.flashing("error" -> "Error reading image."))
            case Some(image) =>
              ImageTools.makeThumbnail(image) match {
              case None =>
                Future(redirect.flashing("error" -> "Error processing image."))
              case Some(thmb) =>
                // Upload the file
                FileUploader.uploadImage(thmb, picture.filename).map { url =>
                  // Save the user info about the profile picture
                  user.copy(picture = Some(url)).save
                  redirect.flashing("info" -> "Profile picture updated")
                }.recover { case _ =>
                  redirect.flashing("error" -> "Failed to upload image")
                }
              }
            }
          }
        }
  }

}

object Users extends Controller with Users
