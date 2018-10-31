import play.api.test._
import play.api.mvc._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import models.{Content, User, Course, Collection}
import controllers.Application

object ApplicationControllerSpec extends Specification {

  class ApplicationTestController() extends Controller with Application

  "The Index Endpoint" should {
    "redirect defined users home" in {
      implicit ee: ExecutionEnv =>
          running(FakeApplication()) {
              val userOpt = User.findByUsername('password, "admin")
              userOpt mustNotEqual None
              val user = userOpt.get
              user.id mustNotEqual None
              val controller = new ApplicationTestController()
              val request = FakeRequest().withSession("userId" -> user.id.get.toString)
              val result = controller.index(request)
              status(result) shouldEqual 303
          }
    }
    "serve the index page to everyone else" in {
      implicit ee: ExecutionEnv =>
          running(FakeApplication()) {
              val controller = new ApplicationTestController()
              val request = FakeRequest()
              val result = controller.index(request)
              status(result) shouldEqual 200
          }
    }
  }

  "The Login Endpoint" should {
    "redirect defined users home" in {
      implicit ee: ExecutionEnv =>
          running(FakeApplication()) {
              val userOpt = User.findByUsername('password, "admin")
              userOpt mustNotEqual None
              val user = userOpt.get
              user.id mustNotEqual None
              val controller = new ApplicationTestController()
              val request = FakeRequest().withSession("userId" -> user.id.get.toString)
              val result = controller.login(request)
              status(result) shouldEqual 303
          }
    }
    "serve the secret login page to everyone else" in {
      implicit ee: ExecutionEnv =>
          running(FakeApplication()) {
              val controller = new ApplicationTestController()
              val request = FakeRequest()
              val result = controller.login(request)
              status(result) shouldEqual 200
          }
    }
  }

  "The Home Endpoint" should {
    "send defined users home" in {
      implicit ee: ExecutionEnv =>
          running(FakeApplication()) {
              val userOpt = User.findByUsername('password, "admin")
              userOpt mustNotEqual None
              val user = userOpt.get
              user.id mustNotEqual None
              val controller = new ApplicationTestController()
              val request = FakeRequest().withSession("userId" -> user.id.get.toString)
              val result = controller.home(request)
              status(result) shouldEqual 200
          }
    }
  }

  "The About Endpoint" should {
    "send defined users to the about page" in {
      implicit ee: ExecutionEnv =>
          running(FakeApplication()) {
              val userOpt = User.findByUsername('password, "admin")
              userOpt mustNotEqual None
              val user = userOpt.get
              user.id mustNotEqual None
              val controller = new ApplicationTestController()
              val request = FakeRequest().withSession("userId" -> user.id.get.toString)
              val result = controller.about(request)
              status(result) shouldEqual 200
          }
    }
  }

  "The Terms Endpoint" should {
    "send defined users to the terms page" in {
      implicit ee: ExecutionEnv =>
          running(FakeApplication()) {
              val userOpt = User.findByUsername('password, "admin")
              userOpt mustNotEqual None
              val user = userOpt.get
              user.id mustNotEqual None
              val controller = new ApplicationTestController()
              val request = FakeRequest().withSession("userId" -> user.id.get.toString)
              val result = controller.terms(request)
              status(result) shouldEqual 200
          }
    }
  }

  "The Policy Endpoint" should {
    "send defined users to the policy page" in {
      implicit ee: ExecutionEnv =>
          running(FakeApplication()) {
              val userOpt = User.findByUsername('password, "admin")
              userOpt mustNotEqual None
              val user = userOpt.get
              user.id mustNotEqual None
              val controller = new ApplicationTestController()
              val request = FakeRequest().withSession("userId" -> user.id.get.toString)
              val result = controller.policy(request)
              status(result) shouldEqual 200
          }
    }
  }  

  "The Get Problem Info Endpoint" should {
    "send bug report info based on user input" in {
      1 mustEqual 1
    }
  }

  "The Get Suggestion Info Endpoint" should {
    "send suggestion info based on user input" in {
      1 mustEqual 1
    }
  }

  "The Get Thought Info Endpoint" should {
    "send thought info based on user input" in {
      1 mustEqual 1
    }
  }

  "The Save Feedback Endpoint" should {
    "save feedback and send it in an email based on the feedback type" in {
      1 mustEqual 1
      // One case each for problems, suggestions, and ratings
      // Will probably need to mock the email
    }
  }

  "The Save Error Feedback Endpoint" should {
    "save error feedback based on user input and send it in an email" in {
      1 mustEqual 1
      // Will probably need to mock the email
    }
  }
}
