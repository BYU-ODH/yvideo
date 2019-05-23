package controllers

import authentication.Authentication
import scala.concurrent._
import ExecutionContext.Implicits.global
import play.api.mvc._
import models._
import service.EmailTools
import models.Content
import play.api.libs.json.{JsNull, Json}

trait Application {
  this: Controller =>

  /**
   * A special login page for admins
   * SCHEDULED FOR DESTRUCTION
   */
  def login = Action {
    implicit request =>
      val user = Authentication.getUserFromRequest()
      if (user.isDefined)
        Redirect(controllers.routes.Application.home())
      else {
        val path = request.queryString.get("path").map(path => path(0)).getOrElse("")
        Ok(views.html.application.login(path))
      }
  }

  /**
   * The home page
   * SCHEDULED FOR DESTRUCTION
   */
  def home = Authentication.authenticatedAction() {
    implicit request =>
      implicit user =>
        Future(Ok(views.html.application.home()))

  }

  /**
   * Feedback helper methods: parse different kinds of info
   * Eventually, these should be eliminated by sending different
   * kinds of structured data to different feedback routes
   */

  def getProblemInfo(description: String): (String, String, String) = {
    val pattern = "^Problem: (.*), Reproduce: (.*), User Agent: (.*)$".r
    description.replaceAll("\\n", "") match {
      case pattern(problem, reproduce, userAgent) => (problem, reproduce, userAgent)
      case _ => ("error", "error", "error")
    }
  }

  def getSuggestionInfo(description: String): String = {
    val pattern = "^Feature: (.*)$".r
    description.replaceAll("\\n", "") match {
      case pattern(suggestion) => suggestion
      case _ => "error"
    }
  }

  def getThoughtInfo(description: String): (String, String, String, String) = {
    val pattern = "^Navigate: (.*), Find: (.*), Useful: (.*), Comments: (.*)$".r
    description.replaceAll("\\n", "") match {
      case pattern(navigate, find, useful, comments) => (navigate, find, useful, comments)
      case _ => ("error", "error", "error", "error")
    }
  }

  /**
   * Saves feedback submissions (bug reports, suggestions, ratings)
   */
  def saveFeedback = Authentication.authenticatedAction(parse.urlFormEncoded) {
    request =>
      user =>
        val category = request.body("category")(0)
        val description = request.body("description")(0)

        // Send out emails
        category match {
        case "problem" =>
          EmailTools.sendAdminNotificationEmail("notifications.notifyOn.bugReport", getProblemInfo(description))
        case "suggestion" =>
          EmailTools.sendAdminNotificationEmail("notifications.notifyOn.suggestion", getSuggestionInfo(description))
        case "rating" =>
          EmailTools.sendAdminNotificationEmail("notifications.notifyOn.rating", getThoughtInfo(description))
        }
        Future(Ok)
  }

  /**
   * Saves feedback submitted on an error
   * THIS MIGHT BE OBSOLETE???
   */
  def saveErrorFeedback = Authentication.authenticatedAction(parse.urlFormEncoded) {
    request =>
      user =>
        val description = request.body("description")(0)
        val errorCode = request.body("errorCode")(0)
        val userId = user.id.get
        EmailTools.sendAdminNotificationEmail("notifications.notifyOn.errorReport", (errorCode, description, userId))
        Future(Ok)
  }
}

object Application extends Controller with Application
