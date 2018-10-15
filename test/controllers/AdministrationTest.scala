import play.api.test._
import play.api.mvc._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import models.{Content, User, Course, Collection}
import controllers.Administration

object AdministrationControllerSpec extends Specification {

  class AdministrationTestController() extends Controller with Administration

  "Administration Controller Tests" >> {

    "The Admin Dashboard Endpoint" should {
      "redirect non-authenticated users" in {
          implicit ee: ExecutionEnv =>
              running(FakeApplication()) {
                  val userOpt = User.findByUsername('password, "student1")
                  userOpt mustNotEqual None
                  implicit val user = userOpt.get
                  user.id mustNotEqual None
                  val controller = new AdministrationTestController()

                  val result = controller.admin()(FakeRequest())

                  result map ( res => {
                      res.header.status shouldEqual 303
                  }) await
          }
      }

      "serve the admin dashboard to admins" in {
          implicit ee: ExecutionEnv =>
              running(FakeApplication()) {
                  val userOpt = User.findByUsername('password, "admin")
                  userOpt mustNotEqual None
                  implicit val user = userOpt.get
                  user.id mustNotEqual None
                  val controller = new AdministrationTestController()
                  val request = FakeRequest().withSession("userId" -> user.id.get.toString)

                  val result = controller.admin(request)

                  result map ( res => {
                      res.header.status shouldEqual 200
                  }) await
          }
      }

      "redirect non-existant users" in {
          implicit ee: ExecutionEnv =>
              running(FakeApplication()) {
                  val controller = new AdministrationTestController()
                  val request = FakeRequest().withSession("userId" -> "9999")
                  val result = controller.admin(request)

                  result map ( res => {
                      res.header.status shouldEqual 303
                  }) await
          }
      }
    }

    "The Manage Users Endpoint" should {
      "redirect non-authenticated users" in {
          implicit ee: ExecutionEnv =>
              running(FakeApplication()) {
                  val userOpt = User.findByUsername('password, "student1")
                  userOpt mustNotEqual None
                  implicit val user = userOpt.get
                  user.id mustNotEqual None
                  val controller = new AdministrationTestController()
                  val result = controller.manageUsers(FakeRequest())

                  result map ( res => {
                      res.header.status shouldEqual 303
                  }) await
          }
      }

      "serve the admin dashboard user view to admins" in {
          implicit ee: ExecutionEnv =>
              running(FakeApplication()) {
                  val userOpt = User.findByUsername('password, "admin")
                  userOpt mustNotEqual None
                  implicit val user = userOpt.get
                  user.id mustNotEqual None
                  val controller = new AdministrationTestController()
                  val request = FakeRequest().withSession("userId" -> user.id.get.toString)
                  val result = controller.manageUsers(request)

                  result map ( res => {
                      res.header.status shouldEqual 200
                  }) await
          }
      }

      "redirect non-existant users" in {
          implicit ee: ExecutionEnv =>
              running(FakeApplication()) {
                  val controller = new AdministrationTestController
                  val request = FakeRequest().withSession("userId" -> "9999")
                  val result = controller.manageUsers(request)

                  result map ( res => {
                      res.header.status shouldEqual 303
                  }) await
          }
      }
    }
  }
}
