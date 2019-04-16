import play.api.test._
import play.api.mvc._
import org.specs2.mutable._
import org.specs2.matcher.JsonMatchers
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._

import models.{Content, User, Course, Collection}
import controllers.Administration
import test.ApplicationContext
import test.TestHelpers
import test.DBClear

object AdministrationControllerSpec extends Specification with ApplicationContext with DBClear with TestHelpers with JsonMatchers{

  class AdministrationTestController() extends Controller with Administration

  "Administration Controller Tests" >> {
    // Authentication.enforcePermission("admin") will be tested in the Authentication Controller 

    "The User Count Endpoint" should {
      "return the number of users in the database" in {
        application {
          val users = List("joe", "jack", "john", "jill", "jane") map newCasStudent
          users.foreach(_.id mustNotEqual None)
          val admin = newCasAdmin("admin3")
          admin.id mustNotEqual None
          val controller = new AdministrationTestController()
          val request = FakeRequest().withSession("userId" -> admin.id.get.toString)
          val length = users.length + 1 //one more for the admin
          val result = controller.userCount()(request)
          contentType(result) mustEqual Some("application/json")
          val jsonResult = contentAsJson(result)
          // '6'
          val count = jsonResult.toString
          count mustEqual length.toString
        }
      }
    }

    "The Search Users Endpoint" should {
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
      //all of these should also redirect with info message
      "remove the permissions" in {
        1 mustEqual 1
      }

      "match the permissions" in {
        1 mustEqual 1
      }

      "add the permissions" in {
        1 mustEqual 1
      }
    }

    "The Send Notification Endpoint" should {
      //should also redirect based on the current page with info message
      "send a notification to a giver user with a message" in {
        1 mustEqual 1
      }
    }

    "The Delete Endpoint" should {
      //should also redirect with info message
      "delete a user based on the id" in {
        1 mustEqual 1
      }
    }

    "The Edit Collection Endpoint" should {
      "update a collection with the map of values" in {
        1 mustEqual 1
      }
    }

    "The Delete Collection Endpoint" should {
      "delete the collection if the current user is the collection teacher or an admin" in {
        1 mustEqual 1
      }
    }
    //And be rejected if they aren't either one

    "The Batch Update Content Endpoint" should {
      "update multiple content items with the map of values" in {
        1 mustEqual 1
      }
    }
    //And throw an error if something goes wrong

    "The Create Home Page Content Endpoint" should {
      "create a banner for displaying on the homepage with the map of values" in {
        1 mustEqual 1
      }
    }
    //Have a case for the background being empty
    //Throw an error if something goes wrong

    "The Toggle Home Page Content Endpoint" should {
      "toggle an existing homepage banner to be active/inactive" in {
        1 mustEqual 1
      }
    }

    "The Delete Home Page Content Endpoint" should {
      "delete an existing homepage banner" in {
        1 mustEqual 1
      }
    }

    "The Proxy Endpoint" should {
      "allow an admin to log in as the user" in {
        1 mustEqual 1
      }
    }
    //And fail if the user is not found
  }
}
