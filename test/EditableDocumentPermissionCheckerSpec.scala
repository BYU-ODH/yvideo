import models.{Content, User, Course}
import org.specs2.mutable._

import play.api.libs.json.Json
import service.DocumentPermissionChecker

/**
 * Tests all the methods in the course model
 */
class EditableDocumentPermissionCheckerSpec extends Specification {

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

  val user1 = User(Some(5), "", 'a, "")

  val user2 = User(Some(5), "", 'a, "", role = User.roles.admin)

  val course = Course(Some(8), "", "", "")
  course.cache.teachers = Some(Nil)

  "The editable document permission checker" should {

    true shouldEqual true

  }
}
