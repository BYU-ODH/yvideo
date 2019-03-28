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
import controllers.DocumentManager
import test.ApplicationContext
import test.TestHelpers

object DocumentManagerSpec extends Specification with ApplicationContext with TestHelpers {

  class DocumentManagerTestController() extends Controller with DocumentManager

  "DocumentManager Controller Tests" >> {

    "The Edit Annotations Endpoint" should {
      "serve the annotation editor page to a user" in {
        application {
          val user = newCasAdmin("admin")
          user.id mustNotEqual None
          val controller = new DocumentManagerTestController()
          val request = FakeRequest().withSession("userId" -> user.id.get.toString)
          val result = controller.editAnnotations(1)(request) //volatile - use content created for this test
          status(result) shouldEqual 200
        }
      }
    }

    "The Save Annotations Endpoint" should {
      "save annotations for a content based on the request data" in {
          1 mustEqual 1
      }
      //lots of cases to cover here
      //mock the resource library
    }

    "The Save Edited Annotations Endpoint" should {
      "save annotations edited in the annotation editor" in {
          1 mustEqual 1
      }
      //lots of cases to cover here
      //mock the resource library
    }

    "The Delete Document Endpoint" should {
      "delete a document from the resource library and all relations to it" in {
          1 mustEqual 1
      }
      //lots of cases to cover here
      //mock the resource library
    }
  }
}
