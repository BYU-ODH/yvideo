package controllers.authentication

import play.api.Logger
import play.api.mvc.{Result, Action, Controller}
import play.api.libs.json._
import play.api.libs.ws.WS
import play.api.Play
import play.api.Play.current
import scala.concurrent.{Await, Future, ExecutionContext}
import scala.concurrent.duration._
import ExecutionContext.Implicits.global

import models.SitePermissions
import models.User
import models.Course
import models.Collection


/**
 * Controller which handles BYU CAS authentication.
 */
object Cas extends Controller {

  def logPrefix(submodule: String): String = s"[CAS]$submodule"

  // For parsing schedule service
  case class BYU_Course(course: String, course_title: String, instructor: String)
  implicit val BYU_CourseReads = Json.reads[BYU_Course]

  val isHTTPS = current.configuration.getBoolean("HTTPS").getOrElse(false)

  /**
   * Redirects to the CAS login page.
   */
  def login(action: String, path: String = "") = Action {
    implicit request =>
      val service = routes.Cas.callback(action, path).absoluteURL(isHTTPS)
      Redirect("https://cas.byu.edu:443?service=" + service, 302)
  }

  /**
   * Gets the specified attribute as an Option[String]
   */
  private def getAttribute(xml: scala.xml.Elem, attributeName: String): Option[String] = {
    (xml \ "authenticationSuccess" \ "attributes" \ attributeName).text match {
      case "" => None
      case attribute => Some(attribute)
    }
  }

  /**
   * Enroll the user in their collections
   */
  private def updateCollections(user: User, isInstructor: Boolean)(implicit enrollment: aim.UserEnrollment) = {
    val eligibleCollections = user.getEligibleCollections(enrollment.class_list.map{
      byuClass =>
        Course(None, byuClass.subject_area, Some(byuClass.catalog_number), Some(byuClass.section_number))
    })
    user.getEnrollment.diff(eligibleCollections).foreach(c => {user.unenroll(c)})
    eligibleCollections.foreach(user.enroll (_, isInstructor))
  }

  /**
   * Query aim to update the user's account
   */
  def updateAccount(user: User, isInstructor: Boolean) = {
    aim.getEnrollment(user.username, isInstructor).map { enrollmentOpt =>
      enrollmentOpt.map { implicit enrollment =>
        val aimName = (enrollment.preferred_first_name + " " + enrollment.surname).trim
        val aimNameOpt = if (aimName.length != 0) Some(aimName) else None
        val emailOpt = if (enrollment.email_address.length != 0) Some(enrollment.email_address) else None
        val updatedUser = user.copy(name=aimNameOpt, email=emailOpt)
        Logger.debug(s"The following is the enrollment for $aimName")
        Logger.debug(enrollment.toString)
        if (user != updatedUser)
          updatedUser.save

        SitePermissions.removeAllUserPermissions(updatedUser)
        SitePermissions.assignRole(updatedUser, if (isInstructor) 'teacher else 'student)

        updateCollections(updatedUser, isInstructor)
      }
    }

    if (isInstructor) {
      // Enroll the instructor in classes they are enrolled in as students
      aim.getEnrollment(user.username, false).map {
        enrollmentOpt => enrollmentOpt.map {
          implicit teacherEnrollment => updateCollections(user, false)
        }
      }
    }
  }

  /**
   * When the CAS login is successful, it is redirected here, where the TGT and login are taken care of.
   */
  def callback(action: String, path: String = "") = Action {
    implicit request =>
    // Retrieve the TGT
      val tgt = request.queryString("ticket")(0)
      val casService = routes.Cas.callback(action, path).absoluteURL(isHTTPS)

      // Verify the TGT with CAS to get the user id
      val url = "https://cas.byu.edu/cas/serviceValidate?ticket=" + tgt + "&service=" + casService

      // Don't use Action.async, but rather wait for a period of time because CAS sometimes times out.
      val r: Future[Result] = WS.url(url).get().map { response =>
        val xml = response.xml
        Logger.debug("Cas user information:")
        Logger.debug(xml.toString)
        val username = ((xml \ "authenticationSuccess") \ "user").text
        val user = Authentication.getAuthenticatedUser(username, 'cas, getAttribute(xml, "name"), getAttribute(xml, "emailAddress"))
        val personId = getAttribute(xml, "personId").getOrElse("")
        val fulltime = getAttribute(xml, "activeFulltimeInstructor").getOrElse("false").toBoolean
        val parttime = getAttribute(xml, "activeParttimeInstructor").getOrElse("false").toBoolean
        implicit val isInstructor = fulltime || parttime
        Logger.debug(s"The person is an instructor $isInstructor")

        updateAccount(user, isInstructor)

        if (action == "merge")
          Authentication.merge(user)
        else
          Authentication.login(user, path)
      }

      try {
        Await.result(r, 20 seconds)
      } catch  {
        case _: Throwable =>
          val advice = Play.configuration.getString("smtp.address") match {
            case Some(address) => "Please contact us at " + address + " so that we can figure out what went wrong."
            case None => "Please contact us at arlitelab@gmail.com so that we can figure out what went wrong."
          }
          Redirect(controllers.routes.Application.index()).flashing("error" -> ("An error occurred with CAS. " + advice))
      }
  }
}
