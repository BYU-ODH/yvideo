package test

import play.api.libs.json._
import play.api.data.validation.ValidationError
import play.api.test.FakeRequest

import org.specs2.matcher.{Matcher, MatchResult, Expectable}
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
   * Checks if an iterable contains all successes
   * Example:
   * {{{
   * list.map { obj =>
   *   obj === something
   * } must allSucceed()
   * }}}
   */
  case class allSucceed() extends Matcher[Iterable[MatchResult[Any]]] {
    def apply[S <: Iterable[MatchResult[Any]]](s: Expectable[S]) = {
      result(s.value.forall(x => x.isSuccess),
        "Tests Succeeded", "Some test failed", s)
    }
  }

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
  def newCasAdmin(name: String): User = casUserWithRole(name, perm='admin)
  def newCasManager(name: String): User = casUserWithRole(name, perm='manager)
  def newCasTeacher(name: String): User = casUserWithRole(name, perm='teacher)
  def newCasStudent(name: String): User = casUserWithRole(name, perm='student)

  /**
   * For testing search functions in models
   */
  def customManager(name: String = "FullName", email: String = "userperson@yvideo.net", netid: String = "userperson123"): User =
    casUserWithRole(name, email, netid, 'manager)
  def customAdmin(name: String = "FullName", email: String = "userperson@yvideo.net", netid: String = "userperson123"): User =
    casUserWithRole(name, email, netid, 'admin)
  def customTeacher(name: String = "FullName", email: String = "userperson@yvideo.net", netid: String = "userperson123"): User =
    casUserWithRole(name, email, netid, 'teacher)
  def customStudent(name: String = "FullName", email: String = "userperson@yvideo.net", netid: String = "userperson123"): User =
    casUserWithRole(name, email, netid, 'student)

  def casUserWithRole(name: String, email: String = "user@yvideo.net", netid: String = "mynetid", perm: Symbol = 'student): User = {
    val user = User(
        id=None,
        email=Some(email),
        username=netid,
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
  def newCollection(name: String, user: User) = createCollection(user, name, false, false)
  def pubCollection(name: String, user: User) = createCollection(user, name, true, false)
  def arcCollection(name: String, user: User) = createCollection(user, name, false, true)
  def pubArcCollection(name: String, user: User) = createCollection(user, name, true, true)

  def createCollection(owner: User, name: String, pub: Boolean, arc: Boolean): Collection = {
    val c = Collection(None, owner.id.get, name, pub, arc).save
    owner.enroll(c, true)
    c
  }

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

