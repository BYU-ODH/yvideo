package controllers

import play.api.mvc._
import controllers.authentication.Authentication
import controllers.authentication.Authentication.result2futureresult
import play.api.libs.json._
import dataAccess.ResourceController
import models.{User, Collection, Content}
import service._
import java.net.URL
import java.io.IOException
import javax.imageio.ImageIO
import scala.concurrent.{ExecutionContext, Await, Future}
import scala.concurrent.duration._
import ExecutionContext.Implicits.global
import play.api.Logger
import scala.Some
import scala.util.matching.Regex

/**
 * Controller that deals with the editing of content
 */
trait ContentEditing {
  this: Controller =>

  /**
   * Class used for parsing json requests
   */
  case class ContentMetadata(title: Option[String], description: Option[String], categories: Option[List[String]],
    keywords: Option[List[String]], languages: Option[List[String]], published: Option[Boolean])
  // implicit json conversion for ContentMetadata
  implicit val cmreads = Json.reads[ContentMetadata]

  /**
   * Sets the metadata for a particular content object
   * @param id The ID of the content
   */
  def setMetadata(id: Long) = Authentication.secureAPIAction(parse.json) {
    implicit request =>
      implicit user =>
        ContentController.getContent(id) { content =>
          // Make sure the user is able to edit
          if (content isEditableBy user) {
            // Get the info from the form
            Json.fromJson[ContentMetadata](request.body) match {
              case JsSuccess(metadata, _) => {
                

                // Update the name and published flag of the content
                val title = if (!metadata.title.isEmpty) {
                  content.copy(name = metadata.title.get, published = metadata.published.getOrElse(content.published)).save.name
                } else if (!metadata.published.isEmpty) {
                  content.copy(published = metadata.published.get).save.name
                } else {
                  content.name
                }

                val description = metadata.description.getOrElse("")
                // Validate description
                val validated = if (description.length > 5000) {
                  description.substring(0,5000)
                } else {
                  description
                }

                // Create the JSON object
                val obj = Json.obj(
                  "title" -> title,
                  "description" -> validated,
                  "keywords" -> metadata.keywords.getOrElse[List[String]](Nil).mkString(","),
                  "categories" -> metadata.categories.getOrElse[List[String]](Nil),
                  "languages" -> Json.obj(
                    "iso639_3" -> metadata.languages.getOrElse[List[String]](Nil)
                  )
                )

                // Save the metadata
                ResourceController.updateResource(content.resourceId, obj).map { _ =>
                  Ok(Json.obj("message" -> "Metadata updated."))
                }.recover { case _ =>
                  Errors.api.serverError("Failed to update resource.")
                }
              }
              case e: JsError => Errors.api.badRequest(s"Failed to parse request body.")
            }
          } else Errors.api.forbidden()
        }
  }

  /**
   * Class used for parsing json requests
   */
  case class ContentSettings(captionTracks: Option[List[String]], annotationDocuments: Option[List[String]],
    targetLanguages: Option[List[String]], aspectRatio: Option[String], showCaptions: Option[Boolean],
    showAnnotations: Option[Boolean], allowDefinitions: Option[Boolean], showTranscripts: Option[Boolean],
    showWordList: Option[Boolean])
  // implicit json conversion for ContentSetting
  implicit val csreads = Json.reads[ContentSettings]

  /**
   * Sets the content's settings
   * @param id The ID of the content
   */
  def setSettings(id: Long) = Authentication.secureAPIAction(parse.json) {
    implicit request =>
      implicit user =>
        ContentController.getContent(id) { content =>
          // Make sure the user is able to edit
          if (content isEditableBy user) {
            Json.fromJson[ContentSettings](request.body) match {
              case JsSuccess(settings: ContentSettings, _) => {
                content.setSetting("captionTrack", settings.captionTracks.getOrElse(Nil))
                content.setSetting("annotationDocument", settings.annotationDocuments.getOrElse(Nil))
                content.setSetting("targetLanguages", settings.targetLanguages.getOrElse(Nil))
                content.setSetting("aspectRatio", List(settings.aspectRatio.getOrElse("1.77")))
                content.setSetting("showCaptions", List(settings.showCaptions.getOrElse(false).toString))
                content.setSetting("showAnnotations", List(settings.showAnnotations.getOrElse(false).toString))
                content.setSetting("allowDefinitions", List(settings.allowDefinitions.getOrElse(false).toString))
                content.setSetting("showTranscripts", List(settings.showTranscripts.getOrElse(false).toString))
                content.setSetting("showWordList", List(settings.showWordList.getOrElse(false).toString))
                Ok(Json.obj("message" -> "Content settings updated."))
              }
              case e: JsError => {
                Logger.debug(e.toString)
                Errors.api.badRequest("Incorrectly formatted content settings.")
              }
            }
          }
          else {
            Errors.api.forbidden("User does not have permission to edit this content.")
          }
        }
  }

  /**
   * Image editing view
   * @param id The ID of the content
   */
  def editImage(id: Long) = Authentication.authenticatedAction() {
    implicit request =>
      implicit user =>
        ContentController.getContent(id) {  content =>
          Future {
            if (content.isEditableBy(user) && content.contentType == 'image) {
              val collection = AdditionalDocumentAdder.getCollection()
              Ok(views.html.content.editImage(content, ResourceController.baseUrl, collection))
            } else
              Errors.forbidden
          }
        }
  }

  /**
   * Saves the image edits.
   * @param id The id of the content
   */
  def saveImageEdits(id: Long) = Authentication.authenticatedAction(parse.urlFormEncoded) {
    implicit request =>
      implicit user =>
        ContentController.getContent(id) { content =>
          if ((content isEditableBy user) && (content.contentType == 'image)) {

            // Get the rotation and crop info
            val rotation = request.body("rotation")(0).toInt
            val cropTop = request.body("cropTop")(0).toDouble
            val cropLeft = request.body("cropLeft")(0).toDouble
            val cropBottom = request.body("cropBottom")(0).toDouble
            val cropRight = request.body("cropRight")(0).toDouble

            // Load the image
            ImageTools.loadImageFromContent(content).flatMap { image =>
              // Make the changes to the image
              val newImage = ImageTools.crop(
                if (rotation > 0) ImageTools.rotate(image, rotation) else image,
                cropTop, cropLeft, cropBottom, cropRight
              )

              // Save the new image
              FileUploader.uploadImage(newImage, FileUploader.uniqueFilename(content.resourceId + ".jpg")).flatMap { url =>
                // Update the resource
                ResourceHelper.updateFileUri(content.resourceId, url)
                  .map { _ =>
                    Ok(Json.obj("info" -> "Image updated"))
                  }.recover { case _ =>
                    Errors.api.serverError("Failed to update image")
                  }
              }.recover { case _ =>
                Errors.api.serverError("Failed to update image")
              }
            }.recover { case _ =>
              Errors.api.serverError("Couldn't load image")
            }
          } else
            Future(Errors.api.forbidden())
        }
  }

  /**
   * Sets the thumbnail for content from either a URL or a file
   * @param id The ID of the content that the thumbnail will be for
   */
  def changeThumbnail(id: Long) = Authentication.secureAPIAction(parse.multipartFormData) {
    implicit request =>
      implicit user =>
        ContentController.getContent(id) { content =>

          val file = request.body.file("file")
          val url = request.body.dataParts("url")(0)

          try {
            (if (url.isEmpty) {
              file.map { filepart =>
                ImageTools.generateThumbnail(filepart.ref.file)
              }
            } else {
              Some(ImageTools.generateThumbnail(url))
            }) match {
              case Some(fut) =>
                fut.map { url =>
                  content.copy(thumbnail = url).save
                  Ok(Json.obj("success" -> "Thumbnail changed."))
                }.recover { case _ =>
                  Errors.api.serverError("Unknown error while attempting to create thumbnail.")
                }
              case None => Ok(JsString("No file provided."))
            }
          } catch {
            case _: IOException =>
              Errors.api.serverError("Error reading image file.")
          }
        }
  }

  /**
   * Creates a thumbnail for content from a particular point in time in a video
   * @param id The ID of the content
   * @param time The time in the video which will be used as the thumbnail
   */
  def createThumbnail(id: Long, time: Double) = Authentication.authenticatedAction() {
    implicit request =>
      implicit user =>
        ContentController.getContent(id) { content =>

          // Get the video resource from the content
          ResourceController.getResource(content.resourceId).flatMap { json =>
            // Get the video file
            (json \ "resource" \ "content" \ "files") match {
              case arr:JsDefined =>
                arr.as[JsArray].value.find { file =>
                  (file \ "mime") match {
                    case str:JsDefined => str.as[JsString].value.startsWith("video")
                    case _ => false
                  }
                }.map[Future[Result]] { videoObject =>
                  //Check to see if it's downloadable
                  (videoObject \ "downloadUri") match {
                    case videoUrl:JsDefined =>
                    /*
                    The "-protocols" command will list your version of ffmpeg
                    List of supported Protocols:
                        applehttp, concat, crypto, file, gopher, http, httpproxy
                        mmsh, mmst, pipe, rtmp, rtp, tcp, udp
                    The default protocol is "file:" and you do not need to specify it in ffmpeg,
                    so we can't check to see if we are using a supported protocol. However,
                    we do know that "https:" is unsupported, so if we get one, try to convert it
                    to "http:". If it doesn't work, we'll just get a message that the thumbnail
                    could not be generated.
                    */
                     // val url = if (videoUrl.as[JsString].value.startsWith("https://"))
                     //        JsString(videoUrl.value.replaceFirst("https://","http://"))
                     //    else
                     //        videoUrl

                      // Generate the thumbnail for that video
                      VideoTools.generateThumbnail(videoUrl.as[JsString].value, time)
                        .map { url =>
                          // Save it and be done
                          content.copy(thumbnail = url).save
                          Ok(Json.obj("success" -> "Thumbnail updated."))
                        }.recover { case e: Exception =>
                          Errors.api.serverError(e.getMessage())
                        }
                    case _ => {
                      (videoObject \ "mime") match {
                        case videoType:JsDefined =>
                          if (videoType.as[JsString].value == "video/x-youtube") {
                            val youtubeRegex = new Regex("(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|watch\\?v%3D|%2Fvideos%2F|embed%2F|youtu.be%2F|%2Fv%2F)[^#\\&\\?\n]*")
                            (videoObject \ "streamUri") match {
                              case youtubeUri:JsDefined => {
                                val stringUri = youtubeUri.value.toString
                                val videoId = youtubeRegex findFirstIn stringUri
                                // Sometimes an extra character gets added on to the video ID so slice off everything but the first 11 chars
                                val thumbnailUrl = "https://img.youtube.com/vi/" + videoId.get.toString.slice(0, 11) + "/default.jpg"
                                content.copy(thumbnail = thumbnailUrl.toString).save
                                Ok(Json.obj("success" -> "Thumbnail updated."))
                              }
                              case _ => Ok(Json.obj("error" -> "Error getting youtube URL"))
                            }
                          } else {
                            Ok(Json.obj("error" -> "Sorry. We can only get youtube thumbnails for now."))
                          }
                        case _ => Ok(Json.obj("error" -> "Error getting video type"))
                      }
                    }
                  }
                }.getOrElse[Future[Result]] {
                  Errors.api.notFound("No video file found.")
                }
              case _ => Errors.api.notFound("No files found")
            }
          }.recover { case _ =>
            Errors.api.serverError("Could not access video.")
          }
        }
  }

  /**
   * Sets the downloadUri of the primary resource associated with the content
   * @param id The ID of the content
   */
  def setMediaSource(id: Long) = Authentication.authenticatedAction(parse.urlFormEncoded) {
    implicit request =>
      implicit user =>
        ContentController.getContent(id) { content =>
          if (content isEditableBy user) {
            val url = request.body("url")(0)
            ResourceHelper.updateFileUri(content.resourceId, url).map { _ =>
              Ok(Json.obj("message" -> "Media source updated."))
            }.recover { case _ =>
              Ok(Json.obj("error" -> "Oops! Something went wrong!"))
            }
          } else Errors.api.forbidden()
        }
  }

}

object ContentEditing extends Controller with ContentEditing
