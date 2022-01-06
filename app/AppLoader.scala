import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceAsync
import com.gu.googleauth.AuthAction
import config.AmiableConfigProvider
import controllers.{AMIable, Healthcheck, Login, routes}
import metrics.CloudWatch
import play.api.ApplicationLoader.Context
import play.api.libs.logback.LogbackLoggerConfigurator
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc.AnyContent
import play.filters.HttpFiltersComponents
import router.Routes
import services.{Agents, Metrics}
import services.notification.{AWSMailClient, Notifications, ScheduledNotificationRunner}
import services.notification.AmazonSimpleEmailServiceAsyncFactory._

class AppLoader extends play.api.ApplicationLoader {

  override def load(context: Context): play.api.Application = {

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


class AppComponents(context: Context) extends play.api.BuiltInComponentsFromContext(context) with HttpFiltersComponents with AhcWSComponents with controllers.AssetsComponents {

  lazy val amiableConfigProvider = new AmiableConfigProvider(wsClient, configuration, httpConfiguration)

  val cloudwatch = new CloudWatch(amiableConfigProvider.stage)

  val agents = new Agents(amiableConfigProvider, applicationLifecycle, actorSystem, environment, cloudwatch)
  val metrics = new Metrics(cloudwatch, amiableConfigProvider.stage, agents, applicationLifecycle)


  lazy val amazonMailClient: AmazonSimpleEmailServiceAsync = amazonSimpleEmailServiceAsync

  lazy val awsMailClient = new AWSMailClient(amazonMailClient)(executionContext)

  lazy val scheduledNotificationRunner = new ScheduledNotificationRunner(awsMailClient, environment, amiableConfigProvider)
  lazy val notifications = new Notifications(amiableConfigProvider, environment, applicationLifecycle, scheduledNotificationRunner)


  private val authAction = new AuthAction[AnyContent](
    amiableConfigProvider.googleAuthConfig,
    routes.Login.startLogin,
    controllerComponents.parsers.default
  )(executionContext)

  lazy val amiableController = new AMIable(controllerComponents, amiableConfigProvider, agents, notifications, authAction)(executionContext)
  lazy val healthCheckController = new Healthcheck(controllerComponents)
  lazy val loginController = new Login(controllerComponents, amiableConfigProvider, wsClient, amiableConfigProvider.googleAuthConfig)(executionContext)

  lazy val router = new Routes(httpErrorHandler, amiableController, healthCheckController, loginController, assets)

}
