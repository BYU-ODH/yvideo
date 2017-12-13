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
 * A collection. Students and teachers are members. Content can be posted here.
 * @param id The id of the collection
 * @param name The name of the collection
 * @param startDate When the collection become functional
 * @param endDate When the collection ceases to be functional
 */
case class Collection(id: Option[Long], owner: Long, name: String) extends SQLSavable with SQLDeletable {
  /**
   * Saves the collection to the DB
   * @return The possibly updated collection
   */
  def save =
    if (id.isDefined) {
      update(Collection.tableName, 'owner -> owner, 'name -> name)
      this
    } 
    else {
      val id = insert(Collection.tableName, 'owner -> owner, 'name -> name)
      this.copy(id)
    }

  /**
   * Deletes the collection from the DB
   */
  def delete() {
    DB.withConnection { implicit connection =>
      try {
        BatchSql(
          "delete from {table} where collectionId = {id}",
          List('table -> "collectionPermissions", 'id -> id),
          List('table -> "collectionMembership", 'id -> id),
          List('table -> "collectionCourseLink", 'id -> id),
          List('table -> "contentListing", 'id -> id)
        ).execute()
      } catch {
        case e: SQLException =>
          Logger.debug("Failed in Collection.scala / delete")
          Logger.debug(e.getMessage())
      }
    }

    delete(Collection.tableName)
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
   * Post content to the collection
   * @param content The content to be posted
   * @return The content listing
   */
  def addContent(content: Content): ContentListing = ContentListing(None, this.id.get, content.id.get).save

  /**
   * Remove content from the collection
   * @param content The content to be removed
   * @return The collection
   */
  def removeContent(content: Content): Collection = {
    val listing = ContentListing.listByCollection(this).filter(_.contentId == content.id.get)

    // Check the number or results
    if (listing.size == 1) {
      // One membership. So delete it
      listing(0).delete()
    } else if (listing.size != 0) {
      // We didn't get exactly one listing so delete one of them, but warn
      Logger.warn("Multiple content listings for content #" + content.id.get + " in collection #" + id.get)
      listing(0).delete()
    }
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
      if (students.isEmpty)
        students = Some(CollectionMembership.listClassMembers(cacheTarget, teacher = false))
      students.get
    }

    var teachers: Option[List[User]] = None

    def getTeachers = {
      if (teachers.isEmpty)
        teachers = Some(CollectionMembership.listClassMembers(cacheTarget, teacher = true))
      teachers.get
    }

    var content: Option[List[Content]] = None

    def getContent = {
      if (content.isEmpty)
        content = Some(ContentListing.listClassContent(cacheTarget))
      content.get
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
   * Get content posted to this collection
   * @return The list of content
   */
  def getContent: List[Content] = cache.getContent

  /**
   * Get content posted to this collection that the current user is allowed to see
   * @return The list of content
   */
  def getContentFor(user: User): List[Content] =
    if (user.hasSitePermission("admin")) cache.getContent
    else cache.getContent.filter { c =>
      c.visibility != Content.visibility._private || user.getContent.contains(c)
    }

  def getUserPermissions(user: User): List[String] = CollectionPermissions.listByUser(this, user)
  def addUserPermission(user: User, permission: String) = CollectionPermissions.addUserPermission(this, user, permission)
  def removeUserPermission(user: User, permission: String) = CollectionPermissions.removeUserPermission(this, user, permission)
  def removeAllUserPermissions(user: User) = CollectionPermissions.removeAllUserPermissions(this, user)
  def userHasPermission(user: User, permission: String) = CollectionPermissions.userHasPermission(this, user, permission)
}

object Collection extends SQLSelectable[Collection] {
  val tableName = "collection"

  val simple = {
    get[Option[Long]](tableName + ".id") ~
      get[Long](tableName + ".owner") ~
      get[String](tableName + ".name") map {
      case id~owner~name =>
        Collection(id, owner, name)
    }
  }

  /**
   * Find a collection with the given id
   * @param id The id of the collection
   * @return If a collection was found, then Some[Collection], otherwise None
   */
  def findById(id: Long): Option[Collection] = findById(id, simple)

  /**
   * Gets all the collections in the DB
   * @return The list of collections
   */
  def list: List[Collection] = list(simple)

  def addCourse(collectionId: Long, courseName: String) = {
    val newCourse = Course(None, courseName).save
    CollectionCourseLink(None, courseId, )
  }

  /**
   * Gets the list of collections that the user should be enrolled in
   * by searching for collections that are linked to the courses provided here
   * @return The list of collections
   */
  def getEligibleCollections(courseNames: List[String]): List[Collection] = {
    DB.withConnection { implicit connection =>
      val courses = Course.getCoursesByName(courseNames)
      val linkedCourses = CollectionCourseLink.getLinkedCollections(courses)
      SQL(s"select * from $tableName where id in ({collectionIds})")
        .on('collectionIds -> linkedCourses.map(_.collectionId)).as(simple *)
    }
  }

  /**
   * Create a collection from fixture data
   * @param data Fixture data
   * @return The user
   */
  def fromFixture(data: (Long, String)): Collection =
    Collection(None, data._1, data._2)

  /**
   * Search the names of collections
   * @param query The string to look for
   * @return The list of collections that match
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
