package controllers

import authentication.Authentication
import service._
import models.{User, Content}
import scala.concurrent.{Future, ExecutionContext}
import ExecutionContext.Implicits.global
import service.ContentDescriptor
import dataAccess.{GoogleFormScripts, PlayGraph, ResourceController}
import java.net.{URLDecoder, URI, URL}
import play.api.mvc._
import play.api.Logger
import play.api.libs.ws.WS
import play.api.libs.iteratee.Enumerator
import play.api.libs.json.Json
import java.text.SimpleDateFormat
import java.util.Calendar


/**
 * The controller for dealing with content.
 */
object ContentController extends Controller {

  /**
   * Store content id created in annotation editor in the browser.
   */
  val createContentFromAnnotationEditorResponse = (contentJson: String) => s"""
  | <script>
  | opener.contentReceiver($contentJson);
  | window.close();
  | </script>
  """.stripMargin

  /**
   * Action mix-in to get the content from the request
   */
  def getContent(id: Long)(f: Content => Future[Result])(implicit request: Request[_]) = {
    Content.findById(id).map(f)
      .getOrElse(Future(Errors.notFound))
  }

  /**
   * Returns a content object as JSON
   * @param id the ID of the content
   */
  def getAsJson(id: Long) = Authentication.authenticatedAction() {
    implicit request =>
      implicit user =>
        getContent(id) { content =>

          // A user can get the JSON if he can see the content
          Future {
            val authKey = request.queryString.get("authKey").getOrElse("")
            if (content.isVisibleBy(user))
              Ok(content.toJson)
            else
              Forbidden
          }
        }
  }

  /**
   * Content creation page
   */
  def createPage(page: String = "file", collectionId: Long = 0) = Authentication.authenticatedAction() {
    implicit request =>
      implicit user =>
        Authentication.enforcePermission("createContent") {
          Future {
            page match {
            case "url" => Ok(views.html.content.create.url(collectionId))
            case "batch" => Ok(views.html.content.create.batchUrl(collectionId))
            case "resource" => Ok(views.html.content.create.resource(collectionId))
            case "playlist" => Ok(views.html.content.create.playlist(collectionId))
            case "questions" => Ok(views.html.content.create.questionSet(collectionId))
            case _ => Ok(views.html.content.create.file(collectionId))
            }
          }
        }
  }

  /**
   * Takes a URL and processes it, encoding it if necessary while preserving the query string
   * @param url The URL to process
   * @return The processed URL
   */
  def processUrl(url: String): String = {

    // Check to see if we need to encode (we will if the decoded is the same as the encoded)
    if (URLDecoder.decode(url, "utf-8") == url) {
      val urlObj = new URL(url)
      val queryString = if (url.contains("?")) url.substring(url.indexOf("?")) else ""
      new URI(urlObj.getProtocol, urlObj.getHost, urlObj.getPath, null).toString + queryString
    } else
      url
  }

  /**
   * Creates content based on the posted data (URL)
   */
  def createFromBatch(collectionId: Long) = Authentication.authenticatedAction(parse.multipartFormData) {
    implicit request =>
      implicit user =>

        Authentication.enforcePermission("createContent") {

          // Collect the information
          val data = request.body.dataParts
          val contentType = Symbol(data("contentType")(0))
          val title = data("title")(0)
          val description = data("description")(0)
          val categories = data.get("categories").map(_.toList).getOrElse(Nil)
          val labels = data.get("labels").map(_.toList).getOrElse(Nil)
          val keywords = data.get("keywords").map(_.toList).getOrElse(Nil).mkString(",")
          val languages = data.get("languages").map(_.toList).getOrElse(List("eng"))


          if (request.body.file("file").isDefined)
          {
              // Upload the file
              request.body.file("file").map { file =>
                FileUploader.normalizeAndUploadFile(file).flatMap { url =>
                  // Create the content
                  val info = ContentDescriptor(title, description, keywords, url,
                                               file.ref.file.length(), file.contentType.get,
                                               labels = labels, categories = categories,
                                               languages = languages)
                  if (collectionId > 0) {
                    ContentManagement.createAndAddToCollection(info, user, contentType, collectionId)
                      .map { cid => Ok(Json.obj("contentId" -> cid)) }
                      .recover { case e: Exception =>
                        val message = e.getMessage()
                        Logger.debug(s"Error creating content in collection $collectionId: $message")
                        InternalServerError(Json.obj("message" -> s"Could not add content to collection: $message"))
                      }
                  } else {
                    ContentManagement.createContent(info, user, contentType)
                    .map { cid => Ok(Json.obj("contentId" -> cid)) }
                    .recover { case e: Exception =>
                      val message = e.getMessage()
                      Logger.debug(s"Error creating content $collectionId: $message")
                      InternalServerError(Json.obj("message" -> s"Could not add content: $message"))
                    }
                  }
                }.recover { case e =>
                  val message = e.getMessage()
                  Logger.debug(s"Failed to upload FILE: $message")
                  InternalServerError(Json.obj("message" -> s"Failed to upload FILE: $message"))
                }
              }.getOrElse {
                Future(BadRequest(Json.obj("message" -> "The FILE is missing.")))
              }
          }


        else{
          // Get the URL and MIME. Process the URL if it is not "special"
            val raw_url = data("url")(0)
            val url = if (ResourceHelper.isHTTP(raw_url)) processUrl(raw_url) else raw_url

            if (ResourceHelper.isValidUrl(url)) {
              val mime = ResourceHelper.getMimeFromUri(url)
              Logger.debug(s"Got mime: $mime")

              // Create the content
              ResourceHelper.getUrlSize(url).recover[Long] { case _ =>
                Logger.debug(s"Could not access $url to determine size.")
                0
              }.flatMap { bytes =>
                val info = ContentDescriptor(title, description, keywords, url, bytes, mime,
                                             labels = labels, categories = categories,
                                             languages = languages)

                // find alternate create content ↓ through annotations method
                if (collectionId > 0 && collectionId != 40747105) {
                  ContentManagement.createAndAddToCollection(info, user, contentType, collectionId)
                    .map { cid => Ok(Json.obj("contentId" -> cid)) }
                    .recover { case e: Exception =>
                      val message = e.getMessage()
                      Logger.debug(s"Error creating content in collection $collectionId: $message")
                      InternalServerError(Json.obj("message" -> s"Could not add content to collection: $message"))
                    }
                } else {
                  ContentManagement.createContent(info, user, contentType)
                  .map { cid => Ok(Json.obj("contentId" -> cid)) }
                    .recover { case e: Exception =>
                      val message = e.getMessage()
                      Logger.debug(s"Error creating content: $message")
                      InternalServerError(Json.obj("message" -> s"Could not add content: $message"))
                    }
                }
              }
            } else
              Future{
                BadRequest(Json.obj("message" -> "The given URL is invalid."))
              }
        }
      }
  }


  /**
   * Creates content based on the posted data (URL)
   */
  def createFromUrl(collectionId: Long, annotations: Boolean = false) = Authentication.authenticatedAction(parse.multipartFormData) {
    implicit request =>
      implicit user =>

        Authentication.enforcePermission("createContent") {

          // Collect the information
          val data = request.body.dataParts
          val contentType = Symbol(data("contentType")(0))
          val title = data("title")(0)
          val description = data("description")(0)
          val categories = data.get("categories").map(_.toList).getOrElse(Nil)
          val createAndAdd = data.getOrElse("createAndAdd", Nil)
          val labels = data.get("labels").map(_.toList).getOrElse(Nil)
          val keywords = data.get("keywords").map(_.toList).getOrElse(Nil).mkString(",")
          val languages = data.get("languages").map(_.toList).getOrElse(List("eng"))

          // Get the URL and MIME. Process the URL if it is not "special"
          val raw_url = data("url")(0)
          val url = if (ResourceHelper.isHTTP(raw_url)) processUrl(raw_url) else raw_url

          if (ResourceHelper.isValidUrl(url)) {
            val mime = ResourceHelper.getMimeFromUri(url)
            Logger.debug(s"Got mime: $mime")

            // Create the content
            ResourceHelper.getUrlSize(url).recover[Long] { case _ =>
              Logger.debug(s"Could not access $url to determine size.")
              0
            }.flatMap { bytes =>
              val info = ContentDescriptor(title, description, keywords, url, bytes, mime,
                                           labels = labels, categories = categories,
                                           languages = languages)

              if (collectionId > 0) {
                val redirect = if (!createAndAdd.isEmpty) {
                  Redirect(routes.ContentController.createPage("url", collectionId))
                } else {
                  Redirect(routes.Collections.view(collectionId))
                }
                ContentManagement.createAndAddToCollection(info, user, contentType, collectionId)
                  .map { _ =>
                    redirect.flashing("success" -> "Content created and added to collection")
                  }
                  .recover { case e: Exception =>
                    val message = e.getMessage()
                    Logger.debug(s"Error creating content in collection $collectionId: $message")
                    redirect.flashing("error" -> s"Could not add content to collection: $message")
                  }
              } else {
                //check if we came from the annotation editor
                val createFromAnnotations: Boolean = request.queryString.getOrElse("annotations", Nil).contains("true")

                ContentManagement.createContentObject(info, user, contentType)
                .map{ content =>
                  if (!createAndAdd.isEmpty) {
                    if (createFromAnnotations) {
                      Ok(views.html.content.create.url(collectionId))
                        .flashing("success" -> "Content Created")
                    } else {
                      Redirect(routes.ContentController.createPage("url", collectionId))
                        .flashing("success" -> "Content Created")
                    }
                  } else {
                    play.Logger.debug(request.queryString.toString)
                    if (createFromAnnotations) {
                      Ok(createContentFromAnnotationEditorResponse(content.toJson.toString)).as(HTML)
                    } else {
                      Redirect(routes.ContentController.view(content.id.get))
                    }
                  }
                }
                .recover { case e: Exception =>
                  val message = e.getMessage()
                  Logger.debug("Error creating content: " + message)
                  Redirect(routes.ContentController.createPage("url", collectionId))
                    .flashing("error" -> s"Failed to create content: $message")
                }
              }
            }
          } else
            Future{
              Redirect(routes.ContentController.createPage("url", collectionId))
                .flashing("error" -> "The given URL is invalid.")
            }
        }
  }

  /**
   * Creates content based on the posted data (File)
   */
  def createFromFile(collectionId: Long) = Authentication.authenticatedAction(parse.multipartFormData) {
    implicit request =>
      implicit user =>

        Authentication.enforcePermission("createContent") {

          // Collect the information
          val data = request.body.dataParts
          val contentType = Symbol(data("contentType")(0))
          val title = data("title")(0)
          val description = data("description")(0)
          val categories = data.get("categories").map(_.toList).getOrElse(Nil)
          val createAndAdd = data.getOrElse("createAndAdd", Nil)
          val labels = data.get("labels").map(_.toList).getOrElse(Nil)
          val keywords = data.get("keywords").map(_.toList).getOrElse(Nil).mkString(",")
          val languages = data.get("languages").map(_.toList).getOrElse(List("eng"))
          lazy val redirect = Redirect(routes.ContentController.createPage("file", collectionId))

          // Upload the file
          request.body.file("file").map { file =>
            FileUploader.normalizeAndUploadFile(file).flatMap { url =>
              // Create the content
              val info = ContentDescriptor(title, description, keywords, url,
                                           file.ref.file.length(), file.contentType.get,
                                           labels = labels, categories = categories,
                                           languages = languages)
              if (collectionId > 0) {
                val redirect = if (!createAndAdd.isEmpty) {
                  Redirect(routes.ContentController.createPage("url", collectionId))
                } else {
                  Redirect(routes.Collections.view(collectionId))
                }
                ContentManagement.createAndAddToCollection(info, user, contentType, collectionId)
                  .map { _ =>
                    redirect.flashing("success" -> "Content created and added to collection")
                  }
                  .recover { case e: Exception =>
                    val message = e.getMessage()
                    Logger.debug(s"Error creating content in collection $collectionId: $message")
                    redirect.flashing("error" -> s"Could not add content to collection: $message")
                  }
              } else {
                ContentManagement.createContent(info, user, contentType)
                .map { _ =>
                    redirect.flashing("success" -> "Content created")
                  }
                  .recover { case e: Exception =>
                    val message = e.getMessage()
                    Logger.debug(s"Error creating content: $message")
                    redirect.flashing("error" -> s"Could not add content: $message")
                  }
              }
            }.recover { case _ =>
              redirect.flashing("error" -> "Failed to upload file")
            }
          }.getOrElse {
            Future(redirect.flashing("error" -> "Missing file"))
          }
        }
  }

  /**
   * Creates content based on the posted data (File)
   */
  def createFromResource(collectionId: Long) = Authentication.authenticatedAction(parse.urlFormEncoded) {
    implicit request =>
      implicit user =>

        Authentication.enforcePermission("createContent") {

          // Create from resource
          val resourceId = request.body("resourceId")(0)
          val createAndAdd = request.body.getOrElse("createAndAdd", Nil)

          ResourceController.getResource(resourceId).map { json =>
            val code = (json \ "response" \ "code").as[Int]
            if (code == 200) {
              val title = (json \ "resource" \ "title").as[String]
              val resourceType = (json \ "resource" \ "type").as[String]

              if (resourceType == "data" || resourceType == "archive") {
                Redirect(routes.ContentController.createPage("resource", collectionId))
                .flashing("error" -> "Can't create content from a data or archive resources.")
              } else {
                //TODO: properly handle collections
                //TODO: update our code to match the resource library, rather than special-casing "text"
                val contentType = if(resourceType == "document") "text" else resourceType
                val content = Content(None, title, Symbol(contentType), "", resourceId).save
                user.addContent(content)
                if (collectionId > 0) {
                  ContentManagement.addToCollection(collectionId, content)
                }
                if (createAndAdd.isEmpty) {
                  Redirect(routes.ContentController.view(content.id.get))
                    .flashing("success" -> "Content Added.")
                } else {
                  Redirect(routes.ContentController.createPage("resource", collectionId))
                    .flashing("success" -> "Content Created")
                }
              }
            } else
              Redirect(routes.ContentController.createPage("resource", collectionId))
                .flashing("error" -> "That resource doesn't exist")
          }.recover { case e =>
            Logger.debug("Couldn't access resource: " + e.getMessage())
            Redirect(routes.ContentController.createPage("resource", collectionId))
              .flashing("error" -> "Couldn't access resource")
          }
        }
  }

  /**
   * Creates content based on the posted data (File)
   */
  def createPlaylist(collectionId: Long) = Authentication.authenticatedAction(parse.urlFormEncoded) {
    implicit request =>
      implicit user =>

        Authentication.enforcePermission("createContent") {

          // Create the node content
          PlayGraph.Author.NodeContent.create("").flatMap { nodeContentJson =>
            val nodeContentId = (nodeContentJson \ "nodeContent" \ "id").as[Long]

            // Create the node
            PlayGraph.Author.Node.create(nodeContentId, "data").flatMap { nodeJson =>
              val nodeId = (nodeJson \ "node" \ "id").as[Long]

              // Create the graph
              PlayGraph.Author.Graph.create(nodeId).map { graphJson =>
                val graphId = (graphJson \ "graph" \ "id").as[Long]

                // Create playlist
                val title = request.body("title")(0)
                val labels = request.body.get("labels").map(_.toList).getOrElse(Nil)
                val description = request.body("description")(0)
                val content = Content(None, title, 'playlist, "", graphId.toString, labels = labels).save
                content.setSetting("description", List(description))
                user.addContent(content)

                Redirect(routes.Playlists.about(content.id.get))
              }
            }
          }
        }
  }

  /**
   * Creates content based on the posted data (File)
   */
  def createQuestionSet(collectionId: Long) = Authentication.authenticatedAction(parse.urlFormEncoded) {
    implicit request =>
      implicit user =>

        Authentication.enforcePermission("createContent") {

          val title = request.body("title")(0)
          val labels = request.body.get("labels").map(_.toList).getOrElse(Nil)
          val description = request.body("description")(0)

          GoogleFormScripts.createForm(title, user.email.get).map { formId =>
            val content = Content(None, title, 'questions, "", formId, labels = labels).save
            content.setSetting("description", List(description))
            user.addContent(content)

            Redirect(routes.QuestionSets.about(content.id.get))
          }
        }
  }

  /**
   * Creates a copy of an existing content object
   */
  def cloneContent(id: Long) = Authentication.authenticatedAction() {
    implicit request =>
      implicit user =>
        Authentication.enforcePermission("createContent") {
          Future {
            Content.findById(id) match {
            case Some(content) =>
              val copied = content.copy(id = None).save
              user.addContent(copied)
              Redirect(routes.ContentController.view(copied.id.get))
                .flashing("success" -> "Content Cloned")
            case None =>
              Redirect(routes.ContentController.mine())
                .flashing("error" -> "No Such Content")
            }
          }
        }
  }

  /**
   * Content view page
   */
  def view(id: Long) = Authentication.authenticatedAction() {
    implicit request =>
      implicit user =>
        getContent(id) { content =>
          Future {
            // Check for playlists
            if (content.contentType == 'playlist) {
              Redirect(routes.Playlists.about(id))
            } else if (content.contentType == 'questions) {
              Redirect(routes.QuestionSets.about(id))
            } else if (content.contentType != 'data) {
              //TODO: make this a whitelist instead of blacklist
              // Check that the user can view the content
              if (content isVisibleBy user) Ok(
                /*if (MobileDetection.isMobile()) {
                  views.html.content.viewMobile(content, ResourceController.baseUrl, Some(user))
                } else {*/
              views.html.content.view(content, ResourceController.baseUrl, Some(user))
                /*}*/
              ) else
                Redirect(routes.Application.home)
                  .flashing("error" -> "You do not have permission to view the requested content.")
            } else {
              Redirect(routes.Application.home)
                .flashing("error" -> "Requested content uses invalid resource")
            }
          }
        }
  }

  /**
   * Content management page
   */
  def manageContent() = Authentication.authenticatedAction() {
    implicit request =>
      implicit user =>
        Future(Ok(views.html.content.batchEdit(user.getContent)))
  }

  /**
   * Content deletion endpoint
   */
  def delete(id: Long) = Authentication.authenticatedAction() {
    implicit request =>
      implicit user =>
        getContent(id) { content =>
          Future {
            // Make sure the user is able to edit
            if (content isEditableBy user) {
              content.delete()
              Redirect(routes.ContentController.mine()).flashing("success" -> "Content deleted.")
            } else
              Errors.forbidden
          }
        }
  }

  /**
   * "My Content" page
   */
  def mine = Authentication.authenticatedAction() {
    implicit request =>
      implicit user =>
        Future(Ok(views.html.content.mine()))
  }
}
