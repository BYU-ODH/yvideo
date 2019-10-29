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
        collections.length === 4
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

}
