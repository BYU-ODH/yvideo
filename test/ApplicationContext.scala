package test

import anorm._
import anorm.SqlParser.str

import java.sql.ResultSet
import java.sql.SQLException

import org.specs2.specification.BeforeAfterAll

import play.api.db.DB
import play.api.test.FakeApplication
import play.api.test.Helpers.inMemoryDatabase
import play.api.Application
import play.api.Logger
import play.api.Play
import play.api.Play.current

/**
 * These are some traits to keep track of application and database contexts.
 * ApplicationContext will start and stop an application before and after running
 * all of the test cases in a single Specification.
 * The app is configured with an in memory h2 database that is deleted whenever the
 * app is stopped.
 * It requires a DBManagement implementation so that it can call the functions
 * dbSetup and dbTeardown.
 * DBClear wipes the database in the dbTeardown step.
 * This is done so the application is started once at the beginning of the specification.
 * The DBManagement trait is used to insert whatever other before after logic is required
 * on a per specification basis.
 */
trait DBManagement {
  def dbSetup()
  def dbTeardown()
}

trait ApplicationContext extends BeforeAfterAll {
  this: DBManagement =>
  type SpecsResult = org.specs2.matcher.MatchResult[Any]

  private[this] val app = FakeApplication(additionalConfiguration=inMemoryDatabase())
  private[this] val mutex = new AnyRef()

  // This function runs some block of code that returns a SpecsResult
  // in the context of a Play application
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

trait DBClear extends DBManagement {
  private[this] class RsIterator(rs: ResultSet) extends Iterator[ResultSet] {
    def hasNext: Boolean = rs.next()
    def next(): ResultSet = rs
  }

  def dbSetup() {}

  /**
   * Clears the Database.
   * Truncates all application tables and reseeds sequences.
   */
  def dbTeardown() {
    DB.withConnection { implicit conn =>
      try {
        val resultSet = conn.getMetaData().getTables(null, null, "%", Array[String]("TABLE"))
        new RsIterator(resultSet).map(r => r.getString("TABLE_NAME"))
          .toList
          .foreach(name => SQL(
            s"""set foreign_key_checks=0;
                TRUNCATE TABLE $name;
                set foreign_key_checks=1;
            """
          ).execute())
        SQL("SELECT SEQUENCE_NAME FROM INFORMATION_SCHEMA.SEQUENCES WHERE SEQUENCE_SCHEMA='PUBLIC'")
          .as(str("SEQUENCE_NAME")*)
          .foreach(seq => SQL(s"ALTER SEQUENCE $seq RESTART WITH 1").execute())
      } catch {
        case e: SQLException => Logger.error(e.getMessage())
        case _: Throwable => Logger.error("DBClear: Failed to wipe database")
      }
    }
  }
}

