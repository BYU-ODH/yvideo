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
import controllers.BYUSecure

object BYUSecureControllerSpec extends Specification {

  class BYUSecureTestController() extends Controller with BYUSecure

  "BYUSecure Controller Tests" >> {

  	"The Build URL Endpoint" should {
  		"build a url for logging in through cas" in {
  			1 mustEqual 1
  		}
  	}
  }
}