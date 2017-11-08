package models

import anorm._
import anorm.SqlParser._
import java.sql.SQLException
import dataAccess.sqlTraits._
import play.api.Logger
import play.api.db.DB
import play.api.Play.current

/**
 * This represents all the courses found within a collection
 */
case class CollectionCourseLink(id: Option[Long], collectionId: Long, courseId: Long) extends SQLSavable with SQLDeletable {

  /**
   * Saves the content listing to the DB
   * @return The possibly updated content listing
   */
  def save =
    if (id.isDefined) {
      update(CollectionCourseLink.tableName, 'id -> id.get, 'collectionId -> collectionId, 'courseId -> courseId)
      this
    } else {
      val id = insert(CollectionCourseLink.tableName, 'collectionId -> collectionId, 'courseId -> courseId)
      this.copy(id)
    }

  /**
   * Deletes the content listing from the DB
   */
  def delete() {
    delete(CollectionCourseLink.tableName)
  }

}

object CollectionCourseLink extends SQLSelectable[CollectionCourseLink] {
  val tableName = "CollectionCourseLink"

  val simple = {
    get[Option[Long]](tableName + ".id") ~
      get[Long](tableName + ".collectionId") ~
      get[Long](tableName + ".courseId") map {
      case id ~ collectionId ~ courseId => CollectionCourseLink(id, collectionId, courseId)
    }
  }

  /**
   * Search the DB for content listing with the given id.
   * @param id The id of the content listing.
   * @return If a content listing was found, then Some[CollectionCourseLink], otherwise None
   */
  def findById(id: Long): Option[CollectionCourseLink] = findById(id, simple)

  /**
   * Gets all content listing in the DB
   * @return The list of content listing
   */
  def list: List[CollectionCourseLink] = list(simple)

  /**
   * Lists the content listing pertaining to a certain course
   * @param course The course whose content we want
   * @return The list of content listings
   */
  def listByCollection(collection: Collection): List[CollectionCourseLink] =
    listByCol("collectionId", collection.id, simple)

  /**
   * Lists the content listing pertaining to a certain content object
   * @param content The content object the listings will be for
   * @return The list of content listings
   */
  def listByCourse(course: Course): List[CollectionCourseLink] =
    listByCol("courseId", course.id, simple)

  /**
   * Gets all content belonging to a certain course
   * @param course The course where the content is posted
   * @return The list of content
   */
  def listCollectionCourses(collection: Collection): List[Course] =
    DB.withConnection { implicit connection =>
      try {
        SQL(
          s"""
          select * from ${Course.tableName} join $tableName
          on ${Course.tableName}.id = ${tableName}.contentId
          where ${tableName}.courseId = {id}
          """
        )
          .on('id -> collection.id)
          .as(Course.simple *)
      } catch {
        case e: SQLException =>
          Logger.debug("Failed in ContentListing.scala / listClassContent")
          Logger.debug(e.getMessage())
          List[Course]()
      }
    }
}