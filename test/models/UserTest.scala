import play.api.test._
import play.api.mvc._
import play.api.libs.json._
import play.api.test._
import play.api.test.Helpers._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.JsonMatchers
import org.specs2.mutable._

import controllers.Users
import models._
import test.{ApplicationContext, TestHelpers, DBClear}
import service.TimeTools

object UserModelSpec extends Specification with ApplicationContext with DBClear
  with TestHelpers with JsonMatchers {

  "The save function" should {
    "create a user record in the database and return a copy with an updated id" in {
      application {
        val name = "fake name"
        val user = User(
            id=None,
            email=Some("fake@email.com"),
            username=name,
            name=Some(name)
          ).save
        user.id must beSome
      }
    }
  }

  "The delete function" should {
    // TODO: Implement this test
    // "remove a user from the user cache" in {
    //   application {
    //     ko
    //   }
    // }

    "unenroll a user from their collections" in {
      // the results should be the same for users of all permissions
      application {
        val user = newCasTeacher("Cas Teacher")
        user.id must beSome

        val collection = newCollection("temp collection", user)
        collection.id must beSome

        val membership = CollectionMembership.listByUser(user)
        membership.length === 1
        membership(0).collectionId === collection.id.get

        user.delete()

        val membershipPostDelete = CollectionMembership.listByUser(user)
        membershipPostDelete.length === 0
      }
    }

    "remove a user's collection permissions" in {
      // the results should be the same for users of all permissions
      application {
        val user = newCasTeacher("Cas Teacher")
        user.id must beSome

        val collection = newCollection("temp collection", user)
        collection.id must beSome

        // collection permission are only added for teachers and TAs which are only added
        // manually when another teacher or a TA are added to a collection that already has a teacher.
        // so we manually add an unnecessary permission here solely for the test
        CollectionPermissions.addUserPermission(collection, user, "TA")
        val perms = CollectionPermissions.listByUser(collection, user)
        perms.length === 1

        user.delete()

        val permsPostDelete = CollectionPermissions.listByUser(collection, user)
        permsPostDelete.length === 0
      }
    }

    "remove a user's viewing history" in {
      application {
        val user = newCasTeacher("Cas Teacher")
        user.id must beSome

        val collection = newCollection("temp collection", user)
        collection.id must beSome

        List(1, 2, 3, 4, 5).map(x => ViewingHistory(None, user.id.get, x, TimeTools.now()).save)
          .map(_.id must beSome) must allSucceed()
        val views = ViewingHistory.getUserViews(user.id.get)
        views.length === 5

        user.delete()

        val viewsPostDelete = ViewingHistory.getUserViews(user.id.get)
        viewsPostDelete.length === 0
      }
    }

    "remove a user's site permissions" in {
      application {
        val user = newCasTeacher("Cas Teacher")
        user.id must beSome

        SitePermissions.addUserPermission(user, "admin")
        val perms = SitePermissions.listByUser(user)
        perms.last === "admin"

        user.delete()

        val permsPostDelete = SitePermissions.listByUser(user)
        permsPostDelete.length === 0
      }
    }

    "remove the user" in {
      application {
        val user = newCasTeacher("Cas Teacher")
        user.id must beSome

        user.delete()

        User.findById(user.id.get) must beNone
      }
    }
  }
}
