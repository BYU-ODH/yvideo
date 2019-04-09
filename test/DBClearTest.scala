import org.specs2.mutable._

import models.User
import test.{ApplicationContext, TestHelpers, DBClear}

/**
 * This spec is to make sure that the DBClear trait is functioning correctly.
 */
object DBClearSpec extends Specification with ApplicationContext with DBClear with TestHelpers {
  "Database Context Test" >> {
    "The application context database" should {
      "Allow record creation" in {
        application {
          val student = newCasStudent("test student")
          student.id mustNotEqual None
          student.id.get === 1
        }
      }

      "truncate the database between test cases" in {
        application {
          val users = User.list
          users.length === 0
        }
      }

      "reseed the database after truncating" in {
        application {
          val student = newCasStudent("test student")
          student.id mustNotEqual None
          student.id.get === 1
        }
      }
    }
  }
}

