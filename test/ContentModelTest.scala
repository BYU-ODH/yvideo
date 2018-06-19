import play.api.test._
import play.api.mvc._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.Logger
import models.Content

object ContentModelTest extends Specification {
  
  "Content Model Tests" >> {

    "Correct Creation (in memory) Test" >> { 
    // Content(id: Option[Long], name: String, contentType: Symbol, thumbnail: String, resourceId: String,
    //                dateAdded: String = TimeTools.now(),
    //                visibility: Int = Content.visibility.tightlyRestricted,
    //                authKey: String = HashTools.md5Hex(util.Random.nextString(16)), labels: List[String] = Nil, views: Long = 0)

      val testContent = Content(None, "Harry Potter: The Sorcerer's Stone", 'video, "", "")
      
      "Correct name" >> { 
        Logger.debug(testContent.name)
        testContent.name must_== "Harry Potter: The Sorcerer's Stone"
      }
    }
    
    "Simple Database Tests" >> {
      var testContentId: Long = -1L

      "Content.save -- content saved to database" >> {
        // Start fake application
        running(FakeApplication()) {
          val testContent = Content(None, "Harry Potter: The Sorcerer's Stone", 'video, "", "").save
          Logger.debug("Created Content: " + testContent.toString)

          testContentId = testContent.id.get
          Logger.debug("testContentId:" + testContentId)
          (testContent.id must not be empty) && (testContentId == -1L) must_== false
        }
      }

//     "Content.delete -- content deleted from database" >> {
//        // Start fake application
//        running(FakeApplication()) {
//          Logger.warn("testContentId: " + testContentId)
//          Logger.debug("Found Content: " + Content.findById(testContentId).get.toString)
//
//          // Deleting user
//          Content.findById(testContentId).get.delete
//          Logger.debug("Deleted Content: " + Content.findById(testContentId).toString)
//
//          // verify that user does not exist
//          Content.findById(testContentId).toString must be empty
//        }
//      }
    }
  }
}
