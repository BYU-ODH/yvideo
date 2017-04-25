import models.{Content, User, Course}
import org.specs2.mutable._
import org.specs2.execute._

import play.api.libs.json.Json
import play.api.test._
import play.api.test.Helpers._
import service.DocumentPermissionChecker

/**
 * Tests all the methods in the course model
 */
class VisibleDocumentPermissionCheckerSpec extends Specification {

  abstract class TestApplication extends WithApplication {
	override def around[T: AsResult](t: => T): Result = super.around {
	  setupData()
	  t
	}

	def setupData() {
	  // setup data
	}
  }

  val personalResource = Json.obj(
    "id" -> "josh1",
    "attributes" -> Json.obj(
      "ayamel_ownerType" -> "user",
      "ayamel_ownerId" -> "5"
    )
  )

  val courseResource = Json.obj(
    "id" -> "josh2",
    "attributes" -> Json.obj(
      "ayamel_ownerType" -> "course",
      "ayamel_ownerId" -> "8"
    )
  )

  val globalResource = Json.obj(
    "id" -> "josh3"
  )

  val user = User(Some(5), "", 'a, "")

  val course = Course(Some(8), "", "", "")

  "The visible document permission checker" should {

    "not allow disabled personal documents" in new TestApplication {
      val content1 = Content(Some(1), "", 'a, "", "")
      val content2 = Content(Some(2), "", 'a, "", "")

      val checker1 = new DocumentPermissionChecker(user, content1, None, "captionTrack")
      val checker2 = new DocumentPermissionChecker(user, content2, None, "captionTrack")

      checker1.canView(personalResource) shouldEqual false
      checker2.canView(personalResource) shouldEqual false
    }

    "allow enabled personal documents" in new TestApplication {
      val content = Content(Some(1), "", 'a, "", "")
      val checker = new DocumentPermissionChecker(user, content, None, "captionTrack")

      checker.canView(personalResource) shouldEqual true
    }

    "not allow disabled course documents" in new TestApplication {
      val content1 = Content(Some(1), "", 'a, "", "")
      val content2 = Content(Some(2), "", 'a, "", "")

      val checker1 = new DocumentPermissionChecker(user, content1, Some(course), "captionTrack")
      val checker2 = new DocumentPermissionChecker(user, content2, Some(course), "captionTrack")

      checker1.canView(courseResource) shouldEqual false
      checker2.canView(courseResource) shouldEqual false
    }

    "allow enabled course documents" in new TestApplication {
      val content = Content(Some(1), "", 'a, "", "")
      val checker = new DocumentPermissionChecker(user, content, Some(course), "captionTrack")

      checker.canView(courseResource) shouldEqual true
    }

    "not allow disabled global documents in course" in new TestApplication {
      val content1 = Content(Some(1), "", 'a, "", "")
      val content2 = Content(Some(2), "", 'a, "", "")
      val content3 = Content(Some(3), "", 'a, "", "")

      val checker1 = new DocumentPermissionChecker(user, content1, Some(course), "captionTrack")
      val checker2 = new DocumentPermissionChecker(user, content2, Some(course), "captionTrack")
      val checker3 = new DocumentPermissionChecker(user, content3, Some(course), "captionTrack")

      checker1.canView(globalResource) shouldEqual false
      checker2.canView(globalResource) shouldEqual false
      checker3.canView(globalResource) shouldEqual false
    }

    "allow enabled global documents in course" in new TestApplication {
      val content = Content(Some(1), "", 'a, "", "")
      val checker = new DocumentPermissionChecker(user, content, Some(course), "captionTrack")

      checker.canView(globalResource) shouldEqual true
    }

    "not allow disabled global documents" in new TestApplication {
      val content1 = Content(Some(1), "", 'a, "", "")
      val content2 = Content(Some(2), "", 'a, "", "")

      val checker1 = new DocumentPermissionChecker(user, content1, None, "captionTrack")
      val checker2 = new DocumentPermissionChecker(user, content2, None, "captionTrack")

      checker1.canView(globalResource) shouldEqual false
      checker2.canView(globalResource) shouldEqual false
    }

    "allow enabled global documents" in new TestApplication {
      val content = Content(Some(1), "", 'a, "", "")
      val checker = new DocumentPermissionChecker(user, content, None, "captionTrack")

      checker.canView(globalResource) shouldEqual true
    }
  }
}

