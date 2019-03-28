package test

import play.api.test.FakeApplication
import play.api.test.Helpers.inMemoryDatabase
import play.api.Logger
import play.api.Play

import org.specs2.specification.BeforeAfterAll

trait ApplicationContext extends BeforeAfterAll {
  type SpecsResult = org.specs2.matcher.MatchResult[Any]

  private[this] val app = FakeApplication(additionalConfiguration=inMemoryDatabase())
  private[this] val mutex = new AnyRef()

  // This function runs some block of code in the context of
  // a Play application
  def application(block: => SpecsResult): SpecsResult =
    mutex.synchronized {
      block
    }

  def beforeAll() {
    Logger.debug("Starting Play App...")
    Play.start(app)
  }

  def afterAll() {
    Logger.debug("Stopping Play App...")
    Play.stop(app)
  }
}

