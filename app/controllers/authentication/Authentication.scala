package controllers.authentication

import scala.concurrent._
import ExecutionContext.Implicits.global
import play.api.mvc._
import play.api.Play.current
import play.api.libs.json._
import models.{User, SitePermissions, Collection}
import controllers.Errors
import service.TimeTools
import scala.collection.JavaConverters._

/**
 * This controller does logging out and has a bunch of helpers for dealing with authentication and permissions.
 */
object Authentication extends Controller {

  val isHTTPS = current.configuration.getBoolean("HTTPS").getOrElse(false)
  val allowedOrigins: Option[List[String]] = current.configuration.getList("allowedOrigins").map(_.asScala.toList.map(_.unwrapped.toString))

  /**
   * Given a user, logs the user in and sets up the session
   * @param user The user to log
   * @param path A path where the user will be redirected
   * @return The result. To be called from within an action
   */
  def login(user: User, path: String)(implicit request: RequestHeader): Result = {

    // Log the user in
    user.copy(lastLogin = TimeTools.now()).save

    // Redirect
    {
      if (path.isEmpty)
        Redirect(controllers.routes.Application.home())
      else
        Redirect(path)
    }.withSession("userId" -> user.id.get.toString)
      .flashing("success" -> ("Welcome " + user.displayName + "!"))
  }

  /**
   * Logs out
   */
  def logout = Action {
    implicit request =>
      val service = controllers.routes.Application.login().absoluteURL(isHTTPS)
      getUserFromRequest()(request).map { user =>
        val casLogoutUrl  = "https://cas.byu.edu/cas/logout?service="

        val redir: String = casLogoutUrl + service
        Redirect(redir)
      }.getOrElse(Redirect(service))
        .withNewSession
  }

  /**
   * Logs out
   */
  def logoutWithService(service: String) = Action {
    implicit request =>
      getUserFromRequest()(request).map { user =>
        val casLogoutUrl  = "https://cas.byu.edu/cas/logout?service="

        val redir: String = casLogoutUrl + service
        Redirect(redir)
      }.getOrElse(Redirect(service))
        .withNewSession
  }

  /**
   * Once the user is authenticated with some scheme, call this to get the actual user object. If it doesn't exist then
   * it will be created.
   * @param username The username of the user
   * @param name The name of the user. Used only if creating the user.
   * @param email The email of the user. Used only if creating the user.
   * @return The user
   */
  def getAuthenticatedUser(username: String, name: Option[String] = None, email: Option[String] = None): User = {
    // Check if the user is already created
    val user = User.findByUsername(username)

    // Add the email and username if they are empty
    val updatedUser = user.map { user =>
      if ((user.email.filterNot(_.length!=0).isEmpty && !email.isEmpty) ||
       (user.name.filterNot(_.length!=0).isEmpty && !name.isEmpty)) {
        user.copy(email = email, name = name).save
      } else user
    }

    updatedUser.getOrElse {
      val user = User(None, username, name, email).save
      SitePermissions.assignRole(user, 'student)
      user
    }
  }


  // ==========================
  //   Authentication Helpers
  // ==========================
  // These are to help with creating authenticated
  // action or ensuring a certain access level.
  // ==========================

  def enforceCollectionAdmin(collection: Option[Collection])(result: Future[Result])(implicit request: Request[_], user: User): Future[Result] = {
    if (collection.map(_.userIsAdmin(user)).getOrElse(false) || user.hasSitePermission("admin"))
      result
    else {
      Future { Errors.forbidden }
    }
  }

  /**
   * Takes and Collection and checks if the user is enrolled in said collection
   * invokes the given function with a boolean that signifies whether the user has
   * admin privileges in the collection
   * {{{
   * Authentication.enforceEnrollment(collection) { isCollAdmin =>
   *  //...
   *  Future(Ok)
   * }
   * }}}
   */
  def enforceEnrollment(collection: Collection)(f: Boolean => Future[Result])(implicit request: Request[_], user: User): Future[Result] = {
    if (user.isEnrolled(collection) || user.isManager) {
      if (collection.userIsAdmin(user) || user.isManager) { f(true) }
      else { f(false) }
    }
    else { Errors.api.forbidden("The user is not enrolled in the collection.") }
  }

  def enforcePermission(permission: String)(result: Future[Result])(implicit request: Request[_], user: User): Future[Result] = {
    if (user.hasSitePermission(permission))
      result
    else
      Future { Errors.forbidden }
  }

  def enforcePermissionAPI(permission: String)(result: Future[Result])(implicit request: Request[_], user: User): Future[Result] = {
    if (user.hasSitePermission(permission))
      result
    else
      Errors.api.forbidden()
  }

  def getUserFromRequest()(implicit request: RequestHeader): Option[User] = {
    request.session.get("userId").flatMap( userId => User.findById(userId.toLong) )
  }

  // Implicit conversion for result to future result
  implicit def result2futureresult(r: Result): Future[Result] = Future(r)

  /**
   * A generic action to be used for secured API endpoints
   * @param f The action logic. A curried function which, given a request and the authenticated user, returns a result.
   * @return The result. A Forbidden (403) with a json message or the Result returned from the given action
   */
  def secureAPIAction[A](parser: BodyParser[A] = BodyParsers.parse.anyContent)(f: Request[A] => User => Future[Result]) = Action.async(parser) {
    implicit request =>
      getUserFromRequest().map( user => f(request)(user) ).getOrElse(
        Future(Errors.api.forbidden("You must be logged in to access this resource")))
  }

  /**
   * A generic action to be used on authenticated pages.
   * @param f The action logic. A curried function which, given a request and the authenticated user, returns a result.
   * @return The result. Either a redirect due to not being logged in, or the result returned by f.
   */
  def authenticatedAction[A](parser: BodyParser[A] = BodyParsers.parse.anyContent)(f: Request[A] => User => Future[Result]) = Action.async(parser) {
    implicit request =>
      getUserFromRequest().map( user => f(request)(user) ).getOrElse {
        Future {
          Redirect(controllers.routes.Application.login().toString(), Map("path" -> List(request.path)))
            .flashing("alert" -> "You are not logged in")
        }
      }
  }
}
