package models

import anorm._
import anorm.SqlParser._
import java.sql.SQLException
import dataAccess.sqlTraits._
import util.Random
import play.api.Logger
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import service.{TimeTools, HashTools}
import play.api.db.DB
import play.api.Play.current

/**
 * A course.
 * @param id The id of the course
 * @param department The department of the course
 * @param catalogNumber The catalogNumber of the course
 * @param sectionNumber The sectionNumber of the course
 */
case class Course(id: Option[Long], department: String, catalogNumber: Option[String], sectionNumber: Option[String]) extends SQLSavable with SQLDeletable {

  /**
   * Saves the course to the DB
   * @return The possibly updated course
   */
  def save =
    if (id.isDefined) {
      if (this.catalogNumber.isEmpty && !this.sectionNumber.isEmpty) {
        Logger.warn("The following course with section number but with no catalog Number called save:")
        Logger.warn(this.toString)
        // TODO: throw an exception
        this
      } else {
        update(Course.tableName,'id -> id.get, 'department -> department,
          'catalogNumber -> catalogNumber, 'sectionNumber -> sectionNumber)
        this
      }
    } else {
      val id = insert(Course.tableName, 'department -> department,
        'catalogNumber -> catalogNumber, 'sectionNumber -> sectionNumber)
      this.copy(id)
    }

  /**
   * Deletes the course from the DB
   */
  def delete() {
    DB.withConnection { implicit connection =>
      try {
        SQL(s"delete from ${CollectionCourseLink.tableName} where courseId = {id}")
          .on('id -> id)
      } catch {
        case e: SQLException =>
          Logger.debug("Failed in Course.scala / delete")
          Logger.debug(e.getMessage())
      }
    }

    delete(Course.tableName)
  }

  def name = s"${this.department}${this.catalogNumber.getOrElse("")}${if (!this.catalogNumber.isEmpty && !this.sectionNumber.isEmpty) " - " + this.sectionNumber.get}"

  def toJson = Json.obj(
    "id" -> id.get,
    "department" -> this.department,
    "catalogNumber" -> this.catalogNumber,
    "sectionNumber" -> this.sectionNumber
  )
}

object Course extends SQLSelectable[Course] {
  val tableName = "course"

  val simple = {
    get[Option[Long]](tableName + ".id") ~
    get[String](tableName + ".department") ~
    get[Option[String]](tableName + ".catalogNumber") ~
    get[Option[String]](tableName + ".sectionNumber") map {
      case id~department~catalogNumber~sectionNumber =>
        Course(id, department, catalogNumber, sectionNumber)
    }
  }

  // Reads method for parsing json into course objects
  implicit val courseReads: Reads[Course] = (
    (JsPath \ "id").readNullable[Long] ~
    (JsPath \ "department").read[String] ~
    (JsPath \ "catalogNumber").readNullable[String] ~
    (JsPath \ "sectionNumber").readNullable[String]
  )(Location.apply _)

  /**
   * Find a course with the given id
   * @param id The id of the course
   * @return If a course was found, then Some[Course], otherwise None
   */
  def findById(id: Long): Option[Course] = findById(id, simple)

  /**
   * parse the name of the course into three parts: (department, catalogNumber and sectionNumber)
   */
  def parseName(name: String) = {
    val course = name.trim
    val department = "[a-zA-Z/s]+"
    val catalog = s"${department}\\d\\d\\d"
    val section = s"${catalog} - \\d\\d\\d"
    if (course.matches(section)) {
      val sectionNumber = course.takeRight(3)
      val catalogNumber = course.dropRight(6).takeRight(3)
      val deptName = course.dropRight(9)
    } else if (course.matches(catalog)) {
      val catalogNumber = course.takeRight(3)
      val deptName = course.dropRight(3)
    } else if (course.matches(department)) {
      val deptName = course
    }
    //(deptName, catalogNumber, )
  }

  /**
   * Find courses with the given names
   * @param courses The list of course names
   * @return A list of courses
   */
  def findByName(courses: List[String]): List[Course] = {
    Logger.info(courses.toString)
    DB.withConnection { implicit connection =>
      SQL(s"select * from $tableName where department in ({courses})")
        .on('courses -> courses).as(simple *)
    }
  }

  /**
   * This takes a list of Courses that have not been saved in the database
   * and returns courses that match at the highest level.
   * Ex: The course HUM101 - 002 will match HUM101 - 002 or HUM101 or HUM
   * depending on the course that matches the most
   * @param courses The List of courses that we want to find in the database
   * @return The list of saved courses that corresponds to the courses that were provided
   **/
  def findCourses(courses: List[Course]): List[Course] = {
    Logger.info(courses.toString)
    DB.withConnection { implicit connection =>
      var matchedCourses: List[Course] = Nil
      courses.foreach { c =>
	      val found = SQL(s"select * from tableName where department in {dep} and (catalogNumber = {cn} or catalogNumber is null) and (sectionNumber = {sn} or sectionNumber is null)")
		.on('dep -> c.department, 'cn -> c.catalogNumber, 'sn -> c.sectionNumber).as(simple *)
	      matchedCourses = found ::: matchedCourses
      }
      matchedCourses
    }
  }

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
  def fromFixture(data: String): Course =
    Course(None, data, None, None)

  /**
   * Search the names of courses
   * @param query The string to look for
   * @return The list of courses that match
   */
  def search(query: String): List[Course] =
    DB.withConnection { implicit connection =>
      val sqlQuery = "%" + query + "%"
      try {
        SQL(s"select * from $tableName where department like {query} order by name asc")
          .on('query -> sqlQuery).as(simple *)
      } catch {
        case e: SQLException =>
          Logger.debug("Failed in Course.scala / search")
          Logger.debug(e.getMessage())
          List[Course]()
      }
    }
}
