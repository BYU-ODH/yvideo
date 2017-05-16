import models.{Content, User, Course}
import org.specs2.mutable._
import org.specs2.execute._
import play.api.test._
import play.api.mvc

import play.api.libs.json.Json
import service.DocumentPermissionChecker

/**
 * The trait used to test the user controller
 */
trait UserController {
  this: Controller =>

  def index() = Action {
    Ok("ok")
  }
}
object UserController extends Controller with UserController

/**
 * Tests all the methods in the course model
 */
class EditableDocumentPermissionCheckerSpec extends Specification with Results {

  abstract class TestApplication extends WithApplication {
	override def around[T: AsResult](t: => T): Result = super.around {
	  setupData()
	  t
	}

	def setupData() {
	  // setup data
	}
  }

  class Users() extends Controller with UserController

  "Users" should {

	"be able to change their password" in new TestApplication {
      val user = User(None, "", 'password, "fakeuser", None)
      val usercontroller = new UserController()
      val result: Future[Result] = usercontroller.changePassword.apply(FakeRequest())
      val bodyText: String = contentAsString(result)
      println(bodyText)
      bodyText must be equalTo "ok"
    }

  }
}
