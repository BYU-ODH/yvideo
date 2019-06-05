package controllers

import authentication.Authentication
import authentication.Authentication.result2futureresult
import service._
import models.{User, Content, Collection, ViewingHistory}
import scala.concurrent.{Future, ExecutionContext}
import ExecutionContext.Implicits.global
import service.ContentDescriptor
import dataAccess.{PlayGraph, ResourceController}
import java.net.{URLDecoder, URI, URL}
import play.api.mvc._
import play.api.Logger
import play.api.libs.iteratee.Enumerator
import play.api.libs.json._


/**
 * The controller for dealing with content.
 */
trait ContentController {
  this: Controller =>

  def contentAsJson(id: Long) = Authentication.secureAPIAction() {
    implicit request =>
      implicit user =>
        getContentCollection(id) { (content, collection) =>
          if (content.contentType != 'data) {
            // Check that the user can view the content
            if (collection.userCanViewContent(user) || user.hasSitePermission("admin")) { Ok(content.toJson) }
            else { Forbidden(Json.obj( "message" -> "User is not authorized to view this content." )) }
          }
          else { Errors.api.badRequest("Illegal Content Type.") }
        }
  }

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

  /*
   * Action mix-in to get the content and collection from the request
   */
  def getContentCollection(id: Long)(f: (Content, Collection) => Result)(implicit request: Request[_]) = {
    Content.findById(id).map(con => Collection.findById(con.collectionId).map(col => f(con, col)).getOrElse(Errors.notFound))
      .getOrElse(Errors.notFound)
  }

  /**
   * Content creation page
   */
  def createPage(page: String = "file", collectionId: Long = 0) = Authentication.authenticatedAction() {
    implicit request =>
      implicit user =>
        Authentication.enforceCollectionAdmin(Collection.findById(collectionId)) {
          Future {
            page match {
            case "url" => Ok(views.html.content.create.url(collectionId))
            case "batch" => Ok(views.html.content.create.batchUrl(collectionId))
            case "resource" => Ok(views.html.content.create.resource(collectionId))
            case "playlist" => Ok(views.html.content.create.playlist(collectionId))
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
                                               categories = categories,
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
                    ContentManagement.createContent(info, user, contentType, collectionId)
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
                                             categories = categories,
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
                  ContentManagement.createContent(info, user, contentType, collectionId)
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
   * Class used to parse json body from request to createFromUrl endpoint
   * {
   *  contentType: String,
   *  title: String,
   *  url: String,
   *  description: String,
   *  categories: Array(String), // Optional
   *  keywords: Array(String),   // Optional
   *  languages: Array(String),  // optional
   * }
   */
  case class createUrlData(contentType: String, title: String, url: String, description: String,
    categories: Option[List[String]], keywords: Option[List[String]],
    languages: Option[List[String]])
  implicit val createUrlDataReads = Json.reads[createUrlData]

  /**
   * Parses the createFromUrl request body into a createUrlData object and
   * runs the given function
   */
  private def parseCreateUrl(js: JsValue)(f: createUrlData => Future[Result]): Future[Result] = {
    Json.fromJson[createUrlData](js) match {
      case JsSuccess(data: createUrlData, _) => f(data)
      case e: JsError => Errors.api.badRequest("Invalid format for create content from url.")
    }
  }

  /**
   * Creates content based on the posted data (URL)
   */
  def createFromUrl(collectionId: Long, annotations: Boolean = false) = Authentication.secureAPIAction(parse.json) {
    implicit request =>
      implicit user =>

        Authentication.enforceCollectionAdmin(Collection.findById(collectionId)) {
          parseCreateUrl(request.body.asOpt[JsObject].getOrElse(Json.obj())) { data =>
            // Process the URL if it is not "special"
            val url = if (ResourceHelper.isHTTP(data.url)) processUrl(data.url) else data.url

            if (ResourceHelper.isValidUrl(url)) {
              val mime = ResourceHelper.getMimeFromUri(url)
              Logger.debug(s"Got mime: $mime")

              // Create the content
              ResourceHelper.getUrlSize(url).recover[Long] { case _ =>
                Logger.debug(s"Could not access $url to determine size.")
                0
              }.flatMap { bytes =>
                val info = ContentDescriptor(data.title, data.description, data.keywords.getOrElse(Nil).mkString(","), url, bytes, mime,
                                             categories = data.categories.getOrElse(Nil),
                                             languages = data.languages.getOrElse(Nil))

                if (collectionId > 0) {
                  ContentManagement.createAndAddToCollection(info, user, Symbol(data.contentType), collectionId)
                    .map { _ =>
                      Ok(Json.obj("success" -> "Content created and added to collection"))
                    }
                    .recover { case e: Exception =>
                      val message = e.getMessage()
                      Logger.debug(s"Error creating content in collection $collectionId: $message")
                      Errors.api.serverError(s"Could not add content to collection: $message")
                    }
                } else {
                  ContentManagement.createContentObject(info, user, Symbol(data.contentType), collectionId)
                  .map{ content =>
                    play.Logger.debug(request.queryString.toString)
                    Ok(content.toJson)
                  }
                  .recover { case e: Exception =>
                    val message = e.getMessage()
                    Logger.debug("Error creating content: " + message)
                    Errors.api.serverError(s"Failed to create content: $message")
                  }
                }
              }
            }
            else {
              Future(Errors.api.badRequest("The given url is invalid."))
            }
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
          val keywords = data.get("keywords").map(_.toList).getOrElse(Nil).mkString(",")
          val languages = data.get("languages").map(_.toList).getOrElse(List("eng"))
          lazy val redirect = Redirect(routes.ContentController.createPage("file", collectionId))

          // Upload the file
          request.body.file("file").map { file =>
            FileUploader.normalizeAndUploadFile(file).flatMap { url =>
              // Create the content
              val info = ContentDescriptor(title, description, keywords, url,
                                           file.ref.file.length(), file.contentType.get,
                                           categories = categories,
                                           languages = languages)
              if (collectionId > 0) {
                val redirect = if (!createAndAdd.isEmpty) {
                  Redirect(routes.ContentController.createPage("url", collectionId))
                } else {
                  Redirect(routes.Application.home)
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
                ContentManagement.createContent(info, user, contentType, collectionId)
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
                val content = Content(None, title, Symbol(contentType), collectionId, "", resourceId,
                                      false, // physicalCopyExists
                                      false, // isCopyrighted
                                      true,  // enabled
                                      None,  // dateValidated
                                      "",    // requester
                                      true   // published
                                      ).save
                if (collectionId > 0) {
                  ContentManagement.addToCollection(collectionId, content)
                }
                Ok(content.toJson)
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
                val description = request.body("description")(0)
                val content = Content(None, title, 'playlist, collectionId, "", graphId.toString,
                                      false, // physicalCopyExists
                                      false, // isCopyrighted
                                      true,  // enabled
                                      None,  // dateValidated
                                      "",    // requester
                                      true  // published
                                      ).save
                content.setSetting("description", List(description))

                Redirect(routes.Playlists.about(content.id.get))
              }
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
        Future {
          getContentCollection(id) { (content, collection) =>
            // Check for playlists
            if (content.contentType == 'playlist) {
              Redirect(routes.Playlists.about(id))
            } else if (content.contentType != 'data) {
              // Check that the user can view the content
              if (collection.userCanViewContent(user) || user.hasSitePermission("admin")) Ok(
                views.html.content.view(content, ResourceController.baseUrl, Some(user))
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
   * Content deletion endpoint
   */
  def delete(id: Long) = Authentication.authenticatedAction() {
    implicit request =>
      implicit user =>
        Authentication.enforcePermission("admin") {
          getContent(id) { content =>
            Future {
              // Make sure the user is able to edit
              if (user.hasSitePermission("admin")) {
                content.delete()
                Redirect(routes.Application.home()).flashing("success" -> "Content deleted.")
              } else
                Errors.forbidden
            }
          }
        }
  }

  /**
   * Disable expired content
   */
  def disableExpired = Authentication.authenticatedAction() {
    implicit request =>
      implicit user =>
        Authentication.enforcePermission("admin") {
          Future {
            val contentList = Content.list
            var count = 0
            for (content <- contentList) {
              if (content.dateValidated.getOrElse("") != "" && service.TimeTools.checkExpired(content.dateValidated.get)) {
                Content.expire(content.id.get)
                count += 1
              }
            }
            Redirect(routes.Application.home()).flashing("success" -> s"Successfully expired $count content")
          }
        }
  }

  /**
   * Update content and re-enable
   */
  def renewContent(id: Long, renewer: String) = Authentication.authenticatedAction() {
    implicit request =>
      implicit user =>
        Authentication.enforcePermission("admin") {
          Future {
            Content.renew(id, renewer)
            Redirect(routes.Application.home()).flashing("success" -> s"Successfully renewed content $id")
          }
        }
  }

  /**
   * Increment add a userview to the userView table
   */
  def addView(contentId: Long) = Authentication.secureAPIAction() {
    implicit request =>
      implicit user =>
        getContentCollection(contentId) { (content, collection) =>
          if (content.contentType != 'data) {
            // Check that the user can view the content
            if (collection.userCanViewContent(user) || user.hasSitePermission("admin")) {
              if (ViewingHistory.addView(user.id.get, contentId)) {
                // update this content's view count
                content.copy(views = content.views+1).save
                NoContent
              }
              else {
                TooManyRequest(Json.obj("message" -> "Request repeated too quickly."))
              }
            }
            else { Forbidden(Json.obj( "message" -> "User is not authorized to view this content." )) }
          }
          else { BadRequest(Json.obj( "message" -> "Illegal Content Type." )) }
        }
  }

}

object ContentController extends Controller with ContentController
