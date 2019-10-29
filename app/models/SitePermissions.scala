package models

import anorm._
import anorm.SqlParser._
import java.sql.SQLException
import dataAccess.sqlTraits.SQLSelectable
import play.api.Logger
import play.api.db.DB
import play.api.Play.current
import java.sql.Connection
import scala.language.postfixOps

object SitePermissions extends SQLSelectable[String] {
  val tableName = "sitePermissions"

  val desc_map = Map(
    "admin" -> "Administrator",
    "manager" -> "Site Manager",
    "createCollection" -> "Create Collection",
    "createContent" -> "Create Content",
    "viewRestricted" -> "View Restricted Content",
    "joinCollection" -> "Join Collections",
    "enableContent" -> "Enable and Disable Content",
    "delete" -> "Delete site data"
  )

  def permissionList = desc_map.keys.toList
  def descriptionList = desc_map.values.toList
  def descriptionMap = desc_map

  def listByUser(user: User): List[String] =
    listByCol("userId", user.id, get[String](tableName+".permission"))

  /**
   * Gets a list of users from the database
   * that have the given permission
   */
  def listByPerm(perm: String): List[User] =
    User.findUsersByUserIdList(listByCol[Long]("permission", perm, get[Long](tableName+".userId")))

  private def permissionExists(user: User, permission: String)(implicit connection: Connection): Boolean = {
    try {
      val result = SQL(s"select 1 from $tableName where userId = {uid} and permission = {permission}")
        .on('uid -> user.id, 'permission -> permission)
        .fold(0) { (c, _) => c + 1 } // fold SqlResult
        .fold(_ => 0, c => c) // fold Either
      result > 0
    } catch {
      case e: SQLException =>
        Logger.debug("Failed in SitePermissions.scala / permissionExists")
        Logger.debug(e.getMessage())
        false
    }
  }

  def userHasPermission(user: User, permission: String): Boolean =
    DB.withConnection { implicit connection =>
      permissionExists(user, permission)
    }

  def addUserPermission(user: User, permission: String) {
    DB.withConnection { implicit connection =>
      if (!permissionExists(user, permission)) {
        try {
          SQL(s"insert into $tableName (userId,permission) values ({uid},{permission})")
            .on('uid -> user.id, 'permission -> permission)
			.executeUpdate()
        } catch {
          case e: SQLException =>
            Logger.debug("Failed in SitePermissions.scala / addUserPermission")
            Logger.debug(e.getMessage())
        }
      }
    }
  }

  /**
   * deletes the permission from sitePermissions if it exists
   * @param user User whose permission is to be removed
   * @param permission String name of the permission to search and delete
   */
  def removeUserPermission(user: User, permission: String) {
    DB.withConnection { implicit connection =>
      try {
        SQL(s"delete from $tableName where userId = {uid} and permission = {permission}")
          .on('uid -> user.id.get, 'permission -> permission).executeUpdate()
      } catch {
        case e: SQLException =>
          Logger.debug("Failed in SitePermissions.scala / removeUserPermission")
          Logger.debug(e.getMessage())
      }
    }
  }

  /**
   * removes all permissions for a user
   * @param user User whose permissions are to be removed
   */
  def removeAllUserPermissions(user: User) {
    DB.withConnection { implicit connection =>
      try {
        SQL(s"delete from $tableName where userId = {uid}")
          .on('uid -> user.id.get).executeUpdate()
      } catch {
        case e: SQLException =>
          Logger.debug("Failed in SitePermissions.scala / removeAllUserPermissions")
          Logger.debug(e.getMessage())
      }
    }
  }

  def getDescription(permission: String) = desc_map.get(permission).getOrElse("")

  val roles = Map(
    'student -> List("joinCollection"),
    'teacher -> List("createContent", "joinCollection", "createCollection", "viewRestricted"),
    'manager -> List("createContent", "joinCollection", "createCollection", "viewRestricted", "enableContent", "manager"),
    'admin -> List("admin", "delete")
  )

  def assignRole(user: User, role: Symbol) {
    roles(role).foreach { p =>
      addUserPermission(user, p)
    }
  }

  def permissionsToRoles(perms: List[String]): List[String] = {
    {if (roles.get('teacher).get.forall(perms.contains)) Some("teacher") else None} ::
    {if (roles.get('student).get.forall(perms.contains)) Some("student") else  None} ::
    {if (roles.get('manager).get.forall(perms.contains)) Some("manager") else  None} ::
    {if (roles.get('admin).get.forall(perms.contains)) Some("admin") else None} :: Nil flatten
  }

}
