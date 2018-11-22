// import play.api.test._
// import play.api.mvc._
// import org.specs2.concurrent.ExecutionEnv
// import org.specs2.mutable._
// import play.api.test._
// import play.api.test.Helpers._
// import scala.concurrent.ExecutionContext.Implicits.global
// import scala.concurrent.Future
// import play.api.Logger
// import play.api.Play.current
// import models.User
// import anorm._
// import play.api.db.DB
// import org.specs2.specification.AfterEach

// object UserModelTest extends Specification with AfterEach {

//   def truncateTables = {
//     running(FakeApplication()) {
//       DB.withConnection {
//         implicit connection =>
//         List("accountLink", "addCourseRequest", "collection", "collectionCourseLink", "collectionMembership", "collectionPermissions",
//           "content", "contentListing", "contentSetting", "course", "contentOwnership", "feedback", "helpPage", "homePageContent", 
//           "login_tokens", "notification", "scoring", "setting", "sitePermissionRequest", "sitePermissions", "userAccount", "wordList")
//         foreach { table: String =>
//           SQL("truncate table {table}").on('table -> table).execute()
//         }
//       }
//     }
//   }

//   def after = {
//     truncateTables
//   }

//   "User Model Tests" >> {

//     "Correct Creation (in memory) Test" >> {
//       // User(id: Option[Long], authId: String, authScheme: Symbol, username: String, name: Option[String])
//       val testUser = User(None, "", 'password, "hpotter1234", Some("POTTER, Harry"))

//       "Correct name" >> {
//         testUser.name.get must_== "POTTER, Harry"
//       }

//       "Correct username" >> {
//         testUser.username must_== "hpotter1234"
//       }
//     }

//     "User.save" >> {
//       // Start fake application
//       running(FakeApplication()) {
//         val testUser1 = User(None, "", 'password, "hpotter1234", Some("POTTER, Harry")).save
//         testUser1.id must not be empty
//       }
//     }

//     "User.delete" >> {
//       // Start fake application
//       running(FakeApplication()) {
//         // Deleting user
//         User.findByUsername('password, "hpotter1234").map(_.delete)

//         // verify that user does not exist
//         User.findByUsername('password, "hpotter1234") must be empty
//       }
//     }
//   }
// }
