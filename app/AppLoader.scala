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
  lazy val agents = new Agents(amiableConfigProvider, applicationLifecycle, actorSystem, environment)

  lazy val metrics = new Metrics(environment, agents, applicationLifecycle)

  lazy val messagesApi: MessagesApi = new DefaultMessagesApi(environment, configuration, new DefaultLangs(configuration))

  lazy val amiableConfigProvider = new AmiableConfigProvider(wsClient, configuration)

  lazy val awsMailClient = new AWSMailClient(amazonSimpleEmailServiceAsync)

  lazy val scheduledNotificationRunner = new ScheduledNotificationRunner(awsMailClient, environment, amiableConfigProvider)

  lazy val notifications = new Notifications(amiableConfigProvider, environment, applicationLifecycle, scheduledNotificationRunner)

  lazy val amiableController = new AMIable(amiableConfigProvider, agents, notifications)(defaultContext)

  lazy val healthCheckController = new Healthcheck

  lazy val loginController = new Login(amiableConfigProvider, wsClient)(defaultContext)

  lazy val assets = new Assets(httpErrorHandler)

  lazy val router = new Routes(httpErrorHandler, amiableController, healthCheckController, loginController, assets)
}
