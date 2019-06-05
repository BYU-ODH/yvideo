import play.api.test._
import play.api.mvc._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.libs.json._
import play.api.Logger

import models.{Content, User, Course, Collection}
import controllers.ContentEditing
import test.{ApplicationContext, TestHelpers, DBClear}

object ContentEditingControllerSpec extends Specification with ApplicationContext with DBClear with TestHelpers {

  class ContentEditingTestController() extends Controller with ContentEditing

  "ContentEditing Controller Tests" >> {
      "The Set Metadata Endpoint" should {
          "edit content metadata by id" in {
              1 mustEqual 1
          }
          //lots of cases to cover here
          //mock the resource library
      }

      "The Record Settings Helper Function" should {
          "extract content settings from the data object or nil if not found" in {
              1 mustEqual 1
          }
      }

      "The Set Settings Endpoint" should {
          "update content settings by id" in {
            val controller = new ContentEditingTestController()
            val admin = newCasAdmin("admin")
            val coll = pubCollection("coll1", admin)
            val content = newContent("content1")(admin, coll)
            val request = sessionReq(admin)
              .withJsonBody(Json.obj(
                "captionTracks" -> Seq("hashabcdef123456", "abdef123123a"),
                "annotationDocuments" -> Seq("hashabcdef123456", "abdef123123a"),
                "showCaptions" -> true,
                "showAnnotations" -> true,
                "allowDefinitions" -> true,
                "showTranscipts" -> true,
                "aspectRatio" -> "19",
                "showWordList" -> true
                ))
            val result = call(controller.setSettings(content.id.get), request)
            contentType(result) mustEqual Some("application/json")
            status(result) mustEqual 200
          }
          "return ok on empty request" in {
            val controller = new ContentEditingTestController()
            val admin = newCasAdmin("admin")
            val coll = pubCollection("coll1", admin)
            val content = newContent("content1")(admin, coll)
            val request = sessionReq(admin)
              .withJsonBody(Json.obj())
            val result = call(controller.setSettings(content.id.get), request)
            contentType(result) mustEqual Some("application/json")
            status(result) mustEqual 200
          }
          "not break on invalid settings" in {
            val controller = new ContentEditingTestController()
            val admin = newCasAdmin("admin")
            val coll = pubCollection("coll1", admin)
            val content = newContent("content1")(admin, coll)
            val request = sessionReq(admin)
              .withJsonBody(Json.obj(
                "invalidsetting" -> "this is not a real setting",
                "captionTracks" -> Seq("resourceId")
                ))
            val result = call(controller.setSettings(content.id.get), request)
            contentType(result) mustEqual Some("application/json")
            status(result) mustEqual 200
          }
      }

      "The Edit Image Endpoint" should {
          "serve the edit image page to a user" in {
              1 mustEqual 1
          }
          //forbidden error if content not editable by user
      }

      "The Save Image Edits Endpoint" should {
          "save the edits to a content by id" in {
              1 mustEqual 1
          }
          //forbidden error if content not editable by user
          //lots of cases to cover here
      }

      "The Change Thumbnail Endpoint" should {
          "change the thumbnail for the content by id with the image in the request" in {
              1 mustEqual 1
          }
          //lots of cases to cover here
      }

      "The Create Thumbnail Endpoint" should {
          "create a thumbnail for the content by id with the image in the request" in {
              1 mustEqual 1
          }
          //lots of cases to cover here
      }

      "The Set Media Source Endpoint" should {
          "set the media source for a content by id" in {
              1 mustEqual 1
          }
          //forbidden error if content not editable by user
      }

      "The Batch Update Content Endpoint" should {
          "update mutiple content objects" in {
              1 mustEqual 1
          }
          //lots of cases to cover here
      }
  }
}
