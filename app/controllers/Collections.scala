package controllers

import scala.concurrent._
import ExecutionContext.Implicits.global
import authentication.Authentication
import play.api.mvc._
import play.api.Play.current
import play.api.libs.json._
import models._
import service.{TimeTools}
import play.api.Logger

/**
 * This controller manages all the endpoints relating to collections, including authentication.
 */
trait Collections {
  // https://coderwall.com/p/t_rapw/cake-pattern-in-scala-self-type-annotations-explicitly-typed-self-references-explained
  this: Controller =>

  val isHTTPS = current.configuration.getBoolean("HTTPS").getOrElse(false)

  /**
   * Gets the collection.
   * @param id The id of the collection
   * @param f The action body. Returns a result
   * @return A future result
   */
  def getCollection(id: Long)(f: Collection => Result): Future[Result] = {
    Future(Collection.findById(id).map(collection => f(collection))
      .getOrElse(Errors.notFound))
  }


  def collectionAsJson(id: Long) = Authentication.secureAPIAction() {
    implicit request =>
      implicit user =>
      Ok(Json.toJson(Collection.findById(id).map(collection =>
        Json.obj(
          "name" -> collection.name,
          "thumbnail" -> Json.toJson(collection.getContent.map(_.thumbnail).find(_.nonEmpty).getOrElse("")),
          "published" -> collection.published,
          "archived" -> collection.archived,
          "id" -> collection.id.get,
          "content" -> collection.getContent.map(content =>
            Json.obj(
              "id" -> content.id,
              "name" -> content.name,
              "contentType" -> content.contentType.toString,
              "thumbnail" -> content.thumbnail,
              "views" -> content.views))))))
  }

  /**
   * The collection page.
   */
  def view(id: Long) = Authentication.authenticatedAction() {
    implicit request =>
      implicit user =>
        getCollection(id) { collection =>
          // if (user.hasCollectionPermission(collection, "viewCollection")) instead
          if (collection.getMembers.contains(user) ||  SitePermissions.userHasPermission(user, "admin"))
            Ok(views.html.collections.view(collection, Json.toJson(collection.getLinkedCourses.map(_.toJson)).toString,
              Json.toJson(User.findUsersByUserIdList(CollectionMembership.getExceptionsByCollection(collection).map(_.userId)).map(_.toJson)).toString,
              Json.toJson(collection.getTAs.map(_.toJson)).toString))
          else
            Errors.forbidden
        }
  }

  /**
   * Edit collection information
   */
  def edit(id: Long) = Authentication.authenticatedAction(parse.urlFormEncoded) {
    implicit request =>
      implicit user =>
        getCollection(id) { collection =>
          if (user.isCollectionTA(collection)) {
            val name = request.body("name")(0)
            collection.copy(name = name).save
            Redirect(routes.Collections.view(id)).flashing("info" -> "Collection updated")
          } else
            Errors.forbidden
        }
  }

  /**
   * Add the content(s) to a specified collection.
   * @param id The ID of the collection
   */
  def addContent(id: Long) = Authentication.authenticatedAction(parse.multipartFormData) {
    implicit request =>
      implicit user =>
        getCollection(id) { collection =>
          // Only collection owners and TAs can add content
          if (user.isCollectionTA(collection)) {
            for ( // Add the content to the collection
              id <- request.body.dataParts("addContent");
              content <- Content.findById(id.toLong)
            ) { collection.addContent(content) }
            Redirect(routes.Collections.view(id))
            .flashing("success" -> "Content added to collection.")
          } else
            Errors.forbidden
        }
  }

  /**
   * Remove the content(s) from a specified collection.
   * @param id The ID of the collection
   */
  def removeContent(id: Long) = Authentication.authenticatedAction(parse.urlFormEncoded) {
    implicit request =>
      implicit user =>
        getCollection(id) { collection =>
          // Only non-guest members and admins can remove content
          if (user.isCollectionTA(collection)) {
            for ( // Remove the content to the collection
              id <- request.body("removeContent");
              content <- Content.findById(id.toLong)
            ) { collection.removeContent(content) }
            Redirect(routes.Collections.view(id))
            .flashing("success" -> "Content removed from collection.")
          } else
            Errors.forbidden
        }
  }

  /**
   * Creates a new collection
   */
  def create = Authentication.authenticatedAction(parse.urlFormEncoded) {
    implicit request =>
      implicit user =>
        Future {
          // Check if the user is allowed to create a collection
          if (user.hasSitePermission("createCollection")) {

            // Collect info
            val collectionName = request.body("collectionName")(0)

            // Create the collection
            val collection = Collection(None, user.id.get, collectionName, false, false).save
            user.enroll(collection, true)

            // Redirect to the collection page
            Redirect(routes.Collections.view(collection.id.get)).flashing("success" -> "Collection Added")
          } else
          Errors.forbidden
        }
  }

  /**
   * Some Functions to publish/unpublish archive/unarchive a collection
   */
  private val publish = (value: Boolean, c: Collection) => c.setPublished(value).toJson;
  private val archive = (value: Boolean, c: Collection) => c.setArchived(value).toJson;

  /**
   * Endpoint to set published/archived flags
   * @param id the collection id
   * @param action A String that is one of the following: publish, unpublished, archive, unarchive
   */
  def setFlag(id: Long, action: String) = Authentication.secureAPIAction() {
    implicit request =>
      implicit user =>
        {action match {
          case "publish"   => processCollection(id, publish(true, _))
          case "unpublish" => processCollection(id, publish(false, _))
          case "archive"   => processCollection(id, archive(true, _))
          case "unarchive" => processCollection(id, archive(false, _))
          case _ => Json.toJson("Failed")
        }} match {
          case Right(collection: JsValue) => Ok(collection)
          case Left(errorMessage: JsValue) => BadRequest(errorMessage)
        }
  }

  /**
   * Runs a function on a collection if it is found.
   * @param id The collection id
   * @param proc The function to run on the collection
   * @return Right(JsValue) if the Collection was found, Left(JsValue) if the collection was not found
   */
  def processCollection(id: Long, proc: Collection => JsValue)(implicit user: User): Either[JsValue, JsValue] = {
    Collection.findById(id).map { col =>
      Right(proc(col))
    }.getOrElse(Left(Json.toJson(s"Collection ${id} not found.")))
  }

  /**
   * The 'create a new collection' view
   */
  def createPage = Authentication.authenticatedAction() {
    implicit request =>
      implicit user =>

        // Check if the user is allowed to create a collection
        Future {
          if (user.hasSitePermission("createCollection"))
            Ok(views.html.collections.create())
          else
            Errors.forbidden
        }
  }

  /**
   * Lists all the collections
   */
  def list = Authentication.authenticatedAction() {
    implicit request =>
      implicit user =>
        Authentication.enforcePermission("joinCollection") {
          Future(Ok(views.html.collections.list(Collection.list)))
        }
  }

  /**
   * Links the given courses to the collection in question by creating collectionCourseLink records
   * This will create course records if they do not exist
   * The json request body will contain a List of Course objects as defined in the course model
   * @param collectionId The collection id to which we want to add courses
   * @return Http response that contains a json list of the newly created courses. If a course if requested to be linked
   * but already exists as a record in the database, then that course is not included in the response.
   */
  def linkCourses(collectionId: Long) = Authentication.authenticatedAction(parse.json) {
    implicit request =>
      implicit user =>
        Future {
          val collection = Collection.findById(collectionId)
          if (collection.isEmpty)
            BadRequest(s"""Collection ${collectionId} does not exist.""").as("application/json")
          else {
            request.body.validate[List[Course]] match {
              case success: JsSuccess[List[Course]] => {
                val linkedCourses = CollectionCourseLink.listByCollection(collection.get).map(_.courseId)
                val proposedCourses = success.get
                val existingCourses = Course.findExact(proposedCourses)
                val newCourses = proposedCourses.filterNot((pcourse) => existingCourses.exists(_.name == pcourse.name)).map { course =>
                  course.save
                }
                val newLinks = existingCourses ::: newCourses filterNot(course => linkedCourses.contains(course.id.get))
                newLinks map { course =>
                  // create the new collection course links
                  CollectionCourseLink(None, collectionId, course.id.get).save
                }
                Ok(Json.toJson(newCourses.map(_.toJson))).as("application/json")
              }
              case e: JsError => {
                Logger.debug(request.body.toString)
                BadRequest(JsError.toJson(e).toString).as("application/json")
              }
            }
          }
        }
  }

  def unlinkCourses(collectionId: Long) = Authentication.authenticatedAction(parse.json) {
    implicit request =>
      implicit user =>
        Future {
          val collection = Collection.findById(collectionId)
          if (collection.isEmpty)
            BadRequest(s"""{"rowsRemoved": 0, "message": "Collection ${collectionId} does not exist."""").as("application/json")
          else {
            val linkedCourses = CollectionCourseLink.listByCollection(collection.get).map(_.courseId)
            request.body.validate[List[Course]] match {
              case success: JsSuccess[List[Course]] => {
                val courseList = success.get
                if (courseList.isEmpty) {
                  Ok(Json.toJson("""{"rowsRemoved": 0, "message": "No Courses Provided"}""")).as("application/json")
                } else {
                  val courseIds = courseList.foldLeft(List[Long]()) {(list,course) =>
                    if (!course.id.isEmpty)
                      course.id.get :: list
                    else
                      list
                  }

                  val numRows = CollectionCourseLink.removeLinks(collectionId, courseIds)
                  Ok(Json.toJson(s"""{"rowsRemoved": $numRows}""")).as("application/json")
                }
              }
              case e: JsError => BadRequest(JsError.toJson(e).toString).as("application/json")
            }
          }
        }
  }

  /**
   * Add a TA to the collection based on a Cas username
   * @param id The collection id
   */
  def addTA(id: Long) = Authentication.authenticatedAction(parse.json) {
    implicit request =>
      implicit user =>
        Future {
          val collOption = Collection.findById(id)
          if (collOption.isEmpty) {
            Results.BadRequest
          } else {
            request.body.validate[String] match {
              case success: JsSuccess[String] => {
                val userOpt = User.findByUsername('cas, success.get)
                if (userOpt.isEmpty) {
                  Results.BadRequest("User not found. Make sure the user has previously logged in via CAS")
                } else {
                  if (!userOpt.get.isEnrolled(collOption.get))
                    userOpt.get.enroll(collOption.get, false, true)

                  CollectionPermissions.addTA(collOption.get, userOpt.get)
                  Ok(userOpt.get.toJson).as("application/json")
                }
              }
              case e: JsError => BadRequest(JsError.toJson(e).toString).as("application/json")
            }
          }
        }
  }

  /**
   * Remove a TA from the collection based on a Cas username
   * @param id the Collection id
   */
  def removeTA(id: Long) = Authentication.authenticatedAction(parse.json) {
    implicit request =>
      implicit user =>
        Future {
          val collOption = Collection.findById(id)
          if (collOption.isEmpty) {
            Results.BadRequest
          } else {
            request.body.validate[String] match {
              case success: JsSuccess[String] => {
                val userOpt = User.findByUsername('cas, success.get)
                if (userOpt.isEmpty) {
                  Results.BadRequest("User not found.")
                } else {
                  userOpt.get.unenroll(collOption.get)
                  Ok(userOpt.get.toJson).as("application/json")
                }
              }
              case e: JsError => BadRequest(JsError.toJson(e).toString).as("application/json")
            }
          }
        }
  }

  /**
   * Add a student to the collection based on a Cas username
   * @param id The collection id
   */
  def addException(id: Long) = Authentication.authenticatedAction(parse.json) {
    implicit request =>
      implicit user =>
        Future {
          val collOption = Collection.findById(id)

          if (collOption.isEmpty)
            BadRequest(s"""Collection %(id) does not exist.""").as("applcation/json")

          else {
            val collection = collOption.get
            request.body.validate[String] match {
              case success: JsSuccess[String] => {
                val username = success.get
                val userOpt = User.findByUsername('cas, username)

                if (userOpt.isEmpty)
                  BadRequest("""{"Message": "NetId does not exist. Make sure that user has logged in via CAS."}""").as("application/json")

                else{
                  val user = userOpt.get

                  // Case: User already has an exception in the given course...
                  if (CollectionMembership.getExceptionsByCollection(collection).exists(_.id == user.id.get))
                    BadRequest("""{"Message": "This exception has already been added."}""").as("application/json")

                  // Case: User is enrolled but does not have an exception...
                  else if (CollectionMembership.userIsEnrolled(user, collection)){
                    // update user to have an exception
                    val membershipRecord = CollectionMembership.listByCollection(collection).find(_.userId == user.id.get)

                    // update membership record in database with exception = true
                    if (!membershipRecord.isEmpty){
                      membershipRecord.get.copy(exception = true).save

                      // TODO: Check if exception actually created for user
                      Ok(user.toJson)
                    }
                    else
                      BadRequest("""{"Message": "500: Internal Server Error."}""").as("application/json")
                  }

                  // Case: User is not enrolled...
                  else{
                    // Enroll user and create exception to collection; then respond ok
                    // TODO: Check if exception actually created for user
                    Ok(user.enroll(collection, false, true).toJson)
                  }
                }
              }

              case e: JsError => BadRequest(JsError.toJson(e).toString).as("application/json")
            }
          }
        }
  }


  /**
   * Remove a student from the collection based on a Cas username
   * @param id The collection id
   */
  def removeException(id: Long) = Authentication.authenticatedAction(parse.json) {
    implicit request =>
      implicit user =>
        Future {
          val collOption = Collection.findById(id)

          if (collOption.isEmpty)
            BadRequest(s"""Collection %(id) does not exist.""").as("applcation/json")

          else {
            val collection = collOption.get
            request.body.validate[String] match {
              case success: JsSuccess[String] => {
                val username = success.get
                val userOpt = User.findByUsername('cas, username)

                // Validate user
                if (userOpt.isEmpty)
                  BadRequest("""{"Message": "NetId does not exist. Make sure that user has logged in via CAS."}""").as("application/json")

                else {
                  val exception = userOpt.get

                  // Case: User has an exception in the given course...
                  if (CollectionMembership.getExceptionsByCollection(collection).exists(_.userId == exception.id.get)){

                    // update user to have an exception
                    val membershipRecord = CollectionMembership.listByCollection(collection).find(_.userId == exception.id.get)

                    // update membership record in database with exception = false
                    if (!membershipRecord.isEmpty) {
                      membershipRecord.get.copy(exception = false).save
                      Ok(exception.toJson)
                    }
                    else
                      BadRequest("""{"Message": "500: Internal Server Error."}""").as("application/json")
                  }
                  // Case: User is enrolled but does not have an exception...
                  else if (CollectionMembership.userIsEnrolled(exception, collection))
                    BadRequest("""{"Message": "Exception does not exist."}""").as("application/json")
                  // Case: User is not enrolled...
                  else
                    BadRequest("""{"Message": "This user is not enrolled."}""").as("application/json")
                }
              }

              case e: JsError => BadRequest(JsError.toJson(e).toString).as("application/json")
            }
          }
        }
  }





  /**
   * Give permissions to a user
   * @param operation add, remove or match
   */
  def setPermission(id: Long, operation: String = "") = Authentication.authenticatedAction(parse.multipartFormData) {
    implicit request =>
      implicit user =>
        val data = request.body.dataParts
        getCollection(id) { collection =>
          if(user.isCollectionTeacher(collection)) {
            User.findById(data("userId")(0).toLong) foreach { member =>
              operation match {
                case "remove" =>
                  data("permission").foreach { permission =>
                    member.removeCollectionPermission(collection, permission)
                  }
                case "match" =>
                  user.removeAllCollectionPermissions(collection)
                  data("permission").foreach { permission =>
                    member.addCollectionPermission(collection, permission)
                  }
                case _ => data("permission").foreach { permission =>
                  member.addCollectionPermission(collection, permission)
                }
              }
            }
            Ok
          } else Results.Forbidden
        }
  }
}

object Collections extends Controller with Collections
