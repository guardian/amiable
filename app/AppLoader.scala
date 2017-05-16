import config.AmiableConfigProvider
import controllers.Assets
import controllers.{AMIable, Healthcheck, Login}
import play.api.ApplicationLoader.Context
import play.api._
import play.api.i18n.{DefaultLangs, DefaultMessagesApi, MessagesApi}
import play.api.libs.logback.LogbackLoggerConfigurator
import play.api.libs.ws.ahc.AhcWSComponents
import services.{Agents, Metrics}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import services.notification.{AWSMailClient, Notifications, ScheduledNotificationRunner}
import services.notification.AmazonSimpleEmailServiceAsyncFactory._

class AppLoader extends ApplicationLoader {
  override def load(context: Context): Application = {

    new LogbackLoggerConfigurator().configure(context.environment)

    println(
      """

                                                         .---.
                  __  __   ___   .--.          /|        |   |      __.....__
                 |  |/  `.'   `. |__|          ||        |   |  .-''         '.
                 |   .-.  .-.   '.--.          ||        |   | /     .-''"'-.  `.
            __   |  |  |  |  |  ||  |    __    ||  __    |   |/     /________\   \
         .:--.'. |  |  |  |  |  ||  | .:--.'.  ||/'__ '. |   ||                  |
        / |   \ ||  |  |  |  |  ||  |/ |   \ | |:/`  '. '|   |\    .-------------'
        `" __ | ||  |  |  |  |  ||  |`" __ | | ||     | ||   | \    '-.____...---.
         .'.''| ||__|  |__|  |__||__| .'.''| | ||\    / '|   |  `.             .'
        / /   | |_                   / /   | |_|/\'..' / '---'    `''-...... -'
        \ \._,\ '/                   \ \._,\ '/'  `'-'`
         `--'  `"                     `--'  `"
      """)

    val components = new AppComponents(context)
    components.application
  }
}

class AppComponents(context: Context) extends BuiltInComponentsFromContext(context) with AhcWSComponents {
  def agents = new Agents(amiableConfigProvider, applicationLifecycle, actorSystem, environment)

  def metrics = new Metrics(environment, agents, applicationLifecycle)

  def messagesApi: MessagesApi = new DefaultMessagesApi(environment, configuration, new DefaultLangs(configuration))

  def amiableConfigProvider = new AmiableConfigProvider(wsClient, configuration)

  def awsMailClient = new AWSMailClient(amazonSimpleEmailServiceAsync)

  def scheduledNotificationRunner = new ScheduledNotificationRunner(awsMailClient, environment, amiableConfigProvider)

  def notifications = new Notifications(amiableConfigProvider, environment, applicationLifecycle, scheduledNotificationRunner)

  def amiableController = new AMIable(amiableConfigProvider, agents, notifications)(defaultContext)

  def healthCheckController = new Healthcheck

  def loginController = new Login(amiableConfigProvider, wsClient)(defaultContext)

  def assets = new Assets(httpErrorHandler)

  lazy val router = new Routes(httpErrorHandler, amiableController, healthCheckController, loginController, assets)
}
