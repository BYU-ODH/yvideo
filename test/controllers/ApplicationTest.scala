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
                val userOpt = User.findByUsername('password, "student1")
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
              val userOpt = User.findByUsername('password, "student1")
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

}
