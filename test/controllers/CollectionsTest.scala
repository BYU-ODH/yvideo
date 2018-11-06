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
import controllers.Collections

object CollectionsControllerSpec extends Specification {

  class CollectionsTestController() extends Controller with Collections

	"Collections Controller Tests" >> {

		"The Get Collection Endpoint" should {
			"return a collection map from the id" in {
				1 mustEqual 1
				//return a not found error if collection doesn't exist
			}
		}

		"The View Endpoint" should {
			"serve the collection based on the id to the user if they are allowed to view it" in {
					implicit ee: ExecutionEnv =>
							running(FakeApplication()) {
									val userOpt = User.findByUsername('password, "admin")
									userOpt mustNotEqual None
									implicit val user = userOpt.get
									user.id mustNotEqual None
									val controller = new CollectionsTestController()
									val request = FakeRequest().withSession("userId" -> user.id.get.toString)
									val result = controller.view(2)(request) // volatile - change after fixtures work
									status(result) shouldEqual 200
							}
							//return a forbidden error if the user does not have permission
							//make sure all the TAs and linked courses are included
							//check for an admin and a teacher
			}
		}

		"The Edit Endpoint" should {
			"edit the collection based on the id and map if the user is allowed to do so" in {
				1 mustEqual 1
				//redirects to the collection when successful
				//return a forbidden error if the user does not have permission
			}
		}

		"The Add Content Endpoint" should {
			"add content to the collection based on the id and map if the user is allowed to do so" in {
				1 mustEqual 1
				//redirects to the collection when successful
				//return a forbidden error if the user does not have permission
			}
		}

		"The Remove Content Endpoint" should {
			"remove content from the collection based on the id and map if the user is allowed to do so" in {
				1 mustEqual 1
				//redirects to the collection when successful
				//return a forbidden error if the user does not have permission
			}
		}

		"The Create Endpoint" should {
			"create a new collection for the user if they are allowed to do so" in {
				1 mustEqual 1
				//redirects to the new collection when successful
				//return a forbidden error if the user does not have permission
			}
		}

		"The Create Page Endpoint" should {
			"serves the create a collection page to the user if they are allowed to do so" in {
					implicit ee: ExecutionEnv =>
							running(FakeApplication()) {
									val userOpt = User.findByUsername('password, "admin")
									userOpt mustNotEqual None
									implicit val user = userOpt.get
									user.id mustNotEqual None
									val controller = new CollectionsTestController()
									val request = FakeRequest().withSession("userId" -> user.id.get.toString)
									val result = controller.createPage(request)
									status(result) shouldEqual 200
							}
							//return a forbidden error if the user does not have permission
			}
		}

		"The List Endpoint" should {
			"serve the page that lists all the collections to anyone" in {
					implicit ee: ExecutionEnv =>
							running(FakeApplication()) {
									val userOpt = User.findByUsername('password, "admin")
									userOpt mustNotEqual None
									implicit val user = userOpt.get
									user.id mustNotEqual None
									val controller = new CollectionsTestController()
									val request = FakeRequest().withSession("userId" -> user.id.get.toString)
									val result = controller.list(request)
									status(result) shouldEqual 200
							}
			}
		}

		"The Quit Collection Endpoint" should {
			"remove the user from the collection" in {
				1 mustEqual 1
				//redirect to the home page afterwards
			}
		}

		"The Link Courses Endpoint" should {
			"link the collection to the courses in the map" in {
				1 mustEqual 1
				//catch errors when the collection doesn't exist or the course doesn't match
			}
		}

		"The Unlink Courses Endpoint" should {
			"unlink the collection from the courses in the map" in {
				1 mustEqual 1
				//catch errors when the collection doesn't exist or the course isn't provided/doesn't match
			}
		}

		"The Add TA Endpoint" should {
			"add a TA to the collection by netid" in {
				1 mustEqual 1
				//catch errors when the collection doesn't exist, the user doesn't exist, or an error happens
			}
		}

		"The Remove TA Endpoint" should {
			"remove a TA from the collection by netid" in {
				1 mustEqual 1
				//catch errors when the collection doesn't exist, the user doesn't exist, or an error happens
			}
		}

		"The Add Exception Endpoint" should {
			"add an exception to the collection by netid" in {
				1 mustEqual 1
				//catch errors when the collection doesn't exist, the user doesn't exist/has already been added, or an error happens
			}
		}

		"The Remove Exception Endpoint" should {
			"remove an exception from the collection by netid" in {
				1 mustEqual 1
				//catch errors when the collection doesn't exist, the user doesn't exist, or an error happens
			}
		}

		"The Set Permission Endpoint" should {
			"remove, add, or match collection specific permissions for the user" in {
				1 mustEqual 1
				//tests for each case and forbidden for non-teachers
			}
		}
	}
}