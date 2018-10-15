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
                val userOpt = User.findByUsername('password, "admin")
                userOpt mustNotEqual None
                val user = userOpt.get
                user.id mustNotEqual None
                val collection = Collection(None, user.id.get, "Test Course").save
                collection.id mustNotEqual None

                val controller = new CollectionTestController()

                val result = controller.getCollection(collection.id.get){
                    (coll) => Future(Results.Ok("XD"))
                }(FakeRequest())

                result map ( res => {
                    res.header.status shouldEqual 200
                }) await
        }
    }

  }
}
