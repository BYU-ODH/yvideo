package controllers.authentication

import play.api.Logger
import play.api.Play.current
import play.api.Play
import play.api.libs.ws.{WS, WSResponse, WSAuthScheme}
import play.api.libs.json._
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object aim {

  case class UserEnrollment(
    person_id: String,
    byu_id: String,
    sort_name: String,
    surname: String,
    preferred_first_name: String,
    email_address: String,
    student_class_count: Int,
    class_list: List[BYU_Class]
  )

  case class BYU_Class(
    year_term: String,
    subject_area: String,
    catalog_number: String,
    section_number: String,
    curriculum_id: String,
    title_code: String,
    section_type: String,
    block_code: String,
    course_title: String
  )

  implicit val byuClassReads = Json.reads[BYU_Class]
  implicit val userEnrollmentReads = Json.reads[UserEnrollment]

  val CONSUMER_KEY = Play.application.configuration.getString("aim.consumerkey").get
  val CONSUMER_SECRET = Play.application.configuration.getString("aim.consumersecret").get

  def getScheduleUrl(idValue: String, yearTerm: String, idType: String = "netid")(implicit isInstructor: Boolean = false) = {
    val service = if (isInstructor) "instructorschedule" else "studentschedule"
    s"https://api.byu.edu:443/domains/legacy/academic/registration/enrollment/v1/$service/$idType/$idValue/$yearTerm"
  }

  def getSchedule(netid: String, yearTerm: String, token: String)(implicit isInstructor: Boolean) = {
    WS.url(getScheduleUrl(netid, yearTerm))
    .withHeaders("Accept" -> "application/json", "Authorization" -> s"Bearer $token")
    .get().map { scheduleResp =>
      scheduleResp.status match {
        case 200 => {
          Logger.info("The schedule url returned with a 200 status code")
          val jsonLookup = (scheduleResp.json \ "EnrollmentService" \ "response").toOption
          if (jsonLookup.isDefined) {
            val json = jsonLookup.get
            val userEnrollmentFromJson = Json.fromJson[UserEnrollment](json)

             userEnrollmentFromJson match {
              case JsSuccess(userEnrollment: UserEnrollment, path: JsPath) => {
                userEnrollment
              }
              case e: JsError => {
                Logger.info(s"There was an exception in the conversion: ${JsError.toJson(e).toString}")
                throw new Exception(s"Errors: ${JsError.toJson(e).toString}")
              }
              case _ => Logger.info(s"Issue with the conversion and didn't even return a json success or error...")
            }
          } else {
            Logger.error("Error parsing AIM service student information. [aim.scala]")
          }
        }
        case _ => Logger.error(s"Error Getting student schedule: [${scheduleResp.status}] ${scheduleResp.statusText}")
      }
    }
  }

  def getCurrentSemester(netid: String, token: String)(implicit isInstructor: Boolean) = {
    val format = new java.text.SimpleDateFormat("yyyyMMdd")
    val date = format.format(new java.util.Date())
    WS.url(s"https://ws.byu.edu/rest/v1/academic/controls/controldatesws/asofdate/$date/semester.json").get().foreach { yearTermResp =>
      yearTermResp.status match {
        case 200 => {
          ((yearTermResp.json \ "ControldateswsService" \ "response" \ "date_list")(0) \ "year_term").as[String] match {
            case yearTerm: String =>
              if (yearTerm.length == 5) {
                getSchedule(netid, yearTerm, token)
              }
            case _ => Logger.error(s"Invalid year term value: ${yearTermResp.json}")
          }
        }
        case _ => Logger.error(s"Got an error with the current semester request: ${yearTermResp.statusText}")
      }
      
    }
  }

  def tokenRequest = WS.url("https://api.byu.edu/token")
    .withAuth(CONSUMER_KEY, CONSUMER_SECRET, WSAuthScheme.BASIC)
    .withBody(Map("grant_type" -> Seq("client_credentials")))
    .post("grant_type=client_credentials")
    
  def getEnrollment(netid: String)(implicit isInstructor: Boolean) = {
    tokenRequest.map { response =>
      if (response.status != 200) {
        Logger.error(s"Error getting aim api token: ${response.statusText}")
        Logger.error(s"AIM api token request response code: ${response.status}")
      } else {
        var token = (response.json \ "access_token").as[String]
        getCurrentSemester(netid, token)
      }
    }
  }
}

