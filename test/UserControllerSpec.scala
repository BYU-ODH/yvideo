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

  "The User Controller" should {

    "return account settings" in { implicit ee: ExecutionEnv =>
      running(FakeApplication()) {

        implicit val user = User(None, "", 'password, "fakeuser", None)
        val controller = new UsersTestController()
        val result = controller.accountSettings(FakeRequest())

        result map ( res => res.header.status shouldEqual 200) await
      }
    }

  }
}
