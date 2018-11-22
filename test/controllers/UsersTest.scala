import play.api.test._
import play.api.mvc._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import models.{Content, User, Course}
import controllers.Users

object UserControllerSpec extends Specification {

  class UsersTestController() extends Controller with Users

  "The Accout Settings Endpoint" should { //will potentially be removed/altered
    "serve the account settings view to authenticated users" in { 
      implicit ee: ExecutionEnv =>
          running(FakeApplication()) {
            val userOpt = User.findByUsername('password, "admin")
            userOpt mustNotEqual None
            implicit val user = userOpt.get
            user.id mustNotEqual None
            val controller = new UsersTestController()
            val request = FakeRequest().withSession("userId" -> user.id.get.toString)
            val result = controller.accountSettings(request)
            status(result) shouldEqual 200
          }
    }

  }

  "The Save Settings Endpoint" should { //will potentially be removed/altered
    "change the user settings based on the map values" in {
      1 mustEqual 1
    }
    //also redirect to the settings view
  }

  "The Change Password Endpoint" should { //will potentially be removed
    "change the user's password after ensuring they match" in {
      1 mustEqual 1
    }
    //another case for failing when passwords don't match
    //also redirect to the settings view
  }

  "The Upload Profile Picture Endpoint" should {//will potentially be removed
    "change the user's profile picture" in {
      1 mustEqual 1
    }
  }
}
