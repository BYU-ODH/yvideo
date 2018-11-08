import play.api.test._
import play.api.mvc._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.libs.json._

import models.{Content, User, Course, Collection}
import controllers.ContentEditing

object ContentEditingControllerSpec extends Specification {

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
  			1 mustEqual 1
  		}
  		//forbidden error if content not editable by user
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
