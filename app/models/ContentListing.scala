package models

import anorm._
import anorm.SqlParser._
import java.sql.SQLException
import dataAccess.sqlTraits._
import play.api.Logger
import play.api.db.DB
import play.api.Play.current

/**
 * This represents content posted to a collection
 * @param id The id of this ownership
 * @param collectionId The id of the collection
 * @param contentId The id of the content
 */
case class ContentListing(id: Option[Long], collectionId: Long, contentId: Long) extends SQLSavable with SQLDeletable {

  /**
   * Saves the content listing to the DB
   * @return The possibly updated content listing
   */
  def save =
    if (id.isDefined) {
      update(ContentListing.tableName, 'id -> id.get, 'collectionId -> collectionId, 'contentId -> contentId)
      this
    } else {
      val id = insert(ContentListing.tableName, 'collectionId -> collectionId, 'contentId -> contentId)
      this.copy(id)
    }

  /**
   * Deletes the content listing from the DB
   */
  def delete() {
    delete(ContentListing.tableName)
  }

}

object ContentListing extends SQLSelectable[ContentListing] {
  val tableName = "contentListing"

  val simple = {
    get[Option[Long]](tableName + ".id") ~
      get[Long](tableName + ".collectionId") ~
      get[Long](tableName + ".contentId") map {
      case id ~ collectionId ~ contentId => ContentListing(id, collectionId, contentId)
    }
  }

  /**
   * Search the DB for content listing with the given id.
   * @param id The id of the content listing.
   * @return If a content listing was found, then Some[ContentListing], otherwise None
   */
  def findById(id: Long): Option[ContentListing] = findById(id, simple)

  /**
   * Gets all content listing in the DB
   * @return The list of content listing
   */
  def list: List[ContentListing] = list(simple)

  /**
   * Lists the content listing pertaining to a certain collection
   * @param collection The collection whose content we want
   * @return The list of content listings
   */
  def listByCollection(collection: Collection): List[ContentListing] =
    listByCol("collectionId", collection.id, simple)

  /**
   * Lists the content listing pertaining to a certain content object
   * @param content The content object the listings will be for
   * @return The list of content listings
   */
  def listByContent(content: Content): List[ContentListing] =
    listByCol("contentId", content.id, simple)

  /**
   * Gets all content belonging to a certain collection
   * @param collection The collection where the content is posted
   * @return The list of content
   */
  def listClassContent(collection: Collection): List[Content] =
    DB.withConnection { implicit connection =>
      try {
        SQL(
          s"""
          select * from ${Content.tableName} join $tableName
          on ${Content.tableName}.id = ${tableName}.contentId
          where ${tableName}.collectionId = {id}
          """
        )
          .on('id -> collection.id)
          .as(Content.simple *)
      } catch {
        case e: SQLException =>
          Logger.debug("Failed in ContentListing.scala / listClassContent")
          Logger.debug(e.getMessage())
          List[Content]()
      }
    }
}
