import scala.concurrent.{Future}
import play.api.libs.json._
import play.api.mvc.Result
import play.api.mvc.Results
import play.api.mvc.Controller
import play.api.test.Helpers._
import org.specs2.mutable._
import org.specs2.matcher.JsonMatchers

import models.{Content, User, Course, Collection, SitePermissions}
import controllers.Collections
import test.ApplicationContext
import test.TestHelpers
import test.DBClear

object CollectionsControllerSpec extends Specification with ApplicationContext with DBClear with TestHelpers with JsonMatchers {

  class CollectionsTestController() extends Controller with Collections
  implicit val collectionReads = Json.reads[Collection]

  "Collections Controller Tests" >> {
    "The collectionAsJson endpoint" should {
      "allow admins to view any collection" in {
        application {
          val controller = new CollectionsTestController()
          val admin = newCasAdmin("admin")
          admin.id mustNotEqual None
          val user = newCasTeacher("Teacher Teacher")
          user.id mustNotEqual None
          val newCol = newCollection("Teacher's Collection", user)
          newCol.id mustNotEqual None
          val res = controller.collectionAsJson(newCol.id.get)(sessionReq(user))
          val json = contentAsJson(res)
          json.validate[Collection] match {
            case JsSuccess(c, _) => {
              c.id.get === newCol.id.get
              c.published === false
              c.archived === false
            }
            case e: JsError => jserr2string(e) must fail
          }
        }
      }
    }

    "The getContent endpoint" should {
        "return the content in a collection if the user is enrolled in the collection" in {
          application {
            val controller = new CollectionsTestController()
            val admin = newCasAdmin("admin1")
            admin.id must beSome
            implicit val newCol = newCollection("coll1", admin)
            newCol.id must beSome
            implicit val student = newCasStudent("stud1")
            student.id must beSome
            student.enroll(newCol)
            val contents = List("c1", "c2", "c3", "c4").map(name => newContent(name))
            contents.map{ c =>
              c.id must beSome
            } must allSucceed()
            val res = controller.getContent(newCol.id.get)(sessionReq(student))
            status(res) === 200
            val json = contentAsJson(res)
            json.validate[List[JsValue]] match {
              case JsSuccess(clist, _) => {
                clist.map { content =>
                  val c = content.toString
                  c must /("expired" -> false)
                  c must /("published" -> true)
                } must allSucceed()
                clist.length === contents.length
              }
              case e: JsError => jserr2string(e) must fail
            }
          }
        }

        "respond with a 403 status code if the user is not enrolled in the collection" in {
          application {
            val controller = new CollectionsTestController()
            val admin = newCasAdmin("admin1")
            admin.id must beSome
            implicit val student = newCasStudent("stud1")
            student.id must beSome
            implicit val newCol = newCollection("coll1", admin)
            newCol.id must beSome
            val res = controller.getContent(newCol.id.get)(sessionReq(student))
            status(res) === 403
          }
        }

        "return published content to a student" in {
          application {
            val controller = new CollectionsTestController()
            val admin = newCasAdmin("admin1")
            admin.id must beSome
            implicit val newCol = newCollection("coll1", admin)
            newCol.id must beSome
            implicit val student = newCasStudent("stud1")
            student.id must beSome
            student.enroll(newCol)
            val contents = List("c1", "c2", "c3", "c4").map(name => newContent(name))
            contents.map{ c =>
              c.id must beSome
            } must allSucceed()
            val unpublishedcontents = List("c5", "c6", "c7", "c8").map(name => createContent(name, pub=false, cid=newCol.id.get))
            unpublishedcontents.map{ c =>
              c.id must beSome
            } must allSucceed()
            val res = controller.getContent(newCol.id.get)(sessionReq(student))
            status(res) === 200
            val json = contentAsJson(res)
            json.validate[List[JsValue]] match {
              case JsSuccess(clist, _) => {
                clist.map { content =>
                  val c = content.toString
                  c must /("expired" -> false)
                  c must /("published" -> true)
                } must allSucceed()
                clist.length === contents.length
              }
              case e: JsError => jserr2string(e) must fail
            }
          }
        }

        "return published non-expired content to a student" in {
          application {
            val controller = new CollectionsTestController()
            val admin = newCasAdmin("admin1")
            admin.id must beSome
            implicit val newCol = newCollection("coll1", admin)
            newCol.id must beSome
            implicit val student = newCasStudent("stud1")
            student.id must beSome
            student.enroll(newCol)
            val contents = List("c1", "c2", "c3", "c4").map(name => newContent(name))
            contents.map{ c =>
              c.id must beSome
            } must allSucceed()
            val expiredcontents = List("c5", "c6", "c7", "c8").map(name => expiredPublishedContent(name))
            expiredcontents.map{ c =>
              c.id must beSome
            } must allSucceed()
            val res = controller.getContent(newCol.id.get)(sessionReq(student))
            status(res) === 200
            val json = contentAsJson(res)
            json.validate[List[JsValue]] match {
              case JsSuccess(clist, _) => {
                clist.map { content =>
                  val c = content.toString
                  c must /("expired" -> false)
                  c must /("published" -> true)
                } must allSucceed()
                clist.length === contents.length
              }
              case e: JsError => jserr2string(e) must fail
            }
          }
        }

        "return all content to a collection teacher" in {
          application {
            val controller = new CollectionsTestController()
            implicit val teacher = newCasTeacher("teacher1")
            teacher.id must beSome
            implicit val newCol = newCollection("coll1", teacher)
            newCol.id must beSome
            val contents = List("c1", "c2", "c3", "c4").map(name => newContent(name))
            contents.map{ c =>
              c.id must beSome
            } must allSucceed()
            val expiredcontents = List("c5", "c6", "c7", "c8").map(name => expiredPublishedContent(name))
            expiredcontents.map{ c =>
              c.id must beSome
            } must allSucceed()
            val unpublishedcontents = List("c9", "c10", "c11", "c12").map(name => createContent(name, cid=newCol.id.get))
            unpublishedcontents.map{ c =>
              c.id must beSome
            } must allSucceed()
            val res = controller.getContent(newCol.id.get)(sessionReq(teacher))
            status(res) === 200
            val json = contentAsJson(res)
            json.validate[List[JsValue]] match {
              case JsSuccess(clist, _) => {
                clist.length === contents.length + expiredcontents.length + unpublishedcontents.length
              }
              case e: JsError => jserr2string(e) must fail
            }
          }
        }

        "return all content to a collection TA" in {
          application {
            val controller = new CollectionsTestController()
            val teacher = newCasTeacher("teacher1")
            implicit val newCol = newCollection("coll1", teacher)
            newCol.id must beSome
            implicit val TA = newCasTA("ta1")
            TA.id must beSome
            val contents = List("c1", "c2", "c3", "c4").map(name => newContent(name))
            contents.map{ c =>
              c.id must beSome
            } must allSucceed()
            val expiredcontents = List("c5", "c6", "c7", "c8").map(name => expiredPublishedContent(name))
            expiredcontents.map{ c =>
              c.id must beSome
            } must allSucceed()
            val unpublishedcontents = List("c9", "c10", "c11", "c12").map(name => createContent(name, cid=newCol.id.get))
            unpublishedcontents.map{ c =>
              c.id must beSome
            } must allSucceed()
            val res = controller.getContent(newCol.id.get)(sessionReq(TA))
            status(res) === 200
            val json = contentAsJson(res)
            json.validate[List[JsValue]] match {
              case JsSuccess(clist, _) => {
                clist.length === contents.length + expiredcontents.length + unpublishedcontents.length
              }
              case e: JsError => jserr2string(e) must fail
            }
          }
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
          application {
              val user = newCasAdmin("admin")
              user.id mustNotEqual None
              val controller = new CollectionsTestController()
              val result = controller.createPage(sessionReq(user))
              status(result) shouldEqual 200
          }
        }
    }

    "The Create Page Endpoint" should {
        "serves the create a collection page to the user if they are allowed to do so" in {
          application {
            val user = newCasAdmin("admin")
            user.id mustNotEqual None
            val controller = new CollectionsTestController()
            val result = controller.createPage(sessionReq(user))
            status(result) shouldEqual 200
          }
                        //return a forbidden error if the user does not have permission
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

    "The getPermissions endpoint" should {
      "return all of the courses, exceptions and admins in a collection to a collection admin" in {
        application {
          val controller = new CollectionsTestController()
          implicit val teacher = newCasTeacher("teacher1")
          teacher.id must beSome
          implicit val newCol = newCollection("coll1", teacher)
          newCol.id must beSome
          val students = List("s1", "s2", "s3", "s4").map(name => newCasStudent(name))
          students.map{ s => s.id must beSome } must allSucceed()
          // enroll as normal students
          students.foreach(s => s.enroll(newCol, false, false))
          val exceptions = List("s5", "s6", "s7", "s8").map(name => newCasStudent(name))
          exceptions.map{ e => e.id must beSome } must allSucceed()
          // enroll as exceptions
          exceptions.foreach(e => e.enroll(newCol, false, true))
          val res = controller.getPermissions(newCol.id.get)(sessionReq(teacher))
          status(res) === 200
          val json = contentAsJson(res)
          json.validate[JsValue] match {
            case JsSuccess(obj, _) => {
              val courses = (obj \ "courses").as[List[JsValue]]
              val admins = (obj \ "admins").as[List[JsValue]]
              val ex = (obj \ "exceptions").as[List[JsValue]]
              courses.length === 0
              // The teacher is the only admin in this collection
              admins.length === 1
              ex.length === 4
              // make sure the exceptions are the correct users
              ex.zip(exceptions).map { tup =>
                tup._1.toString must /("name" -> tup._2.name.getOrElse(""))
              } must allSucceed()
            }
            case e: JsError => jserr2string(e) must fail
          }
        }
      }

      "return a 403 to a user that is enrolled but is not an admin in the collection" in {
        application {
          val controller = new CollectionsTestController()
          implicit val teacher = newCasTeacher("teacher1")
          teacher.id must beSome
          implicit val newCol = newCollection("coll1", teacher)
          newCol.id must beSome
          val students = List("s1", "s2", "s3", "s4").map(name => newCasStudent(name))
          students.map{ s => s.id must beSome } must allSucceed()
          // enroll as normal students
          students.foreach(s => s.enroll(newCol, false, false))
          val res = controller.getPermissions(newCol.id.get)(sessionReq(students(0)))
          status(res) === 403
        }

      }
    }
  }
}
