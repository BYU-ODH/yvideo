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

  	"The word list editing endpoints should" should {
  		// This test also tests the viewJson and deleteWord functions
  		"add a given word to the user's word list and then delete it" in {
  			application {
  				val user = newCasStudent("Bobby")
  				user.id mustNotEqual None
  				val controller = new WordListsTestController()
  				
  				// First make sure that they have an empty word list
  				val checkRequest = FakeRequest().withSession("userId" -> user.id.get.toString)
  				val wordListBefore = controller.viewJson()(checkRequest)
  				contentType(wordListBefore) mustEqual Some("application/json")
  				status(wordListBefore) mustEqual 200
  				val jsonBefore = contentAsJson(wordListBefore)
  				jsonBefore.toString mustEqual """{"wordList":[]}"""

  				// Then add the new word
  				val request = FakeRequest()
  					.withSession("userId" -> user.id.get.toString)
  					.withJsonBody(Json.obj(
  						"word" -> "test",
  						"srcLang" -> "EN",
  						"destLang" -> "JP"
  					))
  				val addResult = call(controller.add, request)
  				contentType(addResult) mustEqual Some("application/json")
  				status(addResult) mustEqual 200
  				val jsonAddResult = contentAsJson(addResult)
  				(jsonAddResult \ "message").as[String] mustEqual "Word added."

  				// Now make sure the new word is added
  				val wordListAfter = controller.viewJson()(checkRequest)
  				contentType(wordListAfter) mustEqual Some("application/json")
  				status(wordListAfter) mustEqual 200
  				val jsonAfter = contentAsJson(wordListAfter)
  				jsonAfter.toString mustEqual """{"wordList":[{"id":1,"word":"test","srcLang":"EN","destLang":"JP"}]}"""

  				// Now delete the word
  				val deleteResult = controller.deleteWord(1)(checkRequest)
  				contentType(deleteResult) mustEqual Some("application/json")
  				status(deleteResult) mustEqual 200
  				val jsonDeleteResult = contentAsJson(deleteResult)
  				(jsonDeleteResult \ "message").as[String] mustEqual "Word deleted."

  				val finalResult = controller.viewJson()(checkRequest)
  				contentType(finalResult) mustEqual Some("application/json")
  				status(finalResult) mustEqual 200
  				val finalJson = contentAsJson(finalResult)
  				finalJson.toString mustEqual """{"wordList":[]}"""
  			}
  		}
  	}

  }
}