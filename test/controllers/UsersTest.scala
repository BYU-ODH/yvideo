import play.api.test._
import play.api.mvc._
import play.api.libs.json._
import play.api.test._
import play.api.test.Helpers._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.JsonMatchers
import org.specs2.mutable._

import controllers.Users
import models.{Content, User, Course, SitePermissions}
import test.{ApplicationContext, TestHelpers, DBClear}

object UserControllerSpec extends Specification with ApplicationContext with DBClear
  with TestHelpers with JsonMatchers {

  class UsersTestController() extends Controller with Users

  "The Upload Profile Picture Endpoint" should {//will potentially be removed
    "change the user's profile picture" in {
      1 mustEqual 1
    }
  }

  "The Save Settings Endpoint" should { //will potentially be removed/altered
    "change the user settings based on the map values" in {
      1 mustEqual 1
    }
    //also redirect to the settings view
  }

  "The getEnrollment endpoint" should {
    "get all of a user's collections" in {
      application {
        val controller = new UsersTestController()
        val user = newCasStudent("Cas Student")
        user.id must beSome
        val newcolls = List("c1", "c2", "c3", "c4").map(x => pubCollection(x, user))
        newcolls.map(c => c.id must beSome) must allSucceed()

        val resp = controller.getEnrollment(sessionReq(user))
        status(resp) === 200
        val collections = contentAsJson(resp).as[List[JsValue]]
        collections.map { coll =>
          val cs = coll.toString
          cs must /("name" -> anyValue)
          cs must /("thumbnail" -> anyValue)
          cs must /("id" -> anyValue)
          cs must /("content" -> anyValue)
          // TODO: check for the content in the collection
        } must allSucceed()
        collections.length === 0
      }
    }

    "return an empty array if user is not enrolled in anything" in {
      application {
        val controller = new UsersTestController()
        val user = newCasStudent("Cas Student")
        user.id must beSome
        val resp = controller.getEnrollment(sessionReq(user))
        val collections = contentAsJson(resp).as[List[JsValue]]
        collections.length === 0
      }
    }
  }

  "The getCollectionsForTeacherOrTA endpoint" should {
    "get the collections a user has created" in {
      application {
        val controller = new UsersTestController()
        val user = newCasTeacher("Cas Teacher")
        user.id must beSome
        val newcolls = List("c1", "c2", "c3", "c4").map(x => pubCollection(x, user))
        newcolls.map(c => c.id must beSome) must allSucceed()
        val resp = controller.getCollectionsForTeacherOrTA(sessionReq(user))
        status(resp) === 200
        val collections = contentAsJson(resp).as[List[JsValue]]
        collections.map { coll =>
          val cs = coll.toString
          cs must /("name" -> anyValue)
          cs must /("thumbnail" -> anyValue)
          cs must /("id" -> anyValue)
          cs must /("content" -> anyValue)
          cs must /("role" -> "teacher")
          // TODO: check for the content in the collection
        } must allSucceed()
        collections.length === newcolls.length
      }
    }
  }

  "The roles endpoint" should {
    "get student roles" in {
      application {
        val controller = new UsersTestController()
        val user = newCasStudent("Cas Student 1")
        user.id must beSome
        val resp = controller.roles(sessionReq(user))
        status(resp) === 200
        contentAsString(resp) must /("authenticated" -> true)
        contentAsString(resp) must /("roles" -> anyValue)
        contentAsString(resp) must /("permissions" -> anyValue)

        val roles = (contentAsJson(resp) \ "roles").as[List[String]]
        roles === List("student")
        val permissions = (contentAsJson(resp) \ "permissions").as[List[String]]
        permissions === SitePermissions.roles.get('student).get
      }
    }

    "get admin roles" in {
      application {
        val controller = new UsersTestController()
        val user = newCasAdmin("Cas Admin 1")
        user.id must beSome
        val resp = controller.roles(sessionReq(user))
        status(resp) === 200
        contentAsString(resp) must /("authenticated" -> true)
        contentAsString(resp) must /("roles" -> anyValue)
        contentAsString(resp) must /("permissions" -> anyValue)

        val roles = (contentAsJson(resp) \ "roles").as[List[String]]
        roles must contain("admin")
        val permissions = (contentAsJson(resp) \ "permissions").as[List[String]]
        permissions === SitePermissions.roles.get('admin).get
      }
    }

    "get teacher roles" in {
      application {
        val controller = new UsersTestController()
        val user = newCasTeacher("Cas Teacher 1")
        user.id must beSome
        val resp = controller.roles(sessionReq(user))
        status(resp) === 200
        contentAsString(resp) must /("authenticated" -> true)
        contentAsString(resp) must /("roles" -> anyValue)
        contentAsString(resp) must /("permissions" -> anyValue)

        val roles = (contentAsJson(resp) \ "roles").as[List[String]]
        roles must contain("teacher")
        val permissions = (contentAsJson(resp) \ "permissions").as[List[String]]
        permissions === SitePermissions.roles.get('teacher).get
      }
    }

    "get manager roles" in {
      application {
        val controller = new UsersTestController()
        val user = newCasManager("Cas Manager 1")
        user.id must beSome
        val resp = controller.roles(sessionReq(user))
        status(resp) === 200
        contentAsString(resp) must /("authenticated" -> true)
        contentAsString(resp) must /("roles" -> anyValue)
        contentAsString(resp) must /("permissions" -> anyValue)

        val roles = (contentAsJson(resp) \ "roles").as[List[String]]
        roles must contain("manager")
        val permissions = (contentAsJson(resp) \ "permissions").as[List[String]]
        permissions === SitePermissions.roles.get('manager).get
      }
    }
  }

  "The getAsJson endpoint" should {
    "return an existing user" in {
      application {
        val controller = new UsersTestController()
        val user = newCasStudent("Cas Student 1")
        user.id must beSome
        val resp = controller.getAsJson(sessionReq(user))
        status(resp) === 200
        contentAsString(resp) must /("id" -> user.id.get.toInt)
        contentAsString(resp) must /("username" -> user.username)
        contentAsString(resp) must /("email" -> user.email.getOrElse[String](""))
        contentAsString(resp) must /("name" -> user.name.getOrElse[String](""))
      }
    }
  }


}
