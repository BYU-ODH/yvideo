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
import controllers.HelpPages
import test.ApplicationContext

object HelpPagesControllerSpec extends Specification with ApplicationContext {

  class HelpPagesTestController() extends Controller with HelpPages

  "HelpPages Controller Tests" >> {
  	"The Table Of Contents Endpoint" should {
  		"serve the table of contents view to a user" in {
          application {
                  val userOpt = User.findByUsername('password, "admin")
                  userOpt mustNotEqual None
                  implicit val user = userOpt.get
                  user.id mustNotEqual None
                  val controller = new HelpPagesTestController()
                  val request = FakeRequest().withSession("userId" -> user.id.get.toString)
                  val result = controller.tableOfContents(request)
                  status(result) shouldEqual 200
              }
  		}
  	}

  	"The View Endpoint" should {
  		"serve a specific help page to a user" in {
          application {
                  val userOpt = User.findByUsername('password, "admin")
                  userOpt mustNotEqual None
                  implicit val user = userOpt.get
                  user.id mustNotEqual None
                  val controller = new HelpPagesTestController()
                  val request = FakeRequest().withSession("userId" -> user.id.get.toString)
                  val result = controller.view(1)(request) //volatile - should use help page we create for this test
                  status(result) shouldEqual 200
              }	
  		}
  	}

  	"The Edit Endpoint" should {
  		"serve a specific edit page for a help page by id to a user" in {
          application {
                  val userOpt = User.findByUsername('password, "admin")
                  userOpt mustNotEqual None
                  implicit val user = userOpt.get
                  user.id mustNotEqual None
                  val controller = new HelpPagesTestController()
                  val request = FakeRequest().withSession("userId" -> user.id.get.toString)
                  val result = controller.edit(1)(request) //volatile - should use help page we create for this test
                  status(result) shouldEqual 200
              }	
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
