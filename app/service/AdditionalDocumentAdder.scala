package service

import models.{Course, User, Content}
import play.api.mvc.{Result, RequestHeader}
import dataAccess.ResourceController
import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global

/**
 * Created with IntelliJ IDEA.
 * User: camman3d
 * Date: 4/29/13
 * Time: 10:32 AM
 * To change this template use File | Settings | File Templates.
 */
object AdditionalDocumentAdder {

  /*
   * Content
   * Additional Document Resource Id
   * Type
   * User
   * Course
   */

  def add(content: Content, resourceId: String, docType: Symbol)(action: Option[Course] => Result)(implicit request: RequestHeader, user: User): Future[Result] = {

    val course = getCourse
    val prefix = getPrefix(content, course)

    // Set the setting on the content
    val settingName = getSettingName(docType, prefix)
    val enabledDocuments = content.settings.get(settingName).map(_.split(",").filterNot(_.isEmpty).toList).getOrElse(Nil)
    content.setSetting(settingName, (resourceId :: enabledDocuments).mkString(",")).save

    // Create the relation
    val attributes = getAttributes(content, course, docType)
    val relationType = getRelationType(docType)
    ResourceController.addRelation("1", resourceId, content.resourceId, relationType, attributes).map(r => {

      // Do something with the result
      action(course)
    })
  }

  private def getRelationType(docType: Symbol): String = {
    if (docType == 'captionTrack)
      "transcriptOf"
    else if (docType == 'annotations)
      "references"
    else
      "unknown"
  }

  private def getAttributes(content: Content, course: Option[Course], docType: Symbol)(implicit user: User): Map[String, String] = {
    val map: Map[String, String] = {
      if (course.isDefined && user.canEdit(course.get))
        Map("owner" -> "course", "ownerId" -> course.get.id.get.toString)
      else if (content.isEditableBy(user))
        Map()
      else
        Map("owner" -> "user", "ownerId" -> user.id.get.toString)
    }
    if (docType == 'annotations)
      map.updated("type", "annotations")
    else
      map
  }

  private def getSettingName(docType: Symbol, prefix: String): String = {
    if (docType == 'captionTrack)
      prefix + "enabledCaptionTracks"
    else if (docType == 'annotations)
      prefix + "enabledAnnotationDocuments"
    else
      "unknown"
  }

  private def getPrefix(content: Content, course: Option[Course])(implicit user: User): String = {
    if (course.isDefined && user.canEdit(course.get))
      "course_" + course.get.id.get + ":"
    else if (content.isEditableBy(user))
      ""
    else
      "user_" + user.id.get + ":"
  }

  private def getCourse()(implicit request: RequestHeader): Option[Course] =
    request.queryString.get("course").flatMap(id => Course.findById(id(0).toLong))

}