package service

import concurrent.{ExecutionContext, Future}
import controllers.authentication.Authentication
import controllers._
import models.{User, Content, Collection}
import javax.imageio.ImageIO
import java.io.File
import java.net.URL
import ExecutionContext.Implicits.global
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import play.api.mvc.Results._

case class ContentDescriptor(title: String, description: String, keywords: String, url: String, bytes: Long,
                             mime: String, thumbnail: Option[String] = None,
                             categories: List[String] = Nil, languages: List[String] = Nil)

/**
 * This service helps with the creation and management of content objects and their corresponding resources.
 */
object ContentManagement {

  /**
   *  This function is used to get the collection
   *  without an implicit request
   */
  private def getCollection(id: Long)(f: Collection => Result): Result = {
    Collection.findById(id).map(f).getOrElse(Errors.notFound)
  }

  /**
   * Add newly created content to collection
   * @param collectionId
   * @param content Content object
   */
  def addToCollection(collectionId: Long, content: Content): Unit = {
    getCollection(collectionId) { collection =>
      collection.addContent(content)
      // how to check if content was correctly added to the collection?
      Ok("")
    }
  }

  /**
   * Creates content depending on the content type and adds it to the specified collection
   * @param info A ContentDescriptor which contains information about the content
   * @param owner The user who is to own the content
   * @param contentType The type of content
   * @param collectionId Id of target collection
   * @return The content object in a future
   */
  def createAndAddToCollection(info: ContentDescriptor, owner: User, contentType: Symbol, collectionId: Long): Future[Content] = {
    createContentObject(info, owner, contentType, collectionId).map { content =>
      addToCollection(collectionId, content)
      content
    }
  }


  /**
   * Creates content depending on the content type
   * @param info A ContentDescriptor which contains information about the content
   * @param owner The user who is to own the content
   * @param contentType The type of content
   * @return The content id in a future
   */
  def createContent(info: ContentDescriptor, owner: User, contentType: Symbol, collectionId: Long): Future[Content] = {
      createContentObject(info, owner, contentType, collectionId)
  }

  /**
   * Creates content depending on the content type
   * @param info A ContentDescriptor which contains information about the content
   * @param owner The user who is to own the content
   * @param contentType The type of content
   * @return The content object in a future
   */
  def createContentObject(info: ContentDescriptor, owner: User, contentType: Symbol, collectionId: Long): Future[Content] = {
    contentType match {
      case 'audio =>
        createAudio(info, owner, collectionId)
      case 'image =>
        // Create a thumbnail
        ImageTools.generateThumbnail(info.url).flatMap { url =>
          val imageInfo = info.copy(thumbnail = Some(url))
          createImage(imageInfo, owner, collectionId)
        }.recoverWith { case _ =>
          createImage(info, owner, collectionId)
        }
      case 'video =>
        // Create a thumbnail
        VideoTools.generateThumbnail(info.url).flatMap { url =>
          val videoInfo = info.copy(thumbnail = Some(url))
          createVideo(videoInfo, owner, collectionId)
        }.recoverWith { case _ =>
          createVideo(info, owner, collectionId)
        }
      case 'text =>
        createText(info, owner, collectionId)
      case _ =>
        Future.failed(new Exception(s"Unrecognized Content Type: $contentType"))
    }
  }

  def createResource(info: ContentDescriptor, resourceType: String, user: User): Future[JsValue] = {
    val resource = ResourceHelper.make.resource(Json.obj(
      "title" -> info.title,
      "description" -> info.description,
      "categories" -> info.categories,
      "keywords" -> info.keywords,
      "type" -> resourceType,
      "languages" -> Json.obj(
        "iso639_3" -> info.languages
      )
    ))
    ResourceHelper.createResourceWithUri(resource, user, info.url, info.bytes, info.mime)
  }

  /**
   * Create a video content object with a corresponding resource object from information.
   * @param info A ContentDescriptor which contains information about the content
   * @param owner The user who is to own the video
   * @return The content object in a future
   */
  def createVideo(info: ContentDescriptor, owner: User, collectionId: Long): Future[Content] = {
    // Create the resource
    createResource(info, "video", owner).map { json =>
      val resourceId = (json \ "id").as[String]

      // Set a thumbnail in the resource
      if (info.thumbnail.isDefined && !info.thumbnail.get.isEmpty)
        ResourceHelper.addThumbnail(resourceId, info.thumbnail.get)

      // Create the content and set the user and the owner
      Content(None, info.title, 'video, collectionId, info.thumbnail.getOrElse(""), resourceId,
              false, // physicalCopyExists
              false, // isCopyrighted
              true,  // enabled
              None,  // dateValidated
              "",    // requester
              true  // published
              ).save
    }
  }

  /**
   * Creates an audio content object with a corresponding resource object from information
   * @param info A ContentDescriptor which contains information about the content
   * @param owner The user who is to own the audio
   * @return The content object in a future
   */
  def createAudio(info: ContentDescriptor, owner: User, collectionId: Long): Future[Content] = {
    // Create the resource
    createResource(info, "audio", owner).map { json =>
      val resourceId = (json \ "id").as[String]

      // Create the content and set the user and the owner
      Content(None, info.title, 'audio, collectionId, info.thumbnail.getOrElse(""), resourceId,
              false, // physicalCopyExists
              false, // isCopyrighted
              true,  // enabled
              None,  // dateValidated
              "",    // requester
              true  // published
              ).save
    }
  }

  /**
   * Creates a text content object with a corresponding resource object from information
   * @param info A ContentDescriptor which contains information about the content
   * @param owner The user who is to own the audio
   * @return The content object in a future
   */
  def createText(info: ContentDescriptor, owner: User, collectionId: Long): Future[Content] = {
    // Create the resource
    createResource(info, "document", owner).map { json =>
      val resourceId = (json \ "id").as[String]

      // Create the content and set the user and the owner
      Content(None, info.title, 'text, collectionId, info.thumbnail.getOrElse(""), resourceId,
              false, // physicalCopyExists
              false, // isCopyrighted
              true,  // enabled
              None,  // dateValidated
              "",    // requester
              true  // published
              ).save
    }
  }

  /**
   * Creates an image content object with a corresponding resource object from information
   * @param info A ContentDescriptor which contains information about the content
   * @param owner The user who is to own the image
   * @return The content object in a future
   */
  def createImage(info: ContentDescriptor, owner: User, collectionId: Long): Future[Content] = {
    // Create the resource
    createResource(info, "image", owner).map { json =>
      val resourceId = (json \ "id").as[String]

      // Set a thumbnail in the resource
      info.thumbnail.foreach( thumbnail =>
        ResourceHelper.addThumbnail(resourceId, thumbnail)
      )

      // Create the content and set the user and the owner
      Content(None, info.title, 'image, collectionId, info.thumbnail.getOrElse(""), resourceId,
              false, // physicalCopyExists
              false, // isCopyrighted
              true,  // enabled
              None,  // dateValidated
              "",    // requester
              true  // published
              ).save
    }
  }

}
