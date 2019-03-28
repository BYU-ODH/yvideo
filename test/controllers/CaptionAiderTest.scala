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
import controllers.CaptionAider
import test.ApplicationContext

object CaptionAiderControllerSpec extends Specification with ApplicationContext {

  class CaptionAiderTestController() extends Controller with CaptionAider

  "CaptionAider Controller Tests" >> {

    "The View Endpoint" should {
      "serve the CaptionAider page to users" in {
        application {
          val userOpt = User.findByUsername('password, "admin")
          implicit val user = userOpt.get
          user.id mustNotEqual None
          val controller = new CaptionAiderTestController()
          val request = FakeRequest().withSession("userId" -> user.id.get.toString)
          val result = controller.view(1,0)(request) //volatile - we need to add a specific content and use that instead of 9
          status(result) shouldEqual 200
        }
        //should we make sure that users who aren't collection TAs or higher can't view?
        //also 0 for collection id isn't going to fly anymore since all content needs a collection from now on
      }
    }

    "The Save Endpoint" should {
      "save the CaptionAider track" in {
        1 mustEqual 1
      }
      //this is going to have a lot of cases to cover so make sure to be thorough
      //probably going to use a lot of mocks as well since we don't want to make lots of fake resource library entries
    }
  }
}
