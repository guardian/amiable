import software.amazon.awssdk.services.ses.SesAsyncClient
import com.gu.googleauth.AuthAction
import config.AmiableConfigProvider
import controllers.{AMIable, Healthcheck, Login, routes}
import filters.TargetFilter
import metrics.CloudWatch
import play.api.ApplicationLoader.Context
import play.api.Logging
import play.api.libs.logback.LogbackLoggerConfigurator
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc.AnyContent
import play.filters.HttpFiltersComponents
import router.Routes
import services.{Agents, Metrics}
import services.notification.{
  AWSMailClient,
  Notifications,
  ScheduledNotificationRunner
}

class AppLoader extends play.api.ApplicationLoader with Logging {

  override def load(context: Context): play.api.Application = {

    val startupStartTime = System.currentTimeMillis()
    new LogbackLoggerConfigurator().configure(context.environment)

    logger.info("""

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
    val app = components.application
    val startupEndTime = System.currentTimeMillis()
    val startupDuration = startupEndTime - startupStartTime
    logger.info(s"Application startup completed in $startupDuration ms")
    app
  }

}

class AppComponents(context: Context)
    extends play.api.BuiltInComponentsFromContext(context)
    with HttpFiltersComponents
    with AhcWSComponents
    with controllers.AssetsComponents {

  override lazy val httpFilters =
    new TargetFilter(sys.env)(
      materializer,
      executionContext
    ) +: super.httpFilters

  lazy val amiableConfigProvider =
    new AmiableConfigProvider(wsClient, configuration, httpConfiguration)

  val cloudwatch = new CloudWatch()

  val agents = new Agents(
    amiableConfigProvider,
    applicationLifecycle,
    actorSystem,
    environment,
    cloudwatch
  )
  val metrics = new Metrics(
    cloudwatch,
    amiableConfigProvider.shouldCreateCloudwatchMetrics,
    amiableConfigProvider.cloudwatchWriteNamespace,
    amiableConfigProvider.cloudwatchSecurityHqNamespace,
    agents,
    applicationLifecycle,
    actorSystem
  )

  lazy val amazonMailClient: SesAsyncClient =
    AWSMailClient.amazonMailClient
  lazy val awsMailClient = new AWSMailClient(amazonMailClient)(executionContext)

  lazy val scheduledNotificationRunner = new ScheduledNotificationRunner(
    awsMailClient,
    environment,
    amiableConfigProvider
  )
  lazy val notifications = new Notifications(
    amiableConfigProvider,
    environment,
    applicationLifecycle,
    scheduledNotificationRunner
  )

  private val authAction = new AuthAction[AnyContent](
    amiableConfigProvider.googleAuthConfig,
    routes.Login.startLogin,
    controllerComponents.parsers.default
  )(executionContext)

  lazy val amiableController = new AMIable(
    controllerComponents,
    amiableConfigProvider,
    agents,
    notifications,
    authAction
  )(executionContext)
  lazy val healthCheckController = new Healthcheck(controllerComponents)
  lazy val loginController = new Login(
    controllerComponents,
    amiableConfigProvider,
    wsClient,
    amiableConfigProvider.googleAuthConfig
  )(executionContext)

  lazy val router = new Routes(
    httpErrorHandler,
    amiableController,
    healthCheckController,
    loginController,
    assets
  )

}
