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


/**
 * Controller which handles BYU CAS authentication.
 */
object Cas extends Controller {

  def logPrefix(submodule: String): String = s"[CAS]$submodule"

  val desc_map = Map(
    "DC" -> "Day Continuing",
    "DO" -> "Day Semester Only",
    "SO" -> "Semester Only",
    "CH" -> "Concurrent Student",
    "CE" -> "Continuing Ed or Evening Classes Only",
    "BG" -> "Bachelor of General Studies",
    "A"  -> "Audit Only"
  )

  val eligibleList = desc_map.keys.toList
  val descriptionList = desc_map.values.toList
  val descriptionMap = desc_map

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
    * classes have these attributes
        "sequence": "parent",
        "course": "C S 498R",
        "section": "002",
        "section_type": null,
        "block": null,
        "curriculum_id": "12417",
        "title_code": "000",
        "lab_quiz_section": null,
        "credit_hours": "3.0",
        "class_period": "4:00p - 4:50p",
        "days": "MWF",
        "room": "1127",
        "building": "JKB",
        "course_title": "Undergraduate Special Projects",
        "instructor": "Seamons, Kent Eldon"
    */
  def updateAccount(personId: String, username: String, user: User) = {
    val logpre = logPrefix("[UPDATE ACCOUNT]:")
    aim.getStudentData("records", personId, username).map { future =>
      future.map { response =>
        // some string manipulation is needed in order to parse the json
        val jsonString = response.body.replaceAll("data list is missing ending delimiter", "")
        val jsonResponse = Json.parse(jsonString) \ "RecMainService" \ "response"
        val regEligibility = (jsonResponse \ "regEligibility").asOpt[String]
        // update the user's permissions
        if (eligibleList.contains(regEligibility.getOrElse(""))) {
          SitePermissions.removeAllUserPermissions(user)
          SitePermissions.assignRole(user, 'student)
        }
        val currentYearTerm = (jsonResponse \ "currentYearTerm").asOpt[String]
        if (!currentYearTerm.isEmpty) {
          aim.getStudentData("schedule", personId + "/" + currentYearTerm.get, username).map { future =>
            future.map { response =>
              val schedule = (response.json \ "WeeklySchedService" \ "response" \ "schedule_table").as[List[JsValue]]
              val aim_courses: List[String] = schedule.flatMap { course =>
                Json.fromJson(course) match {
                  case JsSuccess(c: BYU_Course, _) => c.course :: Nil
                  case _ => Nil
                }
              }
              user.getEnrollment.foreach { course =>
                if (!aim_courses.contains(course)) {
                  Course.search(course.name).take(1).foreach(user.unenroll(_))
                }
              }
              aim_courses.foreach { course =>
                val courses = Course.search(course)
                if (courses.length > 1) Logger.error(s"${logpre}Too many courses match $course[${courses.length}]")
                Logger.info(s"${logpre}User:${user.id.get} AIM CourseName:${course} Ayamel CourseName:${courses.toString}")
                courses.take(1).foreach(user.enroll(_))
              }
              Some(response.body)
            }
          }
        } else {
          Logger.error(s"[Cas.scala]: Error parsing AIM records for [$username]: ${response.body}")
          None
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
        val username = ((xml \ "authenticationSuccess") \ "user").text
        val user = Authentication.getAuthenticatedUser(username, 'cas, getAttribute(xml, "name"), getAttribute(xml, "emailAddress"))
        val personId = getAttribute(xml, "personId").getOrElse("")

        updateAccount(personId, username, user)

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
            case Some(address) => "Either log in with a different method or <a href=\"" + address + "\">notify an administrator</a>."
            case None => "Try an alternate log in method."
          }
          Redirect(controllers.routes.Application.index()).flashing("error" -> ("An error occurred with CAS. " + advice))
      }
  }
}
