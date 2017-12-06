package models

import anorm._
import anorm.SqlParser._
import java.sql.SQLException
import dataAccess.sqlTraits._
import play.api.Logger
import service.{TimeTools, HashTools}
import util.Random
import play.api.db.DB
import play.api.Play.current

/**
 * A course. Students and teachers are members. Content can be posted here.
 * @param id The id of the course
 * @param name The name of the course
 * @param startDate When the course become functional
 * @param endDate When the course ceases to be functional
 */
case class Course(id: Option[Long], yearTerm: String, subjectArea: String, catalogNumber: String,
  sectionNumber: String, curriculumId: String, titleCode: String, sectionType: String, blockCode: String, courseTitle: String) extends SQLSavable with SQLDeletable {

  /**
   * Saves the course to the DB
   * @return The possibly updated course
   */
  def save =
    if (id.isDefined) {
      update(Course.tableName,'id -> id.get,'yearTerm -> yearTerm, 'subjectArea -> subjectArea,
        'catalogNumber -> catalogNumber,'sectionNumber -> sectionNumber,'curriculumId -> curriculumId,
        'titleCode -> titleCode,'sectionType -> sectionType,'blockCode -> blockCode,'courseTitle -> courseTitle)
      this
    } else {
      val id = insert(Course.tableName, 'yearTerm -> yearTerm, 'subjectArea -> subjectArea,
        'catalogNumber -> catalogNumber,'sectionNumber -> sectionNumber,'curriculumId -> curriculumId,
        'titleCode -> titleCode,'sectionType -> sectionType,'blockCode -> blockCode,'courseTitle -> courseTitle)
      this.copy(id)
    }

  /**
   * Deletes the course from the DB
   */
  def delete() {
    DB.withConnection { implicit connection =>
      try {
        BatchSql(
		  "delete from {table} where courseId = {id}",
		  List('table -> "coursePermissions", 'id -> id),
		  List('table -> "courseMembership", 'id -> id),
		  List('table -> "contentListing", 'id -> id)
		).execute()
      } catch {
        case e: SQLException =>
          Logger.debug("Failed in Course.scala / delete")
          Logger.debug(e.getMessage())
      }
    }

    delete(Course.tableName)
  }
}

object Course extends SQLSelectable[Course] {
  val tableName = "course"

  val simple = {
    get[Option[Long]](tableName + ".id") ~
      get[String](tableName + ".yearTerm") ~
      get[String](tableName + ".subjectArea") ~
      get[String](tableName + ".catalogNumber") ~
      get[String](tableName + ".sectionNumber") ~
      get[String](tableName + ".curriculumId") ~
      get[String](tableName + ".titleCode") ~
      get[String](tableName + ".sectionType") ~
      get[String](tableName + ".blockCode") ~
      get[String](tableName + ".courseTitle") map {
      case id~yearTerm~subjectArea~catalogNumber~sectionNumber~
        curriculumId~titleCode~sectionType~blockCode~courseTitle =>
        Course(id, yearTerm, subjectArea, catalogNumber, sectionNumber, 
        curriculumId, titleCode, sectionType, blockCode, courseTitle)
    }
  }

  /**
   * Find a course with the given id
   * @param id The id of the course
   * @return If a course was found, then Some[Course], otherwise None
   */
  def findById(id: Long): Option[Course] = findById(id, simple)

  /**
   * Gets all the courses in the DB
   * @return The list of courses
   */
  def list: List[Course] = list(simple)

  /**
   * Create a course from fixture data
   * @param data Fixture data
   * @return The user
   */
  def fromFixture(data: (String, String, String, String, String, String, String, String, String)): Course =
    Course(None, data._1, data._2, data._3, data._4, data._5, data._6, data._7, data._8, data._9)

  /**
   * Search the names of courses
   * @param query The string to look for
   * @return The list of courses that match
   */
  def search(query: String): List[Course] =
    DB.withConnection { implicit connection =>
      val sqlQuery = "%" + query + "%"
      try {
        SQL(s"select * from $tableName where name like {query} order by name asc")
          .on('query -> sqlQuery).as(simple *)
      } catch {
        case e: SQLException =>
          Logger.debug("Failed in Course.scala / search")
          Logger.debug(e.getMessage())
          List[Course]()
      }
    }
}
