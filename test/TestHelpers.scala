package test

import play.api.libs.json._
import play.api.data.validation.ValidationError
import org.specs2.matcher.Matcher
import org.specs2.matcher.MatchersImplicits._

import models.{SitePermissions, User}

trait TestHelpers {
  /**
   * Custom matcher for descriptive failures
   * Call like so:
   *  "Error message" must fail
   */
  def fail: Matcher[String] = { s: String => (false, s) }

  /**
   * Convert JsErrors into readable strings
   */
  def jserr2string(err: JsError): String =
    err.errors.map(t=>s"JsError: ${t._1.toJsonString}: ${t._2.map(_.message).mkString(",")}").mkString("\n")

  /**
   * Creates new CAS user with admin, manager, teacher or student privileges
   * @name The user's full name
   * @return User object with Some(id)
   */
  def newCasAdmin(name: String): User = casUserWithRole(name, 'admin)
  def newCasManager(name: String): User = casUserWithRole(name, 'manager)
  def newCasTeacher(name: String): User = casUserWithRole(name, 'teacher)
  def newCasStudent(name: String): User = casUserWithRole(name, 'student)

  def casUserWithRole(name: String, perm: Symbol): User = {
    val user = User(
        id=None,
        authId=formatName(name),
        authScheme='cas,
        username=formatName(name),
        name=Some(name)
      ).save
    SitePermissions.assignRole(user, perm)
    user
  }

  /**
   * Returns the name in lower case without spaces
   */
  def formatName(name: String): String = name.toLowerCase().filter(c => !Array(" ", "\n", "\t").contains(c))
}

