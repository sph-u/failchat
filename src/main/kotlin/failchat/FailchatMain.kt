package failchat

import com.github.salomonbrys.kodein.instance
import failchat.chat.ChatMessageRemover
import failchat.chat.ChatMessageSender
import failchat.chat.handlers.IgnoreFilter
import failchat.emoticon.Emoticon
import failchat.emoticon.EmoticonLoader
import failchat.emoticon.EmoticonManager
import failchat.emoticon.EmoticonStorage
import failchat.emoticon.EmoticonStoreOptions
import failchat.goodgame.GgEmoticonLoader
import failchat.gui.GuiLauncher
import failchat.peka2tv.Peka2tvEmoticonLoader
import failchat.reporter.EventAction
import failchat.reporter.EventCategory
import failchat.reporter.EventReporter
import failchat.twitch.BttvGlobalEmoticonLoader
import failchat.twitch.TwitchEmoticonLoader
import failchat.util.submitWithCatch
import failchat.viewers.ViewersCountWsHandler
import failchat.ws.server.ClientConfigurationWsHandler
import failchat.ws.server.DeleteWsMessageHandler
import failchat.ws.server.IgnoreWsMessageHandler
import failchat.ws.server.WsServer
import javafx.application.Application
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.asCoroutineDispatcher
import kotlinx.coroutines.experimental.launch
import org.apache.commons.configuration2.Configuration
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.nio.file.Path
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

object FailchatMain

val wsServerAddress = InetSocketAddress(InetAddress.getLoopbackAddress(), 10880)

private val log: Logger = LoggerFactory.getLogger(FailchatMain::class.java)

fun main(args: Array<String>) {
    checkForAnotherInstance()

    configureLogging(args)

    handleProgramArguments(args)

    // Start GUI
    // Тред блокируется. Javafx приложение лучше запустить раньше(а не а конце main()) для отзывчивости интерфейса
    thread(name = "GuiLauncher") { Application.launch(GuiLauncher::class.java) }


    // Websocket server
    val wsServer: WsServer = kodein.instance()
    val config: Configuration = kodein.instance()
    wsServer.apply {
        setOnMessage("client-configuration", ClientConfigurationWsHandler(kodein.instance<ChatMessageSender>()))
        setOnMessage("viewers-count", kodein.instance<ViewersCountWsHandler>())
        setOnMessage("delete-message", DeleteWsMessageHandler(kodein.instance<ChatMessageRemover>()))
        setOnMessage("ignore-author", IgnoreWsMessageHandler(kodein.instance<IgnoreFilter>(), config))
    }
    wsServer.start()


    // Reporter
    val backgroundExecutor = kodein.instance<ScheduledExecutorService>("background")
    scheduleReportTasks(backgroundExecutor)


    // Load emoticons in background thread
    loadEmoticonsAsync(backgroundExecutor)

    log.info("Application started. Version: {}. Working directory: {}", config.getString("version"),
            kodein.instance<Path>("workingDirectory").toAbsolutePath())
}

private fun checkForAnotherInstance() {
    try {
        val serverSocket = ServerSocket(wsServerAddress.port, 10, wsServerAddress.address)
        serverSocket.close()
    } catch (e: Exception) {
        System.err.println("Another instance is running at $wsServerAddress. Exception: $e")
        System.exit(0)
    }
}

private fun handleProgramArguments(args: Array<String>) {
    //todo make it in a good way
    val config: Configuration = kodein.instance()
    if (args.contains("--disable-release-checker")) {
        config.setProperty("update-checker.enabled", false)
    }
    if (args.contains("--disable-reporter")) {
        config.setProperty("reporter.enabled", false)
    }
}

private fun loadEmoticonsAsync(executor: ExecutorService): Future<*> = executor.submitWithCatch {
    val manager: EmoticonManager = kodein.instance()
    val storage: EmoticonStorage = kodein.instance()
    val loadersAndOptions: List<Pair<EmoticonLoader<out Emoticon>, EmoticonStoreOptions>> = listOf(
            kodein.instance<Peka2tvEmoticonLoader>() to EmoticonStoreOptions(true, false),
            kodein.instance<GgEmoticonLoader>() to EmoticonStoreOptions(true, false),
            kodein.instance<BttvGlobalEmoticonLoader>() to EmoticonStoreOptions(true, false),
            kodein.instance<TwitchEmoticonLoader>() to EmoticonStoreOptions(true, true)
    )

    loadersAndOptions.forEach {
        try {
            manager.loadInStorage(storage, it.first, it.second)
        } catch (e: Exception) {
            log.warn("Exception during loading emoticons for origin {}", it.first.origin, e)
        }
    }
}

/**
 * Посылает репорт General.AppLaunch и репортит по расписанию General.Heartbeat.
 * */
private fun scheduleReportTasks(executor: ScheduledExecutorService) {
    val reporter = kodein.instance<EventReporter>()
    val dispatcher = executor.asCoroutineDispatcher()

    launch(dispatcher) {
        try {
            reporter.report(EventCategory.GENERAL, EventAction.APP_LAUNCH)
        } catch (t: Throwable) {
            log.warn("Failed to report event {}.{}", EventCategory.GENERAL, EventAction.APP_LAUNCH, t)
        }
    }

    executor.scheduleAtFixedRate({
        launch(Unconfined) {
            try {
                reporter.report(EventCategory.GENERAL, EventAction.HEARTBEAT)
            } catch (t: Throwable) {
                log.warn("Failed to report event {}.{}", EventCategory.GENERAL, EventAction.HEARTBEAT, t)
            }
        }
    }, 5, 5, TimeUnit.MINUTES)
}
