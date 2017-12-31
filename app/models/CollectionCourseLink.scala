package models

import anorm._
import anorm.SqlParser._
import java.sql.SQLException
import dataAccess.sqlTraits._
import play.api.Logger
import play.api.db.DB
import play.api.Play.current

/**
 * This represents all the courses linked to a collection
 */
case class CollectionCourseLink(id: Option[Long], collectionId: Long, courseId: Long) extends SQLSavable with SQLDeletable {

  /**
   * Saves the CollectionCourseLink to the DB
   * @return The possibly updated CollectionCourseLink
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
   * Deletes the CollectionCourselink record from the DB
   */
  def delete() {
    delete(CollectionCourseLink.tableName)
  }

}

object CollectionCourseLink extends SQLSelectable[CollectionCourseLink] {
  val tableName = "collectionCourseLink"

  val simple = {
    get[Option[Long]](tableName + ".id") ~
      get[Long](tableName + ".collectionId") ~
      get[Long](tableName + ".courseId") map {
      case id ~ collectionId ~ courseId => CollectionCourseLink(id, collectionId, courseId)
    }
  }

  /**
   * Get a CollectionCourseLink by id
   * @param id The id of the CollectionCourseLink
   * @return If the entry was found, then Some[CollectionCourseLink], otherwise None
   */
  def findById(id: Long): Option[CollectionCourseLink] = findById(id, simple)

  /**
   * Gets every CollectionCourseLink in the db
   * @return The list of CollectionCourseLink
   */
  def list: List[CollectionCourseLink] = list(simple)

  /**
   * Gets list of collections that have linked the provided courses
   * @param couses The list of course ids
   * @return a list of collection ids
   */
  def getLinkedCollections(courses: List[Course]): List[CollectionCourseLink] = {
    DB.withConnection { implicit connection =>
      SQL(s"select * from $tableName where courseId in ({courseIds})")
        .on('courseIds -> courses.map(_.id)).as(simple *)
    }
  }

  /**
   * Get all CollectionCourseLinks that contain the given collection
   * @param collection The Collection whose courses we want
   * @return The list of collection course links
   */
  def listByCollection(collection: Collection): List[CollectionCourseLink] =
    listByCol("collectionId", collection.id, simple)

  /**
   * Get CollectionCourseLinks by course
   * @param course The course to search for
   * @return The list of CollectionCourseLinks
   */
  def listByCourse(course: Course): List[CollectionCourseLink] =
    listByCol("courseId", course.id, simple)

  /**
   * Delete CollectionCourseLinks for the given courses in the given collection
   * @param collectionId The id of the collection
   * @param courses the list of course objects to be "unlinked"
   * @return the number of affected rows
   */
  def removeLinks(collectionId: Long, courses: List[Course]): Int = {
    DB.withConnection { implicit connection =>
      SQL(s"delete from ${tableName} where collectionId = {collectionId} and courseId in ({courseIds})")
        .on('collectionId -> collectionId, 'courseIds -> courses.map(_.id.get))
        .executeUpdate()
    }
  }

  /**
   * Gets all courses linked to a collection
   * @param collection The collection where the courses have been linked
   * @return The list of courses
   */
  def listCollectionCourses(collection: Collection): List[Course] =
    DB.withConnection { implicit connection =>
      try {
        SQL(
          s"""
          select * from ${Course.tableName} join $tableName
          on ${Course.tableName}.id = ${tableName}.courseId
          where ${tableName}.collectionId = {id}
          """
        )
          .on('id -> collection.id.getOrElse(0L))
          .as(Course.simple *)
      } catch {
        case e: SQLException =>
          Logger.debug("Failed in ContentListing.scala / listClassContent")
          Logger.debug(e.getMessage())
          List[Course]()
      }
    }
}
