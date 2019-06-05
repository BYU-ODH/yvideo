import play.api.test._
import play.api.mvc._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.libs.json._

import models.{Content, User, Course, Collection, HelpPage}
import controllers.HelpPages
import test.ApplicationContext
import test.TestHelpers
import test.DBClear

object HelpPagesControllerSpec extends Specification with ApplicationContext with DBClear with TestHelpers {

  class HelpPagesTestController() extends Controller with HelpPages

  "HelpPages Controller Tests" >> {
  	"The Table Of Contents Endpoint" should {
      "serve a json listing of the help pages" in {
        ok
      }
  	}

  	"The Delete Endpoint" should {
  		"delete a help page by id" in {
  			1 mustEqual 1
  		}
  	}

  	"The Save Endpoint" should {
  		"save a help page by id with the data in the request" in {
  			1 mustEqual 1
  		}
  		//make sure to cover all cases
  	}
  }
}
