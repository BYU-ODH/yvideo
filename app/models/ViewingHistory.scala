package models

import anorm._
import anorm.SqlParser._
import java.sql.SQLException
import dataAccess.sqlTraits._
import play.api.db.DB
import play.api.libs.json.{Json, JsValue, JsNumber}
import play.api.Play.current
import play.api.Logger
import controllers.routes
import service.{EmailTools, TimeTools}

/**
 * User
 * @param id The ID of the user.
 * @param userId ID returned from authentication
 * @param contentId Which authentication scheme this user used
 * @param username The username (often the same as userId)
 * @param name A displayable name for the user
 * @param email The user's email address
 * @param role The permissions of the user
 */
case class ViewingHistory(id: Option[Long], userId: Long, contentId: Long, time: String) extends SQLSavable with SQLDeletable {

  /**
   * Saves the user to the DB
   * @return The possibly updated user
   */
  def save =
    if (id.isDefined) {
      update(ViewingHistory.tableName, 'userId -> userId, 'contentId -> contentId, 'time -> time)
      this
    } else {
      val id = insert(ViewingHistory.tableName, 'userId -> userId, 'contentId -> contentId, 'time -> time)
      this.copy(id)
    }

  /**
   */
  def delete() {
    // Delete this ViewingHistory record
    delete(ViewingHistory.tableName)
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

  def toJson = {
    Json.obj(
      "id" -> id.getOrElse[Long](0),
      "userId" -> userId,
      "contentId" -> contentId,
      "time" -> time
    )
  }

  //       _____      _   _
  //      / ____|    | | | |
  //     | |  __  ___| |_| |_ ___ _ __ ___
  //     | | |_ |/ _ \ __| __/ _ \ '__/ __|
  //     | |__| |  __/ |_| ||  __/ |  \__ \
  //      \_____|\___|\__|\__\___|_|  |___/
  //
  //   ______ ______ ______ ______ ______ ______ ______ ______ ______
  // |______|______|______|______|______|______|______|______|______|
  //

  /**
   * Any items that are retrieved from the DB should be cached here in order to reduce the number of DB calls
   */
  val cacheTarget = this
  object cache {

    var userViews: Option[List[ViewingHistory]] = None

    def getViewingHistory = {
      if (userViews.isEmpty)
        userViews = Some(ViewingHistory.list)
      userViews.get
    }

  }

  /**
   * Gets the enrollment--collections the user is in--of the user
   * @return The list of collections
   */
  def getViewingHistory: List[ViewingHistory] = cache.getViewingHistory

  //       _____      _   _
  //      / ____|    | | | |
  //     | (___   ___| |_| |_ ___ _ __ ___
  //      \___ \ / _ \ __| __/ _ \ '__/ __|
  //      ____) |  __/ |_| ||  __/ |  \__ \
  //     |_____/ \___|\__|\__\___|_|  |___/
  //  ______ ______ ______ ______ ______ ______ ______ ______ ______
  // |______|______|______|______|______|______|______|______|______|
  //

}

object ViewingHistory extends SQLSelectable[ViewingHistory] {
  val tableName = "userView"

  val simple = {
    get[Option[Long]](tableName + ".id") ~
      get[Long](tableName + ".userId") ~
      get[Long](tableName + ".contentId") ~
      get[String](tableName + ".time") map {
      case id ~ userId ~ contentId ~ time => {
        ViewingHistory(id, userId, contentId, time)
      }
    }
  }

  /**
   * Search the DB for a user with the given id.
   * @param id The id of the user.
   * @return If a user was found, then Some[User], otherwise None
   */
  def findById(id: Long): Option[ViewingHistory] = findById(id, simple)

  /**
   * Finds a user based on the username and the contentId.
   * @param contentId The auth scheme to search
   * @param username The username to look for
   * @return If a user was found, then Some[User], otherwise None
   */
  def getUserViews(userId: Long): List[ViewingHistory] = {
    DB.withConnection { implicit connection =>
      try {
        SQL(s"select * from $tableName where userId = {userId}")
          .on('userId -> userId)
          .as(simple*)
      } catch {
        case e: SQLException =>
          Logger.debug("Failed in ViewingHistory.scala / getUserViews")
          Logger.debug(e.getMessage())
          List[ViewingHistory]()
      }
    }
  }

  /**
   * Gets all users in the DB
   * @return The list of users
   */
  def list: List[ViewingHistory] = list(simple)

  /**
   * Gets the number of users in the DB
   * @return the number of total users
   */
  def count: List[Int] = {
    DB.withConnection {
      implicit connection =>
      try {
        SQL(s"select COUNT(id) as c from $tableName")
        .as(get[Int]("c") *)
      } catch {
        case e: SQLException =>
          Logger.debug("Error getting user count. User.scala")
          Nil
      }
    }
  }

  private val view2datetime = (x: ViewingHistory) => TimeTools.stringToDateTime(x.time)

  /**
   * Adds a userView entry if it has been more than 1 minute since the user's previous view
   * Compares the dates in the userView entries
   * @param userId The user
   * @param contentId The content
   * @return true if a userView was created false otherwise
   */
  def addView(userId: Long, contentId: Long): Boolean = {
    if (TimeTools.timeHasElapsed(TimeTools.getLatest(getUserViews(userId).map(view2datetime)), 1000 * 60)) {
      ViewingHistory(None, userId, contentId, TimeTools.now()).save
      true
    }
    else false
  }
}
