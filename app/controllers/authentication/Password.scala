package controllers.authentication

import play.api.mvc.{Action, Controller}
import service.HashTools
import models.{User, SitePermissions}

/**
 * Controller which handles password authentication and account creation
 */
class Password @Inject (authentication: Authentication) extends Controller {

  /**
   * Logs the user in
   * @param action Login or merge
   * @param path When logging in, the path where the user will be redirected
   */
  def login(action: String, path: String = "") = Action(parse.multipartFormData) {
    implicit request =>
      val data = request.body.dataParts
      val username = data("username")(0)
      val password = data("password")(0)
      
      // Get the user based on the username and password
      val user = User.findByUsername('password, username)
      val passwordHash = HashTools.sha256Base64(password)

      // Check that the user exists and the password matches
      if (user.isDefined && user.get.authId == passwordHash) {

        authentication.login(user.get, path)
      } else {

        Redirect(controllers.routes.Application.login().toString(), request.queryString)
          .flashing("error" -> "Invalid username/password.")
      }
  }
}
