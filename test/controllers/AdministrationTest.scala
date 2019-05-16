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
          val admin = newCasAdmin("admin")
          admin.id mustNotEqual None
          val controller = new AdministrationTestController()
          val request = FakeRequest().withSession("userId" -> admin.id.get.toString)
          val length = users.length + 1 //one more for the admin
          val result = controller.userCount()(request)
          contentType(result) mustEqual Some("application/json")
          status(result) mustEqual 200
          val jsonResult = contentAsJson(result)
          // '6'
          val count = jsonResult.toString
          count mustEqual length.toString
        }
      }
    }

    "The Search Users Endpoint" should {
      "return a JSON of user objects based on the username" in {
        application {
          val users = List(
            customStudent("joe black", "joe@yvideo.net", "joe12345"),
            customStudent("jack blue", "jack@yvideo.net", "jack12345"),
            customStudent("john black", "john@yvideo.com", "john45678"),
            customTeacher("jill red", "jill@yvideo.com", "jill45678"),
            customManager("jane green", "jane@test.com", "jane78910"))
          users.foreach(_.id mustNotEqual None) 
          val admin = customAdmin("admin man", "admin@example.net", "admin12345")
          admin.id mustNotEqual None
          val controller = new AdministrationTestController()
          val request = FakeRequest().withSession("userId" -> admin.id.get.toString)
          // First search by username 
          val resultUsername = controller.searchUsers("username", "12345")(request)
          // [{"id":1,"authScheme":"cas","username":"joe12345","name":"joe black","email":"joe@yvideo.net","linked":-1,"permissions":["joinCollection"],"lastLogin":"2019-04-27T05:20:50.076Z"},{"id":2,"authScheme":"cas","username":"jack12345","name":"jack blue","email":"jack@yvideo.net","linked":-1,"permissions":["joinCollection"],"lastLogin":"2019-04-27T05:20:50.081Z"},{"id":6,"authScheme":"cas","username":"admin12345","name":"admin man","email":"admin@yvideo.net","linked":-1,"permissions":["admin","delete"],"lastLogin":"2019-04-27T05:20:50.100Z"}]
          status(resultUsername) mustEqual 200
          contentType(resultUsername) mustEqual Some("application/json")
          val jsonUsername = contentAsJson(resultUsername).as[List[JsValue]]
          jsonUsername.foreach { user =>
            val us = user.toString
            us must /("id" -> anyValue)
            us must /("authScheme" -> anyValue)
            us must /("name" -> anyValue)
            us must /("email" -> anyValue)
            us must /("linked" -> -1)
            us must /("permissions" -> anyValue)
            us must /("lastLogin" -> anyValue)
          }
          jsonUsername.length mustEqual 3
        }
      }

      "return a JSON of user objects based on the name" in {
        application {
          val users = List(
            customStudent("joe black", "joe@yvideo.net", "joe12345"),
            customStudent("jack blue", "jack@yvideo.net", "jack12345"),
            customStudent("john black", "john@yvideo.com", "john45678"),
            customTeacher("jill red", "jill@yvideo.com", "jill45678"),
            customManager("jane green", "jane@test.com", "jane78910"))
          users.foreach(_.id mustNotEqual None) 
          val admin = customAdmin("admin man", "admin@example.net", "admin12345")
          admin.id mustNotEqual None
          val controller = new AdministrationTestController()
          val request = FakeRequest().withSession("userId" -> admin.id.get.toString)
          // Now search by name
          val resultName = controller.searchUsers("name", "black")(request)
          status(resultName) mustEqual 200
          contentType(resultName) mustEqual Some("application/json")
          val jsonName = contentAsJson(resultName).as[List[JsValue]]
          jsonName.foreach { user =>
            val us = user.toString
            us must /("id" -> anyValue)
            us must /("authScheme" -> anyValue)
            us must /("name" -> anyValue)
            us must /("email" -> anyValue)
            us must /("linked" -> -1)
            us must /("permissions" -> anyValue)
            us must /("lastLogin" -> anyValue)
          }
          jsonName.length mustEqual 2
        }
      }

      "return a JSON of user objects based on the name" in {
        application {
          val users = List(
            customStudent("joe black", "joe@yvideo.net", "joe12345"),
            customStudent("jack blue", "jack@yvideo.net", "jack12345"),
            customStudent("john black", "john@yvideo.com", "john45678"),
            customTeacher("jill red", "jill@yvideo.com", "jill45678"),
            customManager("jane green", "jane@test.com", "jane78910"))
          users.foreach(_.id mustNotEqual None) 
          val admin = customAdmin("admin man", "admin@example.net", "admin12345")
          admin.id mustNotEqual None
          val controller = new AdministrationTestController()
          val request = FakeRequest().withSession("userId" -> admin.id.get.toString)
          // Finally search by email
          val resultEmail = controller.searchUsers("email", "yvideo")(request)
          status(resultEmail) mustEqual 200
          contentType(resultEmail) mustEqual Some("application/json")
          val jsonEmail = contentAsJson(resultEmail).as[List[JsValue]]
          jsonEmail.foreach { user =>
            val us = user.toString
            us must /("id" -> anyValue)
            us must /("authScheme" -> anyValue)
            us must /("name" -> anyValue)
            us must /("email" -> anyValue)
            us must /("linked" -> -1)
            us must /("permissions" -> anyValue)
            us must /("lastLogin" -> anyValue)
          }
          jsonEmail.length mustEqual 4
        }
      }

      "return a forbidden when the search column is bad" in {
        application {
          val admin = newCasAdmin("admin")
          admin.id mustNotEqual None
          val controller = new AdministrationTestController()
          val request = FakeRequest().withSession("userId" -> admin.id.get.toString)
          val resultBadColumn = controller.searchUsers("toast", "yvideo")(request)
          contentType(resultBadColumn) mustEqual Some("application/json")
          status(resultBadColumn) mustEqual 403
          val jsonBadColumn = contentAsJson(resultBadColumn)
          val expectedError = """{"message":"Search column is not allowed"}"""
          jsonBadColumn.toString mustEqual expectedError
        }
      }

      "return a forbidden when the search value is too short" in {
        application {
          val admin = newCasAdmin("admin")
          admin.id mustNotEqual None
          val controller = new AdministrationTestController()
          val request = FakeRequest().withSession("userId" -> admin.id.get.toString)
          val resultBadColumn = controller.searchUsers("email", "abc")(request)
          contentType(resultBadColumn) mustEqual Some("application/json")
          status(resultBadColumn) mustEqual 403
          val jsonBadColumn = contentAsJson(resultBadColumn)
          val expectedError = """{"message":"Search value was too short"}"""
          jsonBadColumn.toString mustEqual expectedError
        }
      }
    }

    "The Send Notification Endpoint" should {
      "send a notification to a giver user with a message" in {
        application {
          val admin = customAdmin("admin man", "arclitelab@gmail.com", "admin12345")
          admin.id mustNotEqual None
          val controller = new AdministrationTestController()
          val request = FakeRequest()
            .withSession("userId" -> admin.id.get.toString)
            .withFormUrlEncodedBody(
              "userId" -> admin.id.get.toString,
              "message" -> "hello there!"
            )
          val result = call(controller.sendNotification, request)
          contentType(result) mustEqual Some("application/json")
          status(result) mustEqual 200
          val jsonResult = contentAsJson(result)
          val expectedResult = """{"message":"Notification sent to admin12345"}"""
          jsonResult.toString mustEqual expectedResult
        }
      }
    }

    "The Delete Endpoint" should {
      "delete a user based on the id" in {
        application {
          val user = newCasStudent("Bob")
          user.id mustNotEqual None
          val userId = user.id.get
          val admin = newCasAdmin("admin")
          admin.id mustNotEqual None
          val controller = new AdministrationTestController()
          val request = FakeRequest().withSession("userId" -> admin.id.get.toString)
          // Make sure they're both in the db
          val countBefore = controller.userCount()(request)
          contentAsJson(countBefore).toString mustEqual "2"
          val result = controller.delete(user.id.get)(request)
          contentType(result) mustEqual Some("application/json")
          status(result) mustEqual 200
          val jsonResult = contentAsJson(result)
          val expectedResult = """{"message":"User deleted"}"""
          jsonResult.toString mustEqual expectedResult
          // And check that the user is gone
          User.findById(userId) mustEqual None
          val countAfter = controller.userCount()(request)
          contentAsJson(countAfter).toString mustEqual "1"
        }
      }
    }

    "The Delete Collection Endpoint" should {
      "delete the collection if the current user is an admin" in {
        application {
          val admin = newCasAdmin("admin")
          admin.id mustNotEqual None
          val collection = newCollection("Test Collection", admin)
          collection.id mustNotEqual None
          val collectionId = collection.id.get
          val controller = new AdministrationTestController()
          val request = FakeRequest().withSession("userId" -> admin.id.get.toString)
          val result = controller.deleteCollection(collection.id.get)(request)
          contentType(result) mustEqual Some("application/json")
          status(result) mustEqual 200
          val jsonResult = contentAsJson(result)
          val expectedResult = """{"message":"Collection deleted"}"""
          jsonResult.toString mustEqual expectedResult
          Collection.findById(collectionId) mustEqual None
        }
      }
    }

    "The Save Site Settings Endpoint" should {
      "update the site settings with the map of values" in {
        application {
          val admin = newCasAdmin("admin")
          admin.id mustNotEqual None
          val controller = new AdministrationTestController()
          val request = FakeRequest()
            .withSession("userId" -> admin.id.get.toString)
            .withFormUrlEncodedBody(
              "notifications.emails" -> "true"
            )
          val result = call(controller.saveSiteSettings, request)
          contentType(result) mustEqual 200
        }
      }
    }

    "The Proxy Endpoint" should {
      "allow an admin to log in as the user" in {
        application {
          val user = newCasStudent("Bob")
          user.id mustNotEqual None
          val userId = user.id.get
          val admin = newCasAdmin("admin")
          admin.id mustNotEqual None
          val controller = new AdministrationTestController()
          val request = FakeRequest().withSession("userId" -> admin.id.get.toString)
          // Make sure they're both in the db
          val result = controller.proxy(user.id.get)(request)
          contentType(result) mustEqual Some("application/json")
          status(result) mustEqual 200
          val jsonResult = contentAsJson(result)
          val expectedResult = """{"message":"Now proxying as user mynetid"}"""
          jsonResult.toString mustEqual expectedResult
        }
      }
    }
  }
}
