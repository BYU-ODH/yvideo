import play.api.test._
import play.api.mvc._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import models.{Content, User, Course, Collection}
import controllers.Administration

object AdministrationControllerSpec extends Specification {

  class AdministrationTestController() extends Controller with Administration

  "The Administration Controller" should {

    "reject non-authenticated users" in { 
        implicit ee: ExecutionEnv =>
            running(FakeApplication()) {
                val userOpt = User.findByUsername('password, "student1")
                userOpt mustNotEqual None
                implicit val user = userOpt.get
                user.id mustNotEqual None
                val controller = new AdministrationTestController()

                val result = controller.admin()(FakeRequest())

                result map ( res => {
                    println(res.toString)
                    println(res.header.toString)
                    println(res.header.status)
                    res.header.status shouldEqual 303
                }) await
        }
    }

  }
}
