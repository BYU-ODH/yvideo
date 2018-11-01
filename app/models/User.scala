package models

import anorm._
import anorm.SqlParser._
import java.sql.SQLException
import dataAccess.sqlTraits._
import play.api.db.DB
import play.api.libs.json.{Json, JsValue}
import play.api.Play.current
import play.api.Logger
import controllers.routes
import service.{EmailTools, TimeTools}

/**
 * User
 * @param id The ID of the user.
 * @param authId ID returned from authentication
 * @param authScheme Which authentication scheme this user used
 * @param username The username (often the same as authId)
 * @param name A displayable name for the user
 * @param email The user's email address
 * @param role The permissions of the user
 */
case class User(id: Option[Long], authId: String, authScheme: Symbol, username: String,
                name: Option[String] = None, email: Option[String] = None,
                picture: Option[String] = None, accountLinkId: Long = -1,
                created: String = TimeTools.now(), lastLogin: String = TimeTools.now())
  extends SQLSavable with SQLDeletable {

  /**
   * Saves the user to the DB
   * @return The possibly updated user
   */
  def save =
    if (id.isDefined) {
      update(User.tableName,
        'authId -> authId, 'authScheme -> authScheme.name,
        'username -> username, 'name -> name, 'email -> email, 'picture -> picture,
        'accountLinkId -> accountLinkId, 'created -> created, 'lastLogin -> lastLogin
      )
      this
    } else {
      val id = insert(User.tableName,
        'authId -> authId, 'authScheme -> authScheme.name,
        'username -> username, 'name -> name, 'email -> email, 'picture -> picture,
        'accountLinkId -> accountLinkId, 'created -> created, 'lastLogin -> lastLogin
      )
      this.copy(id)
    }

  /**
   * Deletes the user from the DB
   */
  def delete() {
    // Delete the user's content
    getContent.foreach(_.delete())

    DB.withConnection { implicit connection =>
      try {
        SQL("delete from collectionMembership where userId = {userId}")
          .on('userId -> id.get).execute()
        SQL("delete from notification where userId = {userId}")
          .on('userId -> id.get).execute()
        SQL("delete from sitePermissions where userId = {userId}")
          .on('userId -> id.get).execute()
        SQL("delete from sitePermissionRequest where userId = {userId}")
          .on('userId -> id.get).execute()
        // Delete all linked accounts
        getAccountLink.map { accountLink =>
          if (accountLink.primaryAccount == id.get) {
            val params = accountLink.userIds.filterNot(_ == id.get)
              .map { uid =>  List(NamedParameter.symbol('id -> uid)) }
              .toList
            BatchSql(
              s"delete from $User.tableName where id = {id}",
              params.head, params.tail:_*
            ).execute()
          }
        }

      } catch {
        case e: SQLException =>
          Logger.debug("Failed to delete User data")
          Logger.debug(e.getMessage())
      }
    }

    delete(User.tableName)
  }


  //                  _   _
  //        /\       | | (_)
  //       /  \   ___| |_ _  ___  _ __  ___
  //      / /\ \ / __| __| |/ _ \| '_ \/ __|
  //     / ____ \ (__| |_| | (_) | | | \__ \
  //    /_/    \_\___|\__|_|\___/|_| |_|___/
  //
  //  ______ ______ ______ ______ ______ ______ ______ ______ ______
  // |______|______|______|______|______|______|______|______|______|
  //

  /**
   * Checks if a user is already enrolled in a collection
   * @param collection The collection in which the user will be enrolled
   */
  def isEnrolled(collection: Collection): Boolean = CollectionMembership.userIsEnrolled(this, collection)

  /**
   * Enrolls the user in a collection
   * @param collection The collection in which the user will be enrolled
   * @param teacher Is this user a teacher of the collection?
   * @return The user (for chaining)
   */
  def enroll(collection: Collection, teacher: Boolean = false, exception: Boolean = false): User = {
    if(!this.isEnrolled(collection))
      CollectionMembership(None, id.get, collection.id.get, teacher, exception).save
    this
  }

  /**
   * Unenroll the user from a collection
   * @param collection The collection from which to unenroll
   * @return The user (for chaining)
   */
  def unenroll(collection: Collection): User = {

    // First, find the membership
    val membership = CollectionMembership.listByUser(this).filter(_.collectionId == collection.id.get)

    if (membership.size > 1)
      Logger.warn("Multiple (or zero) memberships for user #" + id.get + " in collection #" + collection.id.get)

    // Delete all found
    membership.foreach(_.delete)

    this
  }

  /**
   * Create content from a resource and assign this user as the owner
   * @param content The content that will be owned
   * @return The content ownership
   */
  def addContent(content: Content): ContentOwnership =
    ContentOwnership(None, this.id.get, content.id.get).save

  /**
   * Sends a notification to this user
   * @param message The message of the notification
   * @return The notification
   */
  def sendNotification(message: String): Notification = {
    if (Setting.findByName("notifications.users.emailOn.notification").get.value == "true" && email.isDefined) {
      EmailTools.sendEmail(List((displayName, email.get)), "Ayamel notification") {
        s"You have received the following notification:\n\n$message"
      } {
        s"<p>You have received the following notification:</p><p>$message</p>"
      }
    }
    Notification(None, this.id.get, message).save
  }

  def addWord(word: String, srcLang: String, destLang: String): WordListEntry = WordListEntry(None, word, srcLang, destLang, id.get).save

  /**
   * Moves user ownership and enrollment from the provided user to the current user
   * @param user The user to move ownership
   */
  def consolidateOwnership(user: User) {
    val thisid = this.id.get

    // Transfer content ownership
    ContentOwnership.listByUser(user).foreach { _.copy(userId = thisid).save }

    // Move the notifications over
    user.getNotifications.foreach { _.copy(userId = thisid).save }

    // Move the collection membership over. Check this user's membership to prevent duplicates
    val myMembership = CollectionMembership.listByUser(this).map(_.collectionId)
    CollectionMembership.listByUser(user).foreach { membership =>
      if (myMembership.contains(membership.collectionId))
        membership.delete()
      else
        membership.copy(userId = thisid).save
    }

    // Merge permissions
    user.getPermissions.foreach { p => addSitePermission(p) }

  }

  /**
   * Merges the provided user into this one.
   * @param user The user to merge
   */
  //TODO: Figure out how to properly deal with unchecked gets
  //They will only fail if data is corrupted, and it's not immediately clear
  //what should be done in those cases
  def merge(user: User) {
    val thisid = this.id.get

    /*
     * Three possibilities:
     * 1. Neither user has an account link
     * 2. One user has an account link
     * 3. Both users have an account link
     */
    if (this.accountLinkId == -1) {
      if (user.accountLinkId == -1) {
        // Case 1: Create a new account link
        val accountLink = AccountLink(None, Set(thisid, user.id.get), thisid).save

        val linkId = accountLink.id.get
        this.copy(accountLinkId = linkId).save
        user.copy(accountLinkId = linkId).save

        consolidateOwnership(user)
      } else {
        //Case 2: Make this the primary of the existing account link
        val accountLink = AccountLink.findById(user.accountLinkId).get

        accountLink.addUser(this)

        consolidateOwnership(accountLink.getPrimaryUser.get)

        this.copy(accountLinkId = accountLink.id.get).save
        accountLink.copy(primaryAccount = thisid).save
      }
    } else {
      //these branches can only be talen if this is the primary user
      if (user.accountLinkId == -1) {
        // Case 2: Add the other user to this account link
        val accountLink = AccountLink.findById(this.accountLinkId).get

        accountLink.addUser(user)

        consolidateOwnership(user)
      } else {
        // Case 3: Merge the other account link into this one
        val thisLink = AccountLink.findById(this.accountLinkId).get
        val userLink = AccountLink.findById(user.accountLinkId).get
        val newLink = thisLink.copy(userIds = thisLink.userIds ++ userLink.userIds, primaryAccount = thisid).save
        val linkId = newLink.id.get

        consolidateOwnership(userLink.getPrimaryUser.get)
        newLink.getUsers foreach { user => user.copy(accountLinkId = linkId).save }
        userLink.delete()
      }
    }
  }

  /**
   * Gets a string from an option.
   */
  def getStringFromOption(opt: Option[String]): String = opt.getOrElse("")

  /**
   * Gets all of the fields required for the Admin dashboard table
   */
  def toJson = {
    Json.obj(
      "id" -> id,
      "authScheme" -> authScheme.name,
      "username" -> username,
      "name" -> getStringFromOption(name),
      "email" -> getStringFromOption(email),
      "linked" -> accountLinkId,
      "permissions" -> getPermissions,
      "lastLogin" -> lastLogin
    )
  }

  //       _____      _   _
  //      / ____|    | | | |
  //     | |  __  ___| |_| |_ ___ _ __ ___
  //     | | |_ |/ _ \ __| __/ _ \ '__/ __|
  //     | |__| |  __/ |_| ||  __/ |  \__ \
  //      \_____|\___|\__|\__\___|_|  |___/
  //
  //   ______ ______ ______ ______ ______ ______ ______ ______ ______
  // |______|______|______|______|______|______|______|______|______|
  //

  /**
   * Any items that are retrieved from the DB should be cached here in order to reduce the number of DB calls
   */
  val cacheTarget = this
  object cache {

    var enrollment: Option[List[Collection]] = None

    def getEnrollment = {
      if (enrollment.isEmpty)
        enrollment = Some(CollectionMembership.listUsersClasses(cacheTarget))
      enrollment.get
    }

    var collections: Option[List[Collection]] = None

    def getEligibleCollections(courseNames: List[Course]) = {
      if (collections.isEmpty)
        collections = Some(Collection.getEligibleCollections(courseNames, cacheTarget))
      collections.get
    }

    var teacherEnrollment: Option[List[Collection]] = None

    def getTeacherEnrollment = {
      if (teacherEnrollment.isEmpty)
        teacherEnrollment = Some(CollectionMembership.listTeacherClasses(cacheTarget))
      teacherEnrollment.get
    }

    var content: Option[List[Content]] = None

    def getContent = {
      if (content.isEmpty)
        content = Some(ContentOwnership.listUserContent(cacheTarget))
      content.get
    }

    var contentFeed: Option[List[(Content, Long)]] = None

    def getContentFeed = {
      if (contentFeed.isEmpty)
        contentFeed = Some(
          getEnrollment.flatMap(collection => collection.getContent.map(c => (c, collection.id.get)))
        )
      contentFeed.get
    }

    var notifications: Option[List[Notification]] = None

    def getNotifications = {
      if (notifications.isEmpty)
        notifications = Some(Notification.listByUser(cacheTarget))
      notifications.get
    }

    var accountLink: Option[Option[AccountLink]] = None

    def getAccountLink = {
      if (accountLink.isEmpty)
        accountLink = Some(AccountLink.findById(accountLinkId))
      accountLink.get
    }

    var scorings: Option[List[Scoring]] = None

    def getScorings: List[Scoring] = {
      if (scorings.isEmpty)
        scorings = Some(Scoring.listByUser(cacheTarget))
      scorings.get
    }

    var wordList: Option[List[WordListEntry]] = None

    def getWordList: List[WordListEntry] = {
      if (wordList.isEmpty)
        wordList = Some(WordListEntry.listByUser(cacheTarget))
      wordList.get
    }

  }

  /**
   * Gets the collections that the user can be enrolled in
   * @return A list of collections
   */
  def getEligibleCollections(courseList: List[Course]): List[Collection] = cache.getEligibleCollections(courseList)

  /**
   * Gets the enrollment--collections the user is in--of the user
   * @return The list of collections
   */
  def getEnrollment: List[Collection] = cache.getEnrollment

  /**
   * Gets the collections this user is teaching
   * @return The list of collections
   */
  def getTeacherEnrollment: List[Collection] = cache.getTeacherEnrollment

  /**
   * Gets the content belonging to this user
   * @return The list of content
   */
  def getContent: List[Content] = cache.getContent

  /**
   * Get the profile picture. If it's not set then return the placeholder picture.
   * @return The url of the picture
   */
  def getPicture: String = picture.getOrElse(routes.Assets.at("images/users/facePlaceholder.jpg").url)

  /**
   * Tries the user's name, if it doesn't exists then returns the username
   * @return A displayable name
   */
  def displayName: String = name.getOrElse(username)

  /**
   * Gets the latest content from this user's collections.
   * @param limit The number of content objects to get
   * @return The content
   */
  def getContentFeed(limit: Int = 5): List[(Content, Long)] = cache.getContentFeed.take(limit)

  /**
   * Gets a list of the user's notifications
   * @return
   */
  def getNotifications: List[Notification] = cache.getNotifications

  /**
   * Returns the account link
   * @return If it exists, then Some(AccountLink) otherwise None
   */
  def getAccountLink: Option[AccountLink] =
    if (accountLinkId == -1)
      None
    else
      cache.getAccountLink

  /**
   * Gets the list of this user's scorings
   * @return The list of scorings
   */
  def getScorings = cache.getScorings

  /**
   * Gets the list of this user's scorings for a particular content
   * @return The list of scorings
   */
  def getScorings(content: Content) = cache.getScorings.filter(_.contentId == content.id.get)

  def getWordList = cache.getWordList

  def getPermissions = SitePermissions.listByUser(this)

  def getCollectionPermissions(collection: Collection) = collection.getUserPermissions(this)

  def hasSitePermission(permission: String): Boolean =
    SitePermissions.userHasPermission(this, permission) || SitePermissions.userHasPermission(this, "admin")

  def isCollectionTeacher(collection: Collection): Boolean =
    collection.userIsTeacher(this) || SitePermissions.userHasPermission(this, "admin")

  def isCollectionTA(collection: Collection): Boolean =
    collection.userIsTA(this) || isCollectionTeacher(collection)

  //       _____      _   _
  //      / ____|    | | | |
  //     | (___   ___| |_| |_ ___ _ __ ___
  //      \___ \ / _ \ __| __/ _ \ '__/ __|
  //      ____) |  __/ |_| ||  __/ |  \__ \
  //     |_____/ \___|\__|\__\___|_|  |___/
  //  ______ ______ ______ ______ ______ ______ ______ ______ ______
  // |______|______|______|______|______|______|______|______|______|
  //

  def addSitePermission(permission: String) =
    SitePermissions.addUserPermission(this, permission)

  def removeSitePermission(permission: String) =
    SitePermissions.removeUserPermission(this, permission)

  def removeAllSitePermissions =
    SitePermissions.removeAllUserPermissions(this)

  def addCollectionPermission(collection: Collection, permission: String) =
    collection.addUserPermission(this, permission)

  def removeCollectionPermission(collection: Collection, permission: String) =
    collection.removeUserPermission(this, permission)

  def removeAllCollectionPermissions(collection: Collection) =
    collection.removeAllUserPermissions(this)
}

object User extends SQLSelectable[User] {
  val tableName = "userAccount"

  val simple = {
    get[Option[Long]](tableName + ".id") ~
      get[String](tableName + ".authId") ~
      get[String](tableName + ".authScheme") ~
      get[String](tableName + ".username") ~
      get[Option[String]](tableName + ".name") ~
      get[Option[String]](tableName + ".email") ~
      get[Option[String]](tableName + ".picture") ~
      get[Long](tableName + ".accountLinkId") ~
      get[String](tableName + ".created") ~
      get[String](tableName + ".lastLogin") map {
      case id ~ authId ~ authScheme ~ username ~ name ~ email ~ picture ~ accountLinkId ~ created ~ lastLogin => {
        val _name = if (name.isEmpty) None else name
        val _email = if (email.isEmpty) None else email
        User(id, authId, Symbol(authScheme), username, _name, _email, picture, accountLinkId, created, lastLogin)
      }
    }
  }

  /**
   * Search the DB for a user with the given id.
   * @param id The id of the user.
   * @return If a user was found, then Some[User], otherwise None
   */
  def findById(id: Long): Option[User] = findById(id, simple)


  def findUsersByUserIdList(idList: List[Long]): List[User] = {
    DB.withConnection { implicit connection =>
      try {
        if (idList.isEmpty)
          List[User]()
        else
          SQL(s"select * from $tableName where id in ({ids})") .on('ids -> idList) .as(simple *)
      } catch {
        case e: SQLException =>
          Logger.debug("Failed in User.scala / findUsersByUserIdList")
          Logger.debug(e.getMessage())
          List[User]()
      }
    }
  }


  /**
   * Search the DB for a user with the given authentication info
   * @param authId The id from the auth scheme
   * @param authScheme Which auth scheme
   * @return If a user was found, then Some[User], otherwise None
   */
  def findByAuthInfo(authId: String, authScheme: Symbol): Option[User] = {
    DB.withConnection { implicit connection =>
      try {
        SQL(s"select * from $tableName where authId = {authId} and authScheme = {authScheme}")
          .on('authId -> authId, 'authScheme -> authScheme.name)
          .as(simple.singleOpt)
      } catch {
        case e: SQLException =>
          Logger.debug("Failed in User.scala / findByAuthInfo")
          Logger.debug(e.getMessage())
          None
      }
    }
  }

  /**
   * Finds a user based on the username and the authScheme.
   * @param authScheme The auth scheme to search
   * @param username The username to look for
   * @return If a user was found, then Some[User], otherwise None
   */
  def findByUsername(authScheme: Symbol, username: String): Option[User] = {
    DB.withConnection { implicit connection =>
      try {
        SQL(s"select * from $tableName where authScheme = {authScheme} and username = {username}")
          .on('authScheme -> authScheme.name, 'username -> username)
          .as(simple.singleOpt)
      } catch {
        case e: SQLException =>
          Logger.debug("Failed in User.scala / findByUsername")
          Logger.debug(e.getMessage())
          None
      }
    }
  }

  /**
   * Gets all users in the DB
   * @return The list of users
   */
  def list: List[User] = list(simple)

  /**
   * Gets the number of users in the DB
   * @return the number of total users
   */
  def count: List[Int] = {
    DB.withConnection {
      implicit connection =>
      try {
        SQL(s"select COUNT(id) as c from $tableName")
        .as(get[Int]("c") *)
      } catch {
        case e: SQLException =>
          Logger.debug("Error getting user count. User.scala")
          Nil
      }
    }
  }

  /**
   * Returns a paginated list of users for the admin user view
   * @param id The id of the last user in the current page
   * @param limit The desired size of the page
   * @param up: The direction in which to paginate i.e. up will be true when getting the next page
   * @return a paginated list of users
   */
  def listPaginated(id: Long, limit: Long, up: Boolean): List[User] = {
    DB.withConnection {
      implicit connection =>
        try {
          if (up) {
            SQL(s"select * from $tableName where id >= {lowerBound} order by id asc limit {upperBound}")
              .on('lowerBound -> id, 'upperBound -> (limit))
              .as(simple *)
          } else {
            SQL(s"select * from (select * from $tableName where id <= {lowerBound} order by id desc limit {upperBound}) as a order by id asc")
              .on('lowerBound -> id, 'upperBound -> (limit))
              .as(simple *)
          }
        } catch {
          case e: SQLException =>
            Logger.debug("Error getting paginated users. User.scala")
            Nil
        }
    }
  }


  /**
   * Returns a list of users based on the search criteria that is passed in
   * @param columnName The column that is being searched - Must be username, name or email
   * @param searchValue The value to be search for in the given columnEXIT
   * @return a list of users based on the given search criteria
   */
  def userSearch(columnName: String, searchValue: String): List[User] = {
    DB.withConnection {
      implicit connection =>
        try {
          val searchVal = "%"+searchValue+"%"
          SQL(s"select * from $tableName where $columnName like {searchValue}")
            .on('columnName -> columnName, 'searchValue -> searchVal)
            .as(simple *)
        } catch {
          case e: SQLException =>
            Logger.debug("Error getting user search results. User.scala")
            Nil
        }
    }
  }


  /**
   * Create a user from fixture data
   * @param data Fixture data
   * @return The user
   */
  def fromFixture(data: (String, Symbol, String, Option[String], Option[String], Symbol)): User = {
    val user = User(None, data._1, data._2, data._3, data._4, data._5).save
    SitePermissions.assignRole(user, data._6)
    user
  }
}
