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
case class Collection(id: Option[Long], owner: Long, name: String) extends SQLSavable with SQLDeletable {
  /**
   * Saves the collection to the DB
   * @return The possibly updated collection
   */
  def save =
  	Logger.debug("DAVID: Collection.save was called!")
    if (id.isDefined) {
    	// Update content graph???
      // update(Course.tableName, 'id -> id.get, 'name -> name, 'startDate -> startDate, 'endDate -> endDate, 'featured -> featured)
      this
    } 
    else {
    	// Insert into content graph???
      // val id = insert(Course.tableName, 'name -> name, 'startDate -> startDate, 'endDate -> endDate, 'featured -> featured)
      this.copy(id)
    }

  /**
   * Deletes the course from the DB
   */
  def delete() {
    DB.withConnection { implicit connection =>
  //     try {
  //       BatchSql(
		//   "delete from {table} where courseId = {id}",
		//   List('table -> "coursePermissions", 'id -> id),
		//   List('table -> "courseMembership", 'id -> id),
		//   List('table -> "contentListing", 'id -> id)
		// ).execute()
  //     } catch {
  //       case e: SQLException =>
  //         Logger.debug("Failed in Course.scala / delete")
  //         Logger.debug(e.getMessage())
  //     }
  		Logger.debug("DAVID: Collection.delete was called!")
    }

    delete(Course.tableName)
  }

  //                  _   _
  //        /\       | | (_)
  //       /  \   ___| |_ _  ___  _ __  ___
  //      / /\ \ / __| __| |/ _ \| '_ \/ __|
  //     / ____ \ (__| |_| | (_) | | | \__ \
  //    /_/    \_\___|\__|_|\___/|_| |_|___/
  //
  //  ______ ______ ______ ______ ______ ______ ______ ______ ______
  // |______|______|______|______|______|______|______|______|______|
  //


  def addCoowner(id:Option[Long]){
  	// TODO:
  	// [ ] : Check if is professor
  }


  /**
   * Post content to the course
   * @param content The content to be posted
   * @return The content listing
   */
  def addContent(content: Content): ContentListing = ContentListing(None, this.id.get, content.id.get).save

  /**
   * Remove content from the course
   * @param content The content to be removed
   * @return The course
   */
  def removeContent(content: Content): Collection = {
    //val listing = ContentListing.listByCollection(this).filter(_.contentId == content.id.get)

    // Check the number or results
    //if (listing.size == 1) {
    // One membership. So delete it
      //listing(0).delete()
    //} else if (listing.size != 0) {
    // We didn't get exactly one listing so delete one of them, but warn
      //Logger.warn("Multiple content listings for content #" + content.id.get + " in course #" + id.get)
      //listing(0).delete()
    //}
    this
  }

  //       _____      _   _
  //      / ____|    | | | |
  //     | |  __  ___| |_| |_ ___ _ __ ___
  //     | | |_ |/ _ \ __| __/ _ \ '__/ __|
  //     | |__| |  __/ |_| ||  __/ |  \__ \
  //      \_____|\___|\__|\__\___|_|  |___/
  //
  //  ______ ______ ______ ______ ______ ______ ______ ______ ______
  // |______|______|______|______|______|______|______|______|______|
  //

  val cacheTarget = this
  object cache {
    var students: Option[List[User]] = None

    def getStudents = {
      //if (students.isEmpty)
        //students = Some(CourseMembership.listClassMembers(cacheTarget, teacher = false))
      //students.get
      Nil
    }

    var teachers: Option[List[User]] = None

    def getTeachers = {
      //if (teachers.isEmpty)
        //teachers = Some(CourseMembership.listClassMembers(cacheTarget, teacher = true))
      //teachers.get
      Nil
    }

    var content: Option[List[Content]] = None

    def getContent = {
      //if (content.isEmpty)
        //content = Some(ContentListing.listClassContent(cacheTarget))
      //content.get
      Nil
    }
  }

  /**
   * Get the enrolled students
   * @return The list of users who are students
   */
  def getStudents: List[User] = cache.getStudents

  /**
   * Get the enrolled teachers
   * @return The list of users who are teachers
   */
  def getTeachers: List[User] = cache.getTeachers

  /**
   * Get all the members (teachers and students)
   * @return The list of all members
   */
  def getMembers: List[User] = getTeachers ++ getStudents

  /**
   * Get content posted to this course
   * @return The list of content
   */
  def getContent: List[Content] = cache.getContent

  /**
   * Get content posted to this course that the current user is allowed to see
   * @return The list of content
   */
  //def getContentFor(user: User): List[Content] =
   // if (user.hasSitePermission("admin")) cache.getContent
    //else cache.getContent.filter { c =>
      //c.visibility != Content.visibility._private || user.getContent.contains(c)
    //}

  //def getUserPermissions(user: User): List[String] = CoursePermissions.listByUser(this, user)
  //def addUserPermission(user: User, permission: String) = CoursePermissions.addUserPermission(this, user, permission)
  //def removeUserPermission(user: User, permission: String) = CoursePermissions.removeUserPermission(this, user, permission)
  //def removeAllUserPermissions(user: User) = CoursePermissions.removeAllUserPermissions(this, user)
  //def userHasPermission(user: User, permission: String) = CoursePermissions.userHasPermission(this, user, permission)
}

object Collection extends SQLSelectable[Collection] {
  val tableName = "course"

  val simple = {
    get[Option[Long]](tableName + ".id") ~
      get[Long](tableName + ".owner") ~
      get[String](tableName + ".name") map {
      case id~owner~name =>
        Collection(id, owner, name)
    }
  }

  /**
   * Find a course with the given id
   * @param id The id of the course
   * @return If a course was found, then Some[Collection], otherwise None
   */
  def findById(id: Long): Option[Collection] = findById(id, simple)

  /**
   * Gets all the courses in the DB
   * @return The list of courses
   */
  def list: List[Collection] = list(simple)

  /**
   * Create a course from fixture data
   * @param data Fixture data
   * @return The user
   */
  def fromFixture(data: (Long, String)): Collection =
    Collection(None, data._1, data._2)

  /**
   * Search the names of courses
   * @param query The string to look for
   * @return The list of courses that match
   */
  def search(query: String): List[Collection] = Nil
    
    // DB.withConnection { implicit connection =>
    //   val sqlQuery = "%" + query + "%"
    //   try {
    //     SQL(s"select * from $tableName where name like {query} order by name asc")
    //       .on('query -> sqlQuery).as(simple *)
    //   } catch {
    //     case e: SQLException =>
    //       Logger.debug("Failed in Collection.scala / search")
    //       Logger.debug(e.getMessage())
    //       List[Collection]()
    //   }
    // }
}
