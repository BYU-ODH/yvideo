package test

import play.api.test.FakeApplication
import play.api.test.Helpers.inMemoryDatabase
import play.api.Logger
import play.api.Play
import play.api.Application

import org.specs2.specification.BeforeAfterAll
import org.specs2.specification.ForEach
import org.specs2.specification.AroundEach
import org.specs2.execute.AsResult
import org.specs2.execute.Result

trait ApplicationContext extends BeforeAfterAll {
  this: DBManagement =>
  type SpecsResult = org.specs2.matcher.MatchResult[Any]

  private[this] val app = FakeApplication(additionalConfiguration=inMemoryDatabase())
  private[this] val mutex = new AnyRef()

  // This function runs some block of code in the context of
  // a Play application
  def application(block: => SpecsResult): SpecsResult =
    mutex.synchronized {
      dbSetup()
      val res = block
      dbTeardown()
      res
    }

  def beforeAll() { Play.start(app) }
  def afterAll() { Play.stop(app) }
}

trait DBManagement {
  def dbSetup()
  def dbTeardown()
}

trait DBClear extends DBManagement {
  def dbSetup() {
    Logger.debug("setting up db")
  }

  def dbTeardown() {
    Logger.debug("cleaning up db")
  }
}

