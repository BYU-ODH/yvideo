import play.api.test._
import play.api.mvc._
import org.specs2.mutable._
import org.specs2.matcher.JsonMatchers
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._

import models.{Content, User, Course, Collection}
import controllers.WordLists
import test.ApplicationContext
import test.TestHelpers
import test.DBClear


import models._



object WordListsControllerSpec extends Specification with ApplicationContext with DBClear with TestHelpers with JsonMatchers{

  class WordListsTestController() extends Controller with WordLists

  "WordLists Controller Tests" >> {

  	"The add endpoint should" should {
  		"add a given word to the user's word list" in {
  			application {
  				val user = newCasStudent("Bobby")
  				user.id mustNotEqual None
  				val controller = new WordListsTestController()
  				val request = FakeRequest()
  					.withSession("userId" -> user.id.get.toString)
  					.withJsonBody(Json.obj(
  						"word" -> "test",
  						"srcLang" -> "EN",
  						"destLang" -> "JP"
  					))
  				val result = call(controller.add(), request)
  				contentType(result) mustEqual Some("application/json")
  				status(result) mustEqual 200

  			}
  		}
  	}

  }
}