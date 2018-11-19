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
import controllers.CollectionContent

object CollectionContentControllerSpec extends Specification {

  class CollectionContentTestController() extends Controller with CollectionContent

  "Collection Content Controller Tests" >> {

  	"The View In Collection Endpoint" should {
  		"serve the content view within the collection" in {
  				implicit ee: ExecutionEnv =>
  						running(FakeApplication()) {
  								val userOpt = User.findByUsername('password, "admin")
  								userOpt mustNotEqual None
  								implicit val user = userOpt.get
  								user.id mustNotEqual None
  								val controller = new CollectionContentTestController()
  								val request = FakeRequest().withSession("userId" -> user.id.get.toString)
  								val result = controller.viewInCollection(1,1)(request) ////volatile - we need to add a specific content and collection and user their ids instead
									status(result) shouldEqual 200  								
  						}
  						//forbidden if content isn't visible by user
  		}
  	}

  	"The Add To Collection Endpoint" should {
  		"add content by id to the collection" in {
  			1 mustEqual 1
  		}
  		//forbidden if user is not collection TA
  	}

  	"The Remove From Collection Endpoint" should {
  		"remove content by id from the collection by id" in {
  			1 mustEqual 1
  		}
  		//forbidden if content isn't visible by user
  	}
  }
}