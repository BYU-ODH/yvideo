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

  "The Notifications Endpoint" should {
    "serve the notification view to authenticated users" in {
      implicit ee: ExecutionEnv =>
          running(FakeApplication()) {
            val userOpt = User.findByUsername('password, "admin")
            userOpt mustNotEqual None
            implicit val user = userOpt.get
            user.id mustNotEqual None
            val controller = new UsersTestController()
            val request = FakeRequest().withSession("userId" -> user.id.get.toString)
            val result = controller.notifications(request)
            status(result) shouldEqual 200
          }
    }
  }

  "The Accout Settings Endpoint" should {
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
}
