package test

import play.api.libs.json._
import play.api.data.validation.ValidationError
import org.specs2.matcher.Matcher
import org.specs2.matcher.MatchersImplicits._

import models.User

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
   * Used to generate new users
   * username = the name with spaces removed
   * email = name @ fakemail.biz
   */
  def newCasUser(name: String): User = User(
      id=None,
      authId="",
      authScheme='cas,
      username=name.filter(c => !Array(" ", "\n", "\t").contains(c)),
      name=Some(name),
      email=Some(s"$name@fakemail.biz")
    ).save
}

