package models

import anorm._
import anorm.SqlParser._
import java.sql.SQLException
import dataAccess.sqlTraits._
import service.{HashTools, SerializationTools, TimeTools}
import play.api.db.DB
import play.api.Play.current
import play.api.libs.json.{Json, JsValue}
import play.api.Logger
import concurrent.Future
import dataAccess.ResourceController
import java.text.Normalizer

/**
 * This links a resource object (in a resource library) to this system
 * @param id The id of this link in the DB
 * @param resourceId The id of the resource
 */
case class Content(id: Option[Long], name: String, contentType: Symbol, collectionId: Long, thumbnail: String, resourceId: String,
                   physicalCopyExists: Boolean, isCopyrighted: Boolean, enabled: Boolean, dateValidated: Option[String],
                   requester: String, published: Boolean, authKey: String = HashTools.md5Hex(util.Random.nextString(16)),
                   views: Long = 0, fullVideo: Boolean = true)
  extends SQLSavable with SQLDeletable {

  /**
   * Saves this content link to the DB
   * @return The optionally updated content
   */
  def save =
    if (id.isDefined) {
      update(Content.tableName, 'id -> id.get, 'name -> normalize(name), 'contentType -> contentType.name,
        'physicalCopyExists -> physicalCopyExists, 'isCopyrighted -> isCopyrighted, 'enabled -> enabled, 'dateValidated -> dateValidated.map(str => normalize(str)),
        'requester -> requester, 'collectionId -> collectionId, 'thumbnail -> thumbnail, 'resourceId -> resourceId,
        'published -> published, 'authKey -> authKey, 'views -> views, 'fullVideo -> fullVideo)
      this
    } else {
      val id = insert(Content.tableName, 'name -> normalize(name), 'contentType -> contentType.name,
        'physicalCopyExists -> physicalCopyExists, 'isCopyrighted -> isCopyrighted, 'enabled -> enabled,
        'dateValidated -> dateValidated.map(str => normalize(str)),
        'requester -> requester, 'collectionId -> collectionId, 'thumbnail -> thumbnail, 'resourceId -> resourceId, 'published -> published,
        'authKey -> authKey, 'views -> views, 'fullVideo -> fullVideo)
      this.copy(id)
    }

  /**
   * Deletes the content from the DB, but not from the resource library
   */
  def delete() {
    // Delete the content from courses
    ContentListing.listByContent(this).foreach(_.delete())

    // Delete ownership
    ContentOwnership.findByContent(this).delete()

    // Delete the content
    delete(Content.tableName)
  }

  //                  _   _
  //        /\       | | (_)
  //       /  \   ___| |_ _  ___  _ __  ___
  //      / /\ \ / __| __| |/ _ \| '_ \/ __|
  //     / ____ \ (__| |_| | (_) | | | \__ \
  //    /_/    \_\___|\__|_|\___/|_| |_|___/
  //
  //   ______ ______ ______ ______ ______ ______ ______ ______ ______
  // |______|______|______|______|______|______|______|______|______|
  //

  def setSetting(setting: String, argument: Seq[String]): Content = {
    Content.setSetting(this, setting, argument)
    this
  }

  def addSetting(setting: String, argument: Seq[String]): Content = {
    Content.addSetting(this, setting, argument)
    this
  }

  def removeSetting(setting: String, argument: Seq[String]): Content = {
    Content.removeSetting(this, setting, argument)
    this
  }

  // Recognize and fix certain diacritics that can cause issues in sql
  def normalize(str: String) = {
    Normalizer.normalize(str, Normalizer.Form.NFC)
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
   * Checks if the user is authorized to edit this content.
   * Teachers and TA's in the collection to which the content belongs can edit
   * @param user The user to check
   * @return Can edit or not
   */
  def isEditableBy(user: User): Boolean = {
    val collection: Option[Collection] = Collection.findById(collectionId)
    collection.nonEmpty && (user.hasSitePermission("admin") || collection.get.userCanEditContent(user))
  }

  def getSetting(setting: String) = Content.getSetting(this, setting)

  //for backwards compatibility
  def settings(setting: String) = Content.getSetting(this, setting).mkString(",")

  def enabledCaptionTracks = getSetting("captionTrack")

  def enabledAnnotationDocuments = getSetting("annotationDocument")

  def showTranscripts: String = getSetting("showTranscripts").lift(0).getOrElse("false")

  def embedClass: String =
    if (contentType == 'audio || contentType == 'video)
      contentType.name + {if (showTranscripts == "true") 2 else 1}
    else
      "generic"

  def toJson = Json.obj(
    "id" -> id.get,
    "name" -> name,
    "contentType" -> contentType.name,
    "collectionId" -> collectionId,
    "thumbnail" -> thumbnail,
    "physicalCopyExists" -> physicalCopyExists,
    "isCopyrighted" -> isCopyrighted,
    "enabled" -> enabled,
    "dateValidated" -> dateValidated.getOrElse("").asInstanceOf[String],
    "requester" -> requester,
    "resourceId" -> resourceId,
    "published" -> published,
    "settings" -> Content.getSettingMap(this).mapValues(_.mkString(",")),
    "authKey" -> authKey,
    "views" -> views,
    "fullVideo" -> fullVideo
  )

  val cacheTarget = this
  object cache {
    var owner: Option[User] = None
    var scorings: Option[List[Scoring]] = None


    def getOwner: Option[User] = {
      if (owner.isEmpty)
        owner = User.findById(ContentOwnership.findByContent(cacheTarget).userId)
      owner
    }

    def getScorings: List[Scoring] = {
      if (scorings.isEmpty)
        scorings = Some(Scoring.listByContent(cacheTarget))
      scorings.get
    }
  }

  def getOwner = cache.getOwner

  def getScorings = cache.getScorings

}

object Content extends SQLSelectable[Content] {
  val tableName = "content"
  val settingTable = "contentSetting"

  val simple = {
    get[Option[Long]](tableName + ".id") ~
      get[String](tableName + ".name") ~
      get[String](tableName + ".contentType") ~
      get[Long](tableName + ".collectionId") ~
      get[String](tableName + ".thumbnail") ~
      get[String](tableName + ".resourceId") ~
      get[Boolean](tableName + ".physicalCopyExists") ~
      get[Boolean](tableName + ".isCopyrighted") ~
      get[Boolean](tableName + ".enabled") ~
      get[Option[String]](tableName + ".dateValidated") ~
      get[String](tableName + ".requester") ~
      get[Boolean](tableName + ".published") ~
      get[String](tableName + ".authKey") ~
      get[Long](tableName + ".views") ~
      get[Boolean](tableName + ".fullVideo") map {
      case id ~ name ~ contentType ~ collectionId ~ thumbnail ~ resourceId ~ physicalCopyExists ~ isCopyrighted ~
         enabled ~ dateValidated ~ requester ~ published ~ authKey ~ views ~ fullVideo =>
        Content(id, name, Symbol(contentType), collectionId, thumbnail, resourceId, physicalCopyExists, isCopyrighted,
          enabled, dateValidated, requester, published, authKey, views, fullVideo)
    }
  }

  /**
   * Parser for getting (content, user) tuple in order to display content ownership
   */
  val contentOwnership = {
    // content object
    get[Option[Long]]("contentId") ~
    get[String]("cname") ~
    get[String]("contentType") ~
    get[Long]("collectionId") ~
    get[String]("thumbnail") ~
    get[String]("resourceId") ~
    get[Boolean]("physicalCopyExists") ~
    get[Boolean]("isCopyrighted") ~
    get[Boolean]("enabled") ~
    get[Option[String]]("dateValidated") ~
    get[String]("requester") ~
    get[Boolean]("published") ~
    get[String]("authKey") ~
    get[Long]("views") ~
    get[Boolean]("fullVideo") ~
    // user object
    get[Option[Long]]("userId") ~
    get[String]("authId") ~
    get[String]("authScheme") ~
    get[String]("username") ~
    get[Option[String]]("name") ~
    get[Option[String]]("email") ~
    get[Option[String]]("picture") ~
    get[Long]("accountLinkId") ~
    get[String]("created") ~
    get[String]("lastLogin") map {
      case contentId ~ cname ~ contentType ~ collectionId ~ thumbnail ~ resourceId ~ physicalCopyExists ~ isCopyrighted ~
        enabled ~ dateValidated ~ requester ~ published ~ authKey ~ views ~ fullVideo ~
        userId ~ authId ~ authScheme ~ username ~ name ~ email ~ picture ~ accountLinkId ~ created ~ lastLogin =>
          Content(contentId, cname, Symbol(contentType), collectionId, thumbnail, resourceId, physicalCopyExists,
            isCopyrighted, enabled, dateValidated, requester, published, authKey, views, fullVideo) ->
          User(userId, authId, Symbol(authScheme), username, name, email, picture, accountLinkId, created, lastLogin)
    }
  }

  /**
   * Finds a content by the given id
   * @param id The id of the content link
   * @return If a content link was found, then Some[Content], otherwise None
   */
  def findById(id: Long): Option[Content] = findById(id, simple)

  /**
   * Gets all the content in the DB
   * @return The list of content
   */
  def list: List[Content] = list(simple)

  /**
   * Gets all content and owners information
   * @return map of contentId's to tuple (owner, email)
   */
  def ownershipList: List[(Content, User)] = {
    DB.withConnection { implicit connection =>
      try {
        SQL("""
          select * from
            ( select content.name as cname, content.*, contentOwnership.contentId, contentOwnership.userId
              from content join contentOwnership on content.id = contentOwnership.contentid
            ) as listing
          join userAccount on userAccount.id = listing.userId
            """).as(contentOwnership *)
      } catch {
        case e: SQLException =>
          Logger.debug("Failed in Content.scala / ownershipList")
          Logger.debug(e.getMessage())
          List[(Content, User)]()
      }
    }
  }

  /**
   * Create a content from fixture data
   * @param data Fixture data
   * @return The content
   */
  def fromFixture(data: (String, Symbol, Long, String, String, Boolean, Boolean, Boolean, Option[String], String, Boolean)): Content =
    Content(None, data._1, data._2, data._3, data._4, data._5, data._6, data._7, data._8, data._9, data._10, data._11)

  def setSetting(content: Content, setting: String, arguments: Seq[String]) {
    if (arguments.size == 0) { return }
    DB.withConnection { implicit connection =>
      try {
        val cid = content.id.get
        SQL(s"delete from $settingTable where contentId = {cid} and setting = {setting}")
          .on('cid -> cid, 'setting -> setting)
          .execute()
        val params = arguments.map { arg =>
          List(
            NamedParameter.symbol('cid -> cid),
            NamedParameter.symbol('setting -> setting),
            NamedParameter.symbol('arg -> arg)
          )
        }
        BatchSql(
          s"insert into $settingTable (contentId, setting, argument) values ({cid}, {setting}, {arg})",
          params.head, params.tail:_*
        ).execute()
      } catch {
        case e: SQLException =>
          Logger.debug("Failed in Content.scala / setSetting")
          Logger.debug(e.getMessage())
      }
    }
  }

  def addSetting(content: Content, setting: String, arguments: Seq[String]) {
    if (arguments.size == 0) { return }
    DB.withConnection { implicit connection =>
      try {
        val cid = content.id.get
        val params = arguments.map { arg =>
          List(
            NamedParameter.symbol('cid -> cid),
            NamedParameter.symbol('setting -> setting),
            NamedParameter.symbol('arg -> arg)
          )
        }
        BatchSql(
          s"insert into $settingTable (contentId, setting, argument) values ({cid}, {setting}, {arg})",
          params.head, params.tail:_*
        ).execute()
      } catch {
        case e: SQLException =>
          Logger.debug("Failed in Content.scala / addSetting")
          Logger.debug(e.getMessage())
      }
    }
  }

  def removeSetting(content: Content, setting: String, arguments: Seq[String]) {
    if (arguments.size == 0) { return }
    DB.withConnection { implicit connection =>
      try {
        val cid = content.id.get
        val params = arguments.map { arg =>
          List(
            NamedParameter.symbol('cid -> cid),
            NamedParameter.symbol('setting -> setting),
            NamedParameter.symbol('arg -> arg)
          )
        }
        BatchSql(
          s"delete from $settingTable where contentId = {cid} and setting = {setting} and argument = {arg}",
          params.head, params.tail:_*
        ).execute()
      } catch {
        case e: SQLException =>
          Logger.debug("Failed in Content.scala / removeSetting")
          Logger.debug(e.getMessage())
      }
    }
  }

  def getSetting(content: Content, setting: String): List[String] =
    DB.withConnection { implicit connection =>
      try {
        SQL(s"select argument from $settingTable where contentId = {cid} and setting = {setting}")
          .on('cid -> content.id.get, 'setting -> setting)
          .as(get[String](settingTable + ".argument") *)
      } catch {
        case e: SQLException =>
          Logger.debug("Failed in Content.scala / getSetting")
          Logger.debug(e.getMessage())
          List[String]()
      }
    }

  def getSettingMap(content: Content): Map[String, List[String]] =
    DB.withConnection { implicit connection =>
      try {
        val plist = SQL(s"select setting, argument from $settingTable where contentId = {id}")
          .on('id -> content.id)
          .as(
            get[String](settingTable + ".setting") ~
            get[String](settingTable + ".argument") map {
              case setting ~ argument => setting -> argument
            } *
          )

        (Map[String, List[String]]() /: plist) { (acc, next) =>
          next match {
            case (setting, argument) =>
              if(acc.contains(setting)) acc + (setting -> (argument :: acc(setting)))
              else acc + (setting -> List(argument))
          }
        }
      } catch {
        case e: SQLException =>
          Logger.debug("Failed in Content.scala / getSettingMap")
          Logger.debug(e.getMessage())
          Map[String, List[String]]()
      }
    }

  /**
   * Increments the views for a specific content item
   * @param id The content id
   */
  def incrementViews(id: Long) =
    DB.withConnection { implicit connection =>
      try {
        SQL(s"update $tableName set views = views + 1 where id = {id}")
          .on('id -> id).executeUpdate()
      } catch {
        case e: SQLException =>
          Logger.debug("Failed to increment content view count")
          Logger.debug(e.getMessage())
      }
    }


    def expire(id: Long) =
      DB.withConnection { implicit connection =>
        try {
          SQL(s"update $tableName set enabled = 0 where id = {id}")
            .on('id -> id).executeUpdate()
        } catch {
          case e: SQLException =>
            Logger.debug("Failed to set content as expired")
            Logger.debug(e.getMessage())     
        }
      }
}
