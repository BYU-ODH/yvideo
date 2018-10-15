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

  "The Application Controller" should {

    "allow people with userId cookies to login" in {
        implicit ee: ExecutionEnv =>
            running(FakeApplication()) {
                val userOpt = User.findByUsername('password, "admin")
                userOpt mustNotEqual None
                val user = userOpt.get
                user.id mustNotEqual None
                val controller = new ApplicationTestController()
                val request = FakeRequest().withSession("userId" -> user.id.get.toString)

                val result = controller.login(FakeRequest())

                result map ( res => {
                    res.header.status shouldEqual 200
                }) await
        }
    }

    "redirect invalid cookies" in {
        implicit ee: ExecutionEnv =>
            running(FakeApplication()) {
                val userOpt = User.findByUsername('password, "student1")
                userOpt mustNotEqual None
                val user = userOpt.get
                user.id mustNotEqual None
                val controller = new ApplicationTestController()
                val request = FakeRequest().withSession("userId" -> user.id.get.toString)

                val result = controller.login(FakeRequest())

                result map ( res => {
                    res.header.status shouldEqual 200
                }) await
        }
    }

  }
}
