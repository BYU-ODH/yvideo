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
 * This controller manages all the pages relating to collections, including authentication.
 */
object Collections extends Controller {

  val isHTTPS = current.configuration.getBoolean("HTTPS").getOrElse(false)

  /**
   * Gets the collection. A mix-in for action composition.
   * @param id The id of the collection
   * @param f The action body. Returns a result
   * @param request The implicit http request
   * @return A result
   */
  def getCollection(id: Long)(f: Collection => Future[Result])(implicit request: Request[_]): Future[Result] = {
    Collection.findById(id).map(collection => f(collection))
	  .getOrElse(Future(Errors.notFound))
  }

  /**
   * The collection page.
   */
  def view(id: Long) = Authentication.authenticatedAction() {
    implicit request =>
      implicit user =>
        getCollection(id) { collection =>
          Future {
            // TODO: Once the users get the "viewCollection" permission, use
            // if (user.hasCollectionPermission(collection, "viewCollection")) instead
            if (collection.getMembers.contains(user) ||  SitePermissions.userHasPermission(user, "admin"))
              Ok(views.html.collections.view(collection, Json.toJson(collection.getLinkedCourses.map(_.toJson)).toString, 
                Json.toJson(User.findUsersByUserIdList(CollectionMembership.getExceptionsByCollection(collection).map(_.userId)).map(_.toJson)).toString))
            else
              Errors.forbidden
          }
        }
  }

  /**
   * Edit collection information
   */
  def edit(id: Long) = Authentication.authenticatedAction(parse.urlFormEncoded) {
    implicit request =>
      implicit user =>
        getCollection(id) { collection =>
          Future {
            if (user.hasCollectionPermission(collection, "editCollection")) {
              val name = request.body("name")(0)
              collection.copy(name = name).save
              Redirect(routes.Collections.view(id)).flashing("info" -> "Collection updated")
            } else
              Errors.forbidden
          }
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
          Future {
            // Only non-guest members and admins can add content
            if (user.hasCollectionPermission(collection, "addContent")) {
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
  }

  /**
   * Remove the content(s) from a specified collection.
   * @param id The ID of the collection
   */
  def removeContent(id: Long) = Authentication.authenticatedAction(parse.urlFormEncoded) {
    implicit request =>
      implicit user =>
        getCollection(id) { collection =>
          Future {
            // Only non-guest members and admins can remove content
            if (user.hasCollectionPermission(collection, "removeContent")) {
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
  }

  /**
   * Creates a new collection
   */
  def create = Authentication.authenticatedAction(parse.urlFormEncoded) {
    request =>
      user =>
        Future {
          // Check if the user is allowed to create a collection
          if (user.hasSitePermission("createCollection")) {

            // Collect info
            val collectionName = request.body("collectionName")(0)

            // Create the collection
            val collection = Collection(None, user.id.get, collectionName).save
            user.enroll(collection, true)

            // Redirect to the collection page
            Redirect(routes.Collections.view(collection.id.get)).flashing("success" -> "Collection Added")
          } else
          Errors.forbidden
        }
  }

  /**
   * The create a new collection view
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
   * Remove a student from a collection
   * @param id The ID of the collection
   * @param studentId The user ID of the student
   */
  def removeStudent(id: Long, studentId: Long) = Authentication.authenticatedAction() {
    implicit request =>
      implicit user =>
        getCollection(id) { collection =>
          Future {
            if (user.hasCollectionPermission(collection, "removeStudent")) {
              User.findById(studentId) match {
              case Some(student) =>
                student.unenroll(collection)
                Redirect(routes.Collections.view(collection.id.get))
                  .flashing("info" -> "Student removed")
              case _ =>
                Errors.notFound
              }
            } else
              Errors.forbidden
          }
        }
  }

  /**
   * Used when one removes oneself from a specific collection and one is a user
   * @param id the Collection Id
   */
  def quitCollection(id: Long) = Authentication.authenticatedAction() {
    implicit request =>
      implicit user =>
        getCollection(id) { collection =>
          user.unenroll(collection)
          Future {
            Redirect(routes.Application.home)
              .flashing("info" -> s"You just quit ${collection.name}")
          }
        }
  }

  def linkCourses(collectionId: Long) = Authentication.authenticatedAction(parse.json) {
    implicit request =>
      implicit user =>
        Future {
          val collection = Collection.findById(collectionId)
          if (collection.isEmpty)
            BadRequest(s"""Collection ${collectionId} does not exist.""").as("application/json")
          else {
            val linkedCourses = CollectionCourseLink.listByCollection(collection.get).map(_.courseId)
            request.body.validate[List[String]] match {
              case success: JsSuccess[List[String]] => {
                val courseNames = success.get
                val courses = Course.findByName(courseNames)
                val newCourses = courseNames.diff(courses.map(_.name)).map { courseName =>
                  Course(None, courseName, None, None).save
                }
                val newLinks = courses ::: newCourses filterNot(course => linkedCourses.contains(course.id.get))
                newLinks map { course =>
                  // create the new collection course links
                  CollectionCourseLink(None, collectionId, course.id.get).save.id.get
                }
                Ok(Json.toJson(newLinks.map(_.toJson))).as("application/json")
              }
              case e: JsError => BadRequest(JsError.toJson(e).toString).as("application/json")
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
            request.body.validate[List[String]] match {
              case success: JsSuccess[List[String]] => {
                val courseNames = success.get
                if (courseNames.isEmpty) {
                  Ok(Json.toJson("""{"rowsRemoved": 0, "message": "No Courses Provided"}""")).as("application/json")
                } else {
                  val courses = Course.findByName(courseNames)
                  val numRows = CollectionCourseLink.removeLinks(collectionId, courses)
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
  def addTA(id: Long) = Authentication.authenticatedAction(parse.multipartFormData) {
    implicit request =>
      implicit user =>
        val netid: Option[String] = for {
          parts <- request.body.dataParts.get("netid")
          netid <- parts.headOption
        } yield netid
        Future {
          if (netid.isEmpty) {
            Results.BadRequest
          } else {
            val userOpt = User.findByUsername('cas, netid.get)
            if (userOpt.isEmpty) {
              Results.BadRequest("User not found. Make sure the user has logged in via cas.")
            } else {
              var user = userOpt.get
              val collectionOpt = Collection.findById(id)
              if (collectionOpt.isEmpty) {
                Results.BadRequest("Collection does not exist")
              } else {
                user = user.enroll(collectionOpt.get, false)
                CollectionPermissions.addTA(collectionOpt.get, user)
                Ok
              }
            }
          }
        }
  }

  /**
   * Add a TA to the collection based on a Cas username
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
                    if (!membershipRecord.isEmpty)
                      Ok(membershipRecord.get.copy(exception = true).save.toJson)
                    else
                      BadRequest("""{"Message": "500: Internal Server Error."}""").as("application/json")
                  }

                  // Case: User is not enrolled...
                  else{
                    // Enroll user and create exception to collection; then respond ok
                    Ok(CollectionMembership(None, user.id.get, collection.id.get, false, true).save.toJson)
                  }
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
          Future {
            if(user.hasCollectionPermission(collection, "teacher")) {
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
}
