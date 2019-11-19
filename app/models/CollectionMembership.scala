package models

import anorm._
import anorm.SqlParser._
import java.sql.SQLException
import play.api.Logger
import dataAccess.sqlTraits._
import play.api.db.DB
import play.api.Play.current
import play.api.libs.json._

/**
 * Represents the membership of a user in a collection
 * @param id The id of the membership
 * @param userId The id of the user that is enrolled
 * @param collectionId The id of the collection in which the user is enrolled
 * @param teacher Is the user a teacher?
 * @param exception Does the user have an exception?
 */
case class CollectionMembership(id: Option[Long], userId: Long, collectionId: Long, teacher: Boolean, exception: Boolean) extends SQLSavable with SQLDeletable {

  /**
   * Saves the collection membership to the DB
   * @return The possibly modified collection membership
   */
  def save =
    if (id.isDefined) {
      update(CollectionMembership.tableName, 'id -> id.get, 'userId -> userId, 'collectionId -> collectionId, 'teacher -> teacher, 'exception -> exception)
      this
    } else {
      DB.withConnection { implicit connection =>
        try {
          // won't add users to the collection if they are already enrolled in it
          // Don't use userIsEnrolled method because that takes objects, not IDs
          val result = SQL(s"select 1 from ${CollectionMembership.tableName} where userId = {uid} and collectionId = {cid}")
            .on('uid -> userId, 'cid -> collectionId)
            .fold(0) { (c, _) => c + 1 } // fold SqlResult
            .fold(_ => 0, c => c) // fold Either
          if (result == 0) {
            val id = insert(CollectionMembership.tableName, 'userId -> userId, 'collectionId -> collectionId, 'teacher -> teacher, 'exception -> exception)
            this.copy(id)
          } else {
            this
          }
        } catch {
          case e: SQLException =>
            Logger.debug("Failed in CollectionMembership.scala / save")
            Logger.debug(e.getMessage())
            throw e
        }
      }
    }

  /**
   * Deletes the collection membership from the DB
   */
  def delete() {
    val cid = this.collectionId
    val uid = this.userId
    DB.withConnection { implicit connection =>
      try {
        SQL(s"delete from ${CollectionPermissions.tableName} where collectionId = {cid} and userId = {uid}")
          .on('cid -> cid, 'uid -> uid).execute()
      } catch {
        case e: SQLException =>
          Logger.debug("Failed in CollectionMembership.scala / delete")
          Logger.debug(e.getMessage())
      }
    }
    delete(CollectionMembership.tableName)
  }


  def toJson() = Json.obj(
    "id" -> id.get,
    "userId" -> userId,
    "collectionId" -> collectionId,
    "teacher" -> teacher,
    "exception" -> exception
  )


}

object CollectionMembership extends SQLSelectable[CollectionMembership] {
  val tableName = "collectionMembership"

  val simple = {
    get[Option[Long]](tableName + ".id") ~
      get[Long](tableName + ".userId") ~
      get[Long](tableName + ".collectionId") ~
      get[Boolean](tableName + ".teacher") ~
      get[Boolean](tableName + ".exception") map {
      case id~userId~collectionId~teacher~exception => CollectionMembership(id, userId, collectionId, teacher, exception)
    }
  }

  /**
   * Finds a collection membership by the id
   * @param id The id of the membership
   * @return If a collection membership was found, then Some[CollectionMembership], otherwise None
   */
  def findById(id: Long): Option[CollectionMembership] = findById(id, simple)

  /**
   * Lists the membership pertaining to a certain user
   * @param user The user for whom the membership will be
   * @return The list of collection membership
   */
  def listByUser(user: User): List[CollectionMembership] =
    listByCol("userId", user.id, simple)

  /**
   * Lists the membership pertaining to a certain collection
   * @param user The collection for whom the membership will be
   * @return The list of collection membership
   */
  def listByCollection(collection: Collection): List[CollectionMembership] =
    listByCol("collectionId", collection.id, simple)

  /**
   * Finds all collections that a certain user is enrolled in
   * @param user The user for whom the collection list will be
   * @return The list of collections
   */
  def listUsersClasses(user: User): List[Collection] = {
    DB.withConnection { implicit connection =>
      try {
        SQL(
          s"""
          select * from ${Collection.tableName} join $tableName
          on ${Collection.tableName}.id = ${tableName}.collectionId
          where ${tableName}.userId = {id}
          order by name asc
          """
        )
          .on('id -> user.id.get)
          .as(Collection.simple *)
      } catch {
        case e: SQLException =>
          Logger.debug("Failed in CollectionMembership.scala / listUsersClasses")
          Logger.debug(e.getMessage())
          List[Collection]()
      }
    }
  }

  /**
   * Finds all collections that a certain user is teaching
   * @param user The user for whom the collection list will be
   * @return The list of collections
   */
  def listTeacherClasses(user: User): List[Collection] = {
    DB.withConnection { implicit connection =>
      try {
        SQL(
          s"""
          select * from ${Collection.tableName} join $tableName
          on ${Collection.tableName}.id = ${tableName}.collectionId
          where ${tableName}.userId = {id} and ${tableName}.teacher = true
          order by name asc
          """
        )
          .on('id -> user.id.get)
          .as(Collection.simple *)
      } catch {
        case e: SQLException =>
          Logger.debug("Failed in CollectionMembership.scala / listTeacherClassesd")
          Logger.debug(e.getMessage())
          List[Collection]()
      }
    }
  }

  /**
   * Finds all students or teachers who are enrolled in a certain collection
   * @param collection The collection in which the users are enrolled
   * @param teacher Get teachers instead of students?
   * @return The list of users
   */
  def listClassMembers(collection: Collection, teacher: Boolean): List[User] = {
    DB.withConnection { implicit connection =>
      try {
        SQL(
          s"""
          select * from ${User.tableName} join $tableName
          on ${User.tableName}.id = ${tableName}.userId
          where ${tableName}.collectionId = {id} and ${tableName}.teacher = {teacher}
          order by name asc
          """
        )
          .on('id -> collection.id.get, 'teacher -> teacher)
          .as(User.simple *)
      } catch {
        case e: SQLException =>
          Logger.debug("Failed in CollectionMembership.scala / listClassMembers")
          Logger.debug(e.getMessage())
          List[User]()
      }
    }
  }

  /**
   * Checks if a specific user is enrolled in a certain collection
   * @param user The user who's enrollment is being checked
   * @param collection The collection in which the user may be enrolled
   * @return Whether or not they're enrolled
   */
  def userIsEnrolled(user: User, collection: Collection): Boolean =
    DB.withConnection { implicit connection =>
      try {
        val result = SQL(s"select 1 from $tableName where userId = {uid} and collectionId = {cid}")
          .on('uid -> user.id, 'cid -> collection.id)
          .fold(0) { (c, _) => c + 1 } // fold SqlResult
          .fold(_ => 0, c => c) // fold Either
        result > 0
      } catch {
        case e: SQLException =>
          Logger.debug("Failed in CollectionMembership.scala / userIsEnrolled")
          Logger.debug(e.getMessage())
          false
      }
    }

  /**
   * Lists all collection membership
   * @return The list of collection memberships
   */
  def list: List[CollectionMembership] = list(simple)

  /**
   * Lists all of the users that have the TA collection permission in the given collections
   * @return List[User] list of Tas
   */
  def getTAs(collection: Collection): List[User] = {
    DB.withConnection { implicit connection =>
      try {
        SQL(
          s"""
          select * from (select userId from collectionPermissions where permission = 'ta' and collectionId = {id})
          a join userAccount on a.userId = userAccount.id
          """
        )
          .on('id -> collection.id.get)
          .as(User.simple *)
      } catch {
        case e: SQLException =>
          Logger.debug("Failed in CollectionMembership.scala / getTAs")
          Logger.debug(e.getMessage())
          List[User]()
      }
    }
  }

  /**
   * Gets the exceptions in the collection
   * @return the list of CollectionMembership records
   */
  def getExceptionsByCollection(collection: Collection): List[User] =
    DB.withConnection { implicit connection =>
      try {
        SQL (
          s"""
          select * from (select userId from $tableName where exception = true and collectionId = {collId})
          a join userAccount on a.userId = userAccount.id
          """
        )
          .on('collId -> collection.id.get)
          .as(User.simple *)
      } catch {
        case e: SQLException =>
          Logger.debug("Failed in CollectionMembership.scala / getExceptionsByCollection")
          Logger.debug(e.getMessage())
          List[User]()
      }
    }

  /**
   * Returns the collection membership of a user where the user
   * is enrolled as an exception
   * @return The list of CollectionMembership records for the user
   */
  def getExceptionsByUser(user: User): List[CollectionMembership] =
    DB.withConnection { implicit connection =>
      try {
        SQL (
          s"""
          select * from $tableName where userId = {userID} and exception = true
          """
        )
          .on('userID -> user.id.get)
          .as(simple *)
      } catch {
        case e: SQLException =>
          Logger.debug("Failed in CollectionMembership.scala / getExceptionsByUser")
          Logger.debug(e.getMessage())
          List[CollectionMembership]()
      }
    }
}
