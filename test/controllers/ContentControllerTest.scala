import play.api.test._
import play.api.mvc._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.libs.json._

import models.{Content, User, Course, Collection}
import controllers.ContentController
import test.ApplicationContext
import test.TestHelpers
import test.DBClear

object ContentControllerSpec extends Specification with ApplicationContext with DBClear with TestHelpers {

  class ContentTestController() extends Controller with ContentController

  "ContentController Controller Tests" >> {

  	"The Get Content Endpoint" should {
  		"return a map of content from a request" in {
  			1 mustEqual 1
  		}
  	}

  	"The Get As Json Endpoint" should {
  		"return a content by id as Json" in {
  			//implicit ee: ExecutionEnv =>
            //running(FakeApplication()) {
                //val userOpt = User.findByUsername('password, "admin")
                //userOpt mustNotEqual None
                //implicit val user = userOpt.get
                //user.id mustNotEqual None
                //val controller = new ContentTestController()
                //val request = FakeRequest().withSession("userId" -> user.id.get.toString)
                //val result:String = controller.contentAsJson(1)(request) //volatile - create a content and then use it
                //val jsonResult = contentAsJson(result)
                //contentType(result) mustEqual Some("application/json")
                //val jsVal: JsValue = Json.parse(jsonResult.toString)
                //val id = (jsVal \\ "id")
                //val name = (jsVal \\ "name")
                //id(0).toString mustEqual "1"
                //name(0).toString mustEqual "\"Dreyfus by Yves Duteil\"" //update this with the content created for this test
            //}
            //forbidden if content isn't visible by the user
            //check more than just the result being json
            ok
  		}
  	}

  	"The Create Page Endpoint" should {
  		"serve the content creation page based on the string passed in" in {
          application {
                val user = newCasAdmin("admin")
                user.id mustNotEqual None
                val controller = new ContentTestController()
                val request = FakeRequest().withSession("userId" -> user.id.get.toString)
                val pageList = List("url", "batch", "resource", "playlist", "questions", "nothing")
                for(x <- pageList) {
                		val result = controller.createPage(x,7)(request)
                		status(result) shouldEqual 200
                }
                1 mustEqual 1 //this assertion needs to be here to prevent syntax errors
  					}
  		}
  	}

  	"The Process Url Endpoint" should {
  		"return the url as is when it doesn't need to be encoded" in {
  				val controller = new ContentTestController()
					val url = controller.processUrl("http://www.yvideo.byu.edu")
					url mustEqual "http://www.yvideo.byu.edu"	
  		}
  		//add another case for when it does need to be encoded
  	}

  	"The Create From Batch Endpoint" should {
  		"create a batch of content for the collection by id using the data in the request" in {
  			1 mustEqual 1
  		}
  		//we'll need to mock the resource library here to avoid cluttering it
  		//make sure to be thorough and add all possible cases
  	}

  	"The Create From Url Endpoint" should {
  		"create content for the collection by id using the url and data in the request" in {
  			1 mustEqual 1
  		}
  		//we'll need to mock the resource library here to avoid cluttering it
  		//make sure to be thorough and add all possible cases
  	}

  	"The Create From File Endpoint" should {
  		"create content for the collection by id using the file and data in the request" in {
  			1 mustEqual 1
  		}
  		//we'll need to mock the resource library here to avoid cluttering it
  		//make sure to be thorough and add all possible cases
  	}

  	"The Create From Resource Endpoint" should {
  		"create content for the collection by id using the resource id and data in the request" in {
  			1 mustEqual 1
  		}
  	}

  	"The Create Playlist Endpoint" should {
  		"create a playlist for the collection" in { //this will probably be converted into a cliplist function
  			1 mustEqual 1
  		}
  	}

  	"The Create Question Set Endpoint" should { //will probably be deleted
  		"do something?" in {
  			1 mustEqual 1
  		}
  	}

  	"The View Endpoint" should {
  		"serve the content page to the user if they are allowed" in {
  			1 mustEqual 1
  		}
  		//we'll need to make lots of cases or simplify the function to only do what it has to do
  	}

  	"The Manage Content Endpoint" should { //will probably be deleted
  		"serve the manage content page to the current user" in {
  			1 mustEqual 1
  		}
  	}

  	"The Delete Endpoint" should {
  		"delete content by id" in {
  			1 mustEqual 1
  		}
  		//forbidden if user is not admin
  	}

  	"The Mine Endpoint" should {
  		"serves the my content page to the current user" in { //will probably be deleted
  			1 mustEqual 1
  		}
  	}
  }
}
