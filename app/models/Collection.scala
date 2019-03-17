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
        SQL("delete from collectionMembership where collectionId = {id}").on('id -> id).execute()
        SQL("delete from collectionPermissions where collectionId = {id}").on('id -> id).execute()
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

  /**
   * Post content to the collection
   * @param content The content to be posted
   * @return The content listing
   */
  def addContent(content: Content): Content = content.copy(collectionId = this.id.get).save

  /**
   * Remove content from the collection
   * @param content The content to be removed
   * @return The collection
   */
  def removeContent(content: Content): Collection = {
    content.delete()
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

    case class CacheListHolder[A](cachedObject: Option[List[A]] = None) {
      def getList: List[A] = {
        if (cachedObject.isEmpty)
          List[A]()
        else
          cachedObject.get
      }
    }

    var students = CacheListHolder[User]()

    var teachers = CacheListHolder[User]()

    var content = CacheListHolder[Content]()

    var linkedCourses = CacheListHolder[Course]()

    // TODO: make this a list of users
    var exceptions = CacheListHolder[CollectionMembership]()

    var tas = CacheListHolder[User]()

    // template function
    def getForCollection[A](setter: CacheListHolder[A] => Unit, getter: () => CacheListHolder[A], function: Collection => List[A]) = {
      if (getter().cachedObject.isEmpty)
        setter(CacheListHolder(Some(function(cacheTarget))))
      getter()
    }
  }

  /**
   * Get the collection TAs
   * @return The list of users who are TAs
   */
  def getTAs: List[User] = cache.getForCollection[User](cache.tas_=, cache.tas _, CollectionMembership.getTAs) match {
    case tas: cache.CacheListHolder[User] => tas.getList
    case _ => List[User]()
  }

  /**
   * Get the enrolled students
   * @return The list of users who are students
   */
  def getStudents: List[User] = cache.getForCollection[User](cache.students_=, cache.students _, CollectionMembership.listClassMembers(_: Collection, false)) match {
    case students: cache.CacheListHolder[User] => students.getList
    case _ => List[User]()
  }

  /**
   * Get the enrolled teachers
   * @return The list of users who are teachers
   */
  def getTeachers: List[User] = cache.getForCollection[User](cache.teachers_=, cache.teachers _, CollectionMembership.listClassMembers(_: Collection, true)) match {
    case teachers: cache.CacheListHolder[User] => teachers.getList
    case _ => List[User]()
  }

  /**
   * Get all the members (teachers and students)
   * @return The list of all members
   */
  def getMembers: List[User] = getTeachers ++ getStudents ++ getTAs

  /**
   * Get content posted to this collection
   * @return The list of content
   */
  def getContent: List[Content] = cache.getForCollection[Content](cache.content_=, cache.content _, coll => Content.list.filter(_.collectionId == coll.id.get)) match {
    case content: cache.CacheListHolder[Content] => content.getList
    case _ => List[Content]()
  }

  /**
   * Get content posted to this collection that the current user is allowed to see
   * @return The list of content
   */
  def getContentFor(user: User): List[Content] =
    if (user.hasSitePermission("admin") || userIsTeacher(user) || userIsTA(user)) this.getContent
    else this.getContent.filter { c =>
      c.published && c.enabled
    }

  def getLinkedCourses: List[Course] = cache.getForCollection[Course](cache.linkedCourses_=, cache.linkedCourses _, CollectionCourseLink.listCollectionCourses) match {
    case courses: cache.CacheListHolder[Course] => courses.getList
    case _ => List[Course]()
  }

  def getUserPermissions(user: User): List[String] = CollectionPermissions.listByUser(this, user)
  def addUserPermission(user: User, permission: String) = CollectionPermissions.addUserPermission(this, user, permission)
  def removeUserPermission(user: User, permission: String) = CollectionPermissions.removeUserPermission(this, user, permission)
  def removeAllUserPermissions(user: User) = CollectionPermissions.removeAllUserPermissions(this, user)

  def userIsTA(user: User) = getTAs.contains(user)
  def userIsTeacher(user: User) = getTeachers.contains(user)
  def userIsAdmin(user: User) = userIsTA(user) || userIsTeacher(user)
  def userCanEditContent(user: User) = userIsAdmin(user)
  def userCanViewContent(user: User) = getMembers.contains(user)
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

  /**
   * Gets the list of collections that the user should be enrolled in
   * by searching for collections that are linked to the courses provided here
   * @return The list of collections
   */
  def getEligibleCollections(courseNames: List[Course], user: User): List[Collection] = {
    DB.withConnection { implicit connection =>
      val courses = Course.findInexact(courseNames)
      val exceptions = CollectionMembership.getExceptionsByUser(user)
      val linkedCourses = CollectionCourseLink.getLinkedCollections(courses)
      try {
        val collectionIds = linkedCourses.map(_.collectionId) ::: exceptions.map(_.collectionId)
        if (collectionIds.isEmpty)
          List[Collection]()
        else
          SQL(s"select * from $tableName where id in ({collectionIds})")
            .on('collectionIds ->  collectionIds).as(simple *)
      } catch{
        case e: SQLException =>
          Logger.debug("Failed in Collection.scala / getEligibleCollections")
          Logger.debug(e.getMessage())
          List[Collection]()
      }
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
