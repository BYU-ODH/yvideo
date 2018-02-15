import play.api.test._
import play.api.mvc._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import models.{Content, User, Course, Collection}
import controllers.Collections

object CollectionControllerSpec extends Specification {

  class CollectionTestController() extends Controller with Collections

  "The Collection Controller" should {

    "return a collection" in { 
        implicit ee: ExecutionEnv =>
            running(FakeApplication()) {
                implicit val collection = Collection(None, 13, "fakeCourse")
                val controller = new CollectionTestController()

                val result = controller.getCollection(13L){
                    (collection) => Future(Results.Ok("test"))
                }(FakeRequest())

                result map ( res => {
                    println(res.toString)
                    println(res.header.toString)
                    res.header.status shouldEqual 303
                }) await
        }
    }

  }
}
