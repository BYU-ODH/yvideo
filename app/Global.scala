import models.{Setting, HelpPage, HomePageContent, User}
import play.api.{Logger, GlobalSettings}
import play.api.mvc.RequestHeader
import play.api.mvc.Results.InternalServerError
import service.EmailTools
import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global

object Global extends GlobalSettings {

  override def onStart(app: play.api.Application) {
    if (app.mode.toString != "Test") {
      // If there are no users or collections then create all the fixtures
      if (User.list.isEmpty || models.Collection.list.isEmpty) {
        Logger.info("Creating fixtures")
        Fixtures.create()
      }

      if (HomePageContent.list.isEmpty) {
        Fixtures.createHomePageContent()
      }

      if (HelpPage.list.isEmpty) {
        Fixtures.createHelpPages()
      }

      Fixtures.setupSetting()
    } else {
      Logger.debug("Skipping fixtures in test mode")
    }
  }

  override def onError(request: RequestHeader, ex: Throwable) = {
    EmailTools.sendAdminNotificationEmail("notifications.notifyOn.error", ex.toString)
    Future { InternalServerError(views.html.application.error(request, ex)) }
  }

}
