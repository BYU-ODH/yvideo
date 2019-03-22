import collection.mutable.ListBuffer
import models._
import play.api.Logger
import scala.Some
import service.HashTools

object Fixtures {

  object data {
    val passwordHash = HashTools.sha256Base64("test123")
    val users = List(
      (passwordHash, 'password, "student1", Some("Student 1"), Some("s1@ayamel.byu.edu"), 'student),
      (passwordHash, 'password, "student2", Some("Student 2"), Some("s2@ayamel.byu.edu"), 'student),
      (passwordHash, 'password, "student3", Some("Student 3"), Some("s3@ayamel.byu.edu"), 'student),
      (passwordHash, 'password, "student4", Some("Student 4"), Some("s4@ayamel.byu.edu"), 'student),
      (passwordHash, 'password, "student5", Some("Student 5"), Some("s5@ayamel.byu.edu"), 'student),
      (passwordHash, 'password, "student6", Some("Student 6"), Some("s6@ayamel.byu.edu"), 'student),
      (passwordHash, 'password, "teacher1", Some("Teacher 1"), Some("t1@ayamel.byu.edu"), 'teacher),
      (passwordHash, 'password, "teacher2", Some("Teacher 2"), Some("t2@ayamel.byu.edu"), 'teacher),
      (passwordHash, 'password, "teacher3", Some("Teacher 3"), Some("t3@ayamel.byu.edu"), 'teacher),
      (passwordHash, 'password, "teacher4", Some("Teacher 4"), Some("t4@ayamel.byu.edu"), 'teacher),
      (passwordHash, 'password, "teacher5", Some("Teacher 5"), Some("t5@ayamel.byu.edu"), 'teacher),
      (passwordHash, 'password, "teacher6", Some("Teacher 6"), Some("t6@ayamel.byu.edu"), 'teacher),
      (passwordHash, 'password, "admin", Some("Admin"), Some("admin@ayamel.byu.edu"), 'admin)
    )

    val content = List(
      ("Dreyfus by Yves Duteil", 'video, 1L, "", "515c9b7d35e544681f000000", false, false, true, Some("adatestring"), "me@pm.me", true),
      ("Resource 2", 'video, 1L, "", "resource2", false, false, true, Some("adatestring"), "me@pm.me", true),
      ("Resource 3", 'video, 1L, "", "resource3", false, false, true, Some("adatestring"), "me@pm.me", true),
      ("Resource 4", 'video, 1L, "", "resource4", false, false, true, Some("adatestring"), "me@pm.me", true),
      ("Resource 5", 'video, 1L, "", "resource5", false, false, true, Some("adatestring"), "me@pm.me", true),
      ("Resource 6", 'video, 1L, "", "resource6", false, false, true, Some("adatestring"), "me@pm.me", true),
      ("Resource 7", 'video, 1L, "", "resource7", false, false, true, Some("adatestring"), "me@pm.me", true),
      ("Resource 8", 'video, 1L, "", "resource8", false, false, true, Some("adatestring"), "me@pm.me", true)
    )

    val collections = List(
      (1L, "Collection Test 1", true, false),
      (2L, "Collection Test 2", true, false),
      (3L, "Collection Test 3", true, false),
      (4L, "Collection Test 4", false, false),
      (5L, "Collection Test 5", false, true)
    )

    val courses = List(
      ("MATH"),
      ("CS"),
      ("HUM"),
      ("CHIN"),
      ("CS")
    )

    val collectionMembership = List(
      (0, 0, false, false),
      (1, 0, false, false),
      (1, 1, false, false),
      (2, 1, false, false),
      (2, 2, false, false),
      (3, 2, false, false),
      (3, 3, false, false),
      (4, 3, false, false),
      (0, 4, false, false),
      (0, 5, false, false),
      (0, 6, true, false),
      (1, 7, true, false),
      (2, 8, true, false),
      (3, 9, true, false),
      (4, 10, true, false),
      (0, 11, true, false)
    )

    val contentOwnership = List(
      (0, 0),
      (1, 1),
      (3, 2),
      (4, 3),
      (6, 4),
      (7, 5),
      (9, 6),
      (10, 7)
    )

  }

  val helpPages = List(
    "Updating account information",
    "Changing your password",
    "Merging accounts",
    "Notifications",
    "Searching",
    "Collection directory",
    "Joining collections",
    "Making announcements",
    "Adding content you own to a collection",
    "Content types",
    "Browsing content",
    "Viewing content",
    "Viewing captions and transcripts",
    "Translating",
    "Viewing annotations",
    "Sharing content",
    "Viewing content information",
    "Public content listing",
    "Uploading",
    "Adding hosted content",
    "Adding a YouTube video",
    "Adding a Brightcove video",
    "Create from existing resource",
    "Content settings",
    "Updating metadata",
    "Setting a thumbnail",
    "Setting the visibility",
    "Deleting content",
    "Ownership and availability",
    "Uploading captions",
    "Creating new annotations",
    "Editing existing annotations",
    "Publishing personal captions and annotations",
    "Becoming a teacher",
    "Creating a collection",
    "Adding content you don't own to a collection",
    "Setting collection captions and annotations",
    "How playlists work",
    "Creating a playlist",
    "Viewing a playlist"
  )

  val settings = List(
    ("notifications.emails", ""),
    ("notifications.notifyOn.error", "false"),
    ("notifications.notifyOn.errorReport", "true"),
    ("notifications.notifyOn.bugReport", "true"),
    ("notifications.notifyOn.rating", "false"),
    ("notifications.notifyOn.suggestion", "false"),
    ("notifications.users.emailOn.notification", "true"),
    ("help.gettingStartedContent", "0")
  )

  def create() {

    // Create the objects
    val users = new ListBuffer[User]()
    val content = new ListBuffer[Content]()
    val courses = new ListBuffer[Course]()
    val collections = new ListBuffer[Collection]()

    Logger.info("Creating user fixtures")
    data.users foreach {
      userData => users.append(User.fromFixture(userData).save)
    }

    Logger.info("Creating content fixtures")
    data.content foreach {
      contentData => content.append(Content.fromFixture(contentData).save)
    }

    Logger.info("Creating course fixtures")
    data.courses foreach {
      courseData => courses.append(Course.fromFixture(courseData).save)
    }

    Logger.info("Creating collection fixtures")
    data.collections foreach {
      collectionData => collections.append(Collection.fromFixture(collectionData).save)
    }

    Logger.info("Creating collection membership fixtures")
    data.collectionMembership.foreach {
      data => CollectionMembership(None, users(data._2).id.get, collections(data._1).id.get, data._3, data._4).save
    }
  }

  def createHomePageContent() {
    Logger.info("Creating home page content fixtures")

    HomePageContent(None, "Enrich your studies",
      "With Y-Video, increase your language speaking ability.",
      "", "", "/assets/images/home/byu-campus.jpg", active = true).save

    HomePageContent(None, "Pardon our dust",
      "We're working hard to provide language learning magic, so there may be some things don't work well, or at all. Please be patient. You will be rewarded as awesomeness occurs.",
      "", "", "/assets/images/home/construction.jpg", active = true).save
  }

  def createHelpPages() {
    Logger.info("Creating help pages")

    helpPages.foreach(title => HelpPage(None, title, "", "Uncategorized").save)
  }

  def setupSetting() {
    Logger.info("Checking settings...")

    settings.foreach { setting =>
      if (Setting.findByName(setting._1).isEmpty) {
        Logger.info("Adding setting: " + setting._1)
        Setting(None, setting._1, setting._2).save
      }
    }
  }
}
