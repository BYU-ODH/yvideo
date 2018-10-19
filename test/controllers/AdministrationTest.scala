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
                  val request = FakeRequest().withSession("userId" -> "-1")
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
                  val request = FakeRequest().withSession("userId" -> "-1")
                  val result = controller.manageUsers(request)

                  result map ( res => {
                      res.header.status shouldEqual 303
                  }) await
          }
      }
    }

    "The Paged Users Endpoint" should {
      "redirect non-authenticated users" in {
        1 mustEqual 1
      }

      "return a JSON of user objects with the given length and starting point" in {
        1 mustEqual 1
        //must be of type JSON
        //must be length 'limit'
        //must start with 'id'
      }
    }

    "The User Count Endpoint" should {
      "redirect non-authenticated users" in {
        1 mustEqual 1
      }      

      "return the number of users in the database" in {
        1 mustEqual 1
        //must be of type JSON
        //number in JSON should match the number of users in the database
      }
    }

    "The Search Users Endpoint" should {
      "redirect non-authenticated users" in {
        1 mustEqual 1
      }

      "return a JSON of user objects based on the search criteria" in {
        1 mustEqual 1
        //must be of type JSON
        //search based on username
        //search based on email
        //search based on name
        //error from search value too short
        //error from bad search column
      }
    }

    "The Get User Endpoint" should {
      //redirect non-authenticated users?
      "return a single user object from an id" in {
        1 mustEqual 1
        //return error not found if user doesn't exist
      }
    }

    //consider updating this endpoint to fit more with Y-Video (teacher, student, TA, etc.)
    "The Set Permissions Endpoint" should {
      "redirect non-authenticated users" in {
        1 mustEqual 1
      }
      //all of these should also redirect with info message
      "remove the given permissions" in {
        1 mustEqual 1
      }

      "match the given permissions" in {
        1 mustEqual 1
      }

      "add the given permissions" in {
        1 mustEqual 1
      }
    }

    "The Send Notification Endpoint" should {
      "redirect non-authenticated users" in {
        1 mustEqual 1
      }
      //should also redirect based on the current page with info message
      "send a notification to a giver user with a given message" in {
        1 mustEqual 1
      }
    }

    "The Delete Endpoint" should {
      "redirect non-authenticated users" in {
        1 mustEqual 1
      }
      //should also redirect with info message
      "delete a user based on the given id" in {
        1 mustEqual 1
      }
    }

    "The Manage Collections Endpoint" should {
      "redirect non-authenticated users" in {
        implicit ee: ExecutionEnv =>
              running(FakeApplication()) {
                  val userOpt = User.findByUsername('password, "student1")
                  userOpt mustNotEqual None
                  implicit val user = userOpt.get
                  user.id mustNotEqual None
                  val controller = new AdministrationTestController()

                  val result = controller.manageCollections()(FakeRequest())

                  result map ( res => {
                      res.header.status shouldEqual 303
                  }) await
          }
      }

      "serve the manage collections page to admins" in {
          implicit ee: ExecutionEnv =>
              running(FakeApplication()) {
                  val userOpt = User.findByUsername('password, "admin")
                  userOpt mustNotEqual None
                  implicit val user = userOpt.get
                  user.id mustNotEqual None
                  val controller = new AdministrationTestController()
                  val request = FakeRequest().withSession("userId" -> user.id.get.toString)

                  val result = controller.manageCollections(request)

                  result map ( res => {
                      res.header.status shouldEqual 200
                  }) await
          }
      }

      "redirect non-existant users" in {
          implicit ee: ExecutionEnv =>
              running(FakeApplication()) {
                  val controller = new AdministrationTestController()
                  val request = FakeRequest().withSession("userId" -> "-1")
                  val result = controller.manageCollections(request)

                  result map ( res => {
                      res.header.status shouldEqual 303
                  }) await
          }
      }


    }
  }
}
