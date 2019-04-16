package test

import play.api.libs.json._
import play.api.data.validation.ValidationError
import play.api.test.FakeRequest

import org.specs2.matcher.Matcher
import org.specs2.matcher.JsonMatchers
import org.specs2.matcher.MatchersImplicits._

import service.HashTools
import models.{SitePermissions, User, Collection, Content}

trait TestHelpers {
  /**
   * Custom matcher for descriptive failures
   * Call like so:
   *  {{{"Error message" must fail}}}
   */
  def fail: Matcher[String] = { s: String => (false, s) }

  /**
   * Convert JsErrors into readable strings
   * Can be used in conjunction with the fail function in this file.
   * {{{ jserr2string(error) must fail }}}
   * JsError objects may contain multiple errors. This function combines them all into one string.
   * @param err The play JsError
   * @return A String representation of the given error
   */
  def jserr2string(err: JsError): String =
    err.errors.map(t=>s"JsError: ${t._1.toJsonString}: ${t._2.map(_.message).mkString(",")}").mkString("\n")

  /**
   * Get A FakeRequest with a user session instantiated.
   * @param user The User object. The user must have Some(id) otherwise this will result in a runtime error.
   * @return Request with a session that is parseable by the Authentication functions.
   */
  def sessionReq(user: User) = FakeRequest().withSession("userId" -> user.id.get.toString)

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
   * Create a new collection which is:
   * pub: published and not archived
   * arc: archived and not published
   * pubArc: published and archived
   * unpub: unpublished and not archived
   * @param name The String name of the collection
   * @param user User to be the owner of the collection
   */
  def newCollection(name: String, user: User) = createCollection(user.id.get, name, false, false)
  def pubCollection(name: String, user: User) = createCollection(user.id.get, name, true, false)
  def arcCollection(name: String, user: User) = createCollection(user.id.get, name, false, true)
  def pubArcCollection(name: String, user: User) = createCollection(user.id.get, name, true, true)

  def createCollection(owner: Long, name: String, pub: Boolean, arc: Boolean): Collection =
    Collection(None, owner, name, pub, arc).save

  /**
   *  Create Content
   */
  def newContent(name: String)(implicit user: User, col: Collection) =
    createContent(name, 'video, col.id.get, HashTools.md5Hex(scala.util.Random.nextString(16)), false, false, true, None,
      user.name+user.email.getOrElse(""), false, false)

  def createContent(name: String, t: Symbol, cid: Long, rid: String, phys: Boolean, copy: Boolean, expired: Boolean,
                    date: Option[String], req: String, pub: Boolean, full: Boolean): Content =
    Content(
      id=None,
      name=name,
      contentType=t,
      collectionId=cid,
      thumbnail="asdf",
      resourceId=rid,
      physicalCopyExists=phys,
      isCopyrighted=copy,
      expired=expired,
      dateValidated=date,
      requester=req,
      published=pub,
      fullVideo=full
      ).save

  /**
   * Returns the name in lower case without spaces
   */
  def formatName(name: String): String = name.toLowerCase().filter(c => !Array(" ", "\n", "\t").contains(c))
}

