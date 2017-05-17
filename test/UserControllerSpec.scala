import models.{Content, User, Course}
import controllers.Users

import play.api.test._
import play.api.mvc._
import org.specs2.mock._
import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._
import scala.concurrent.Future

object UserControllerSpec extends Specification with Mockito {

  class UsersTestController() extends Controller with Users

  "The User Controller" should {

	"return account settings" in {
      running(FakeApplication()) {

        implicit val user = User(None, "", 'password, "fakeuser", None)
        val controller = new UsersTestController()
        val result = controller.accountSettings(FakeRequest())
        println(result.header.toString)

        result.header.status shouldEqual 200

      }
    }

  }
}
