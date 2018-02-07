import play.api.test._
import play.api.mvc._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.Logger
import models.User

object UserModelTest extends Specification {
  
  "User Model Tests" >> {

    "Correct Creation (in memory) Test" >> { 
      // User(id: Option[Long], authId: String, authScheme: Symbol, username: String, name: Option[String])
      val testUser = User(None, "", 'password, "hpotter1234", Some("POTTER, Harry"))
      
      "Correct name" >> { 
        testUser.name.get must_== "POTTER, Harry" 
      }
      
      "Correct username" >> { 
        testUser.username must_== "hpotter1234" 
      }
    }
    
    "User.save" >> {
      // Start fake application
      running(FakeApplication()) {
        val testUser1 = User(None, "", 'password, "hpotter1234", Some("POTTER, Harry")).save
        Logger.debug("Created User")
        Logger.debug(testUser1.toString)
        testUser1.id must not be empty
      }
    }

    "User.delete" >> {
      // Start fake application
      running(FakeApplication()) {
        Logger.debug("Found User: " + User.findByUsername('password, "hpotter1234") .toString)

        // Deleting user
        User.findByUsername('password, "hpotter1234").get.delete
        Logger.debug("Deleted User: " + User.findByUsername('password, "hpotter1234").toString)

        // verify that user does not exist
        User.findByUsername('password, "hpotter1234") must be empty
      }
    }  
  }
}