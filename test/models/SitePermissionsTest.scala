import org.specs2.mutable._

import models.{User, SitePermissions}
import test.ApplicationContext
import test.TestHelpers
import test.DBClear

object SitePermissionsSpec extends Specification with ApplicationContext with DBClear with TestHelpers {
  "Site Permissions Model Test" >> {
    "The listByPerm function" should {
      // The order of the tests for this function is important if we are using BeforeAll instead of
      // a per test application setup
      // They list the users by permission. we select permissions here that are the highest level
      // for the role we want to target
      "return all students" in {
        application {
          val newStudents = List("student1", "student2", "student3").map(x => newCasStudent(x))
          val retrievedStudents = SitePermissions.listByPerm("joinCollection")
          newStudents === retrievedStudents
        }
      }

      "return all teachers" in {
        application {
          val newTeachers = List("teacher1", "teacher2", "teacher3").map(x => newCasTeacher(x))
          val retrievedTeachers = SitePermissions.listByPerm("createCollection")
          newTeachers === retrievedTeachers
        }
      }

      "return all managers" in {
        application {
          val newManagers = List("manager1", "manager2", "manager3").map(x => newCasManager(x))
          val retrievedManagers = SitePermissions.listByPerm("enableContent")
          newManagers === retrievedManagers
        }
      }

      "return all admins" in {
        application {
          val newAdmins = List("admin1", "admin2", "admin3").map(x => newCasAdmin(x))
          val retrievedAdmins = SitePermissions.listByPerm("admin")
          newAdmins === retrievedAdmins
        }
      }

      "fail gracefully when an invalid permission is given" in {
        application {
          val ret = SitePermissions.listByPerm("not a real permission")
          ret === Nil
        }
      }
    }

    "The assign role function" should {
      "assign the manager role successfully" in {
        application {
          val user = newCasStudent("person1")
          SitePermissions.listByUser(user).sorted === SitePermissions.roles('student).sorted
          SitePermissions.removeAllUserPermissions(user)
          SitePermissions.assignRole(user, 'manager)
          SitePermissions.listByUser(user).sorted === SitePermissions.roles('manager).sorted
        }
      }

      "convert a manager to an admin" in {
        application {
          val user = newCasManager("manager4")
          SitePermissions.listByUser(user).sorted === SitePermissions.roles('manager).sorted
          SitePermissions.removeAllUserPermissions(user)
          SitePermissions.assignRole(user, 'admin)
          SitePermissions.listByUser(user).sorted === SitePermissions.roles('admin).sorted
        }
      }

      "convert an admin to a manager" in {
        application {
          val user = newCasAdmin("admin4")
          SitePermissions.listByUser(user).sorted === SitePermissions.roles('admin).sorted
          SitePermissions.removeAllUserPermissions(user)
          SitePermissions.assignRole(user, 'manager)
          SitePermissions.listByUser(user).sorted === SitePermissions.roles('manager).sorted
        }
      }
    }
  }
}

