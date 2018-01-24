package models

import anorm._
import anorm.SqlParser._
import java.sql.SQLException
import play.api.Logger
import play.api.db.DB
import play.api.Play.current
import java.sql.Connection

object CollectionPermissions {
  val tableName = "collectionPermissions"

  val desc_map = Map(
    "teacher" -> "Collection Admin",
    "ta" -> "TA"
  )

  def permissionList = desc_map.keys.toList
  def descriptionList = desc_map.values.toList
  def descriptionMap = desc_map

  def listByUser(collection: Collection, user: User): List[String] =
    DB.withConnection { implicit connection =>
      try {
        SQL(s"select permission from $tableName where collectionId = {cid} and userId = {uid}")
          .on('cid -> collection.id.get, 'uid -> user.id.get)
          .as(get[String](tableName + ".permission") *)
      } catch {
        case e: SQLException =>
          Logger.debug("Failed in CollectionPermissions.scala / listByUser")
          Logger.debug(e.getMessage())
          List[String]()
      }
    }

  private def permissionExists(collection: Collection, user: User, permission: String)(implicit connection: Connection): Boolean = {
    try {
      val result = SQL(s"select 1 from $tableName where collectionId = {cid} and userId = {uid} and permission = {permission}")
        .on('cid -> collection.id.get, 'uid -> user.id.get, 'permission -> permission)
        .fold(0) { (c, _) => c + 1 } // fold SqlResult
        .fold(_ => 0, c => c) // fold Either
      result > 0
    } catch {
      case e: SQLException =>
        Logger.debug("Failed in CollectionPermissions.scala / permissionExists")
        Logger.debug(e.getMessage())
        false
    }
  }

  def userHasPermission(collection: Collection, user: User, permission: String): Boolean =
    DB.withConnection { implicit connection =>
      permissionExists(collection, user, permission)
    }

  def addUserPermission(collection: Collection, user: User, permission: String) =
    DB.withConnection { implicit connection =>
      if (!permissionExists(collection, user, permission)) {
        try {
          SQL(s"insert into $tableName (collectionId, userId, permission) values ({cid}, {uid}, {permission})")
            .on('cid -> collection.id.get, 'uid -> user.id.get, 'permission -> permission)
            .executeUpdate()
        } catch {
          case e: SQLException =>
            Logger.debug("Failed in CollectionPermissions.scala / addUserPermissions")
            Logger.debug(e.getMessage())
        }
      }
    }

  /**
   * remove a permission from a user
   */
  def removeUserPermission(collection: Collection, user: User, permission: String) = {
    DB.withConnection { implicit connection =>
      try {
        SQL(s"delete from $tableName where collectionId = {cid} and userId = {uid} and permission = {permission}")
          .on('cid -> collection.id.get, 'uid -> user.id.get, 'permission -> permission)
          .executeUpdate()
      } catch {
        case e: SQLException =>
          Logger.debug("Failed in CollectionPermissions.scala / removeUserPermission")
          Logger.debug(e.getMessage())
      }
    }
  }

  /**
   * Removes all the permissions a user has for a collection
   */
  def removeAllUserPermissions(collection: Collection, user: User) = {
    DB.withConnection { implicit connection =>
      try {
        SQL(s"delete from $tableName where collectionId = {cid} and userId = {uid}")
          .on('cid -> collection.id.get, 'uid -> user.id)
          .executeUpdate()
      } catch {
        case e: SQLException =>
          Logger.debug("Failed in CollectionPermissions.scala / removeAllUserPermissions")
          Logger.debug(e.getMessage())
      }
    }
  }

  /**
   * adds the TA permission set to the specified user in the specified collection
   */
  def addTA(collection: Collection, user: User) = {
    val taPermissions = List("ta")
    removeAllUserPermissions(collection, user)
    taPermissions.foreach { role =>
      addUserPermission(collection, user, role)
    }
  }

  def getDescription(permission: String) = desc_map.get(permission).getOrElse("")

}
