package failchat.peka2tv

import com.google.common.collect.EvictingQueue
import failchat.core.Origin
import failchat.core.Origin.peka2tv
import failchat.core.chat.ChatClient
import failchat.core.chat.ChatClientStatus
import failchat.core.chat.ChatClientStatus.connected
import failchat.core.chat.ChatClientStatus.connecting
import failchat.core.chat.ChatClientStatus.offline
import failchat.core.chat.ChatClientStatus.ready
import failchat.core.chat.MessageFilter
import failchat.core.chat.MessageHandler
import failchat.core.chat.MessageIdGenerator
import failchat.core.chat.OriginStatus.CONNECTED
import failchat.core.chat.OriginStatus.DISCONNECTED
import failchat.core.chat.StatusMessage
import failchat.core.chat.handlers.BraceEscaper
import failchat.core.chat.handlers.ElementLabelEscaper
import failchat.core.emoticon.EmoticonFinder
import failchat.core.viewers.ViewersCountLoader
import failchat.exception.UnexpectedResponseException
import failchat.util.warn
import io.socket.client.IO
import io.socket.client.Socket
import okhttp3.OkHttpClient
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.Queue
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class Peka2tvChatClient(
        private val channelName: String,
        private val channelId: Long,
        private val socketIoUrl: String,
        private val okHttpClient: OkHttpClient,
        private val messageIdGenerator: MessageIdGenerator,
        private val emoticonFinder: EmoticonFinder
) : ChatClient<Peka2tvMessage>,
        ViewersCountLoader {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(Peka2tvChatClient::class.java)
        const val historySize = 50
    }

    override val status: ChatClientStatus get() = atomicStatus.get()
    override val origin = peka2tv

    override var onChatMessage: ((Peka2tvMessage) -> Unit)? = null
    override var onStatusMessage: ((StatusMessage) -> Unit)? = null
    override var onChatMessageDeleted: ((Peka2tvMessage) -> Unit)? = null


    private val socket = buildSocket()
    private val atomicStatus: AtomicReference<ChatClientStatus> = AtomicReference(ready)
    private val history: Queue<Peka2tvMessage> = EvictingQueue.create(historySize)
    private val historyLock: Lock = ReentrantLock()

    private val messageHandlers: List<MessageHandler<Peka2tvMessage>> = listOf(
            ElementLabelEscaper(),
            BraceEscaper(),
            Peka2tvHighlightHandler(channelName),
            Peka2tvEmoticonHandler(emoticonFinder)
    )
    private val messageFilters: List<MessageFilter<Peka2tvMessage>> = listOf(
            SourceFilter(),
            AnnounceMessageFilter()
    )


    override fun start() {
        if (!atomicStatus.compareAndSet(ready, connecting)) {
            return
        }

        socket.connect()
    }

    override fun stop() {
        atomicStatus.set(offline)
        socket.disconnect()
    }

    override fun loadViewersCount(): CompletableFuture<Int> {
        val viewersCountFuture = CompletableFuture<Int>()

        val obj = JSONObject()
        obj.put("channel", "stream/$channelId")

        socket.emit("/chat/channel/list", arrayOf(obj)) { responseObjects ->
            try {
                val response = responseObjects[0] as JSONObject
                val status = response.getString("status")
                if (status != "ok") {
                    viewersCountFuture.completeExceptionally(UnexpectedResponseException("Unexpected response status $status"))
                }

                viewersCountFuture.complete(response.getJSONObject("result").getInt("amount"))
            } catch (e: Exception) {
                log.warn(e) {
                    "Unexpected exception during updating peka2tv viewers count. response message: ${responseObjects.contentToString()}"
                }
                viewersCountFuture.completeExceptionally(e)
            }
        }

        return viewersCountFuture
    }

    private fun buildSocket(): Socket {
        IO.setDefaultOkHttpCallFactory(okHttpClient)
        IO.setDefaultOkHttpWebSocketFactory(okHttpClient)

        val options = IO.Options().apply {
            transports = arrayOf("websocket")
            forceNew = true
        }

        val socket = IO.socket(socketIoUrl, options)
        socket
                // Connect
                .on(Socket.EVENT_CONNECT) {
                    if (atomicStatus.get() == ChatClientStatus.offline) { //for quick close case
                        socket.disconnect()
                        return@on
                    }

                    val message = JSONObject().apply {
                        put("channel", "stream/$channelId")
                    }
                    socket.emit("/chat/join", arrayOf(message)) {
                        log.info("Connected to ${Origin.peka2tv}")
                        atomicStatus.set(connected)
                        onStatusMessage?.invoke(StatusMessage(peka2tv, CONNECTED))
                    }
                }

                // Disconnect
                .on(Socket.EVENT_DISCONNECT) {
                    atomicStatus.set(connecting)
                    log.info("Received disconnected event from peka2tv ")
                    onStatusMessage?.invoke(StatusMessage(peka2tv, DISCONNECTED))
                }

                // Message
                .on("/chat/message") { objects ->
                    // https://github.com/peka2tv/api/blob/master/chat.md#Новое-сообщение
                    val messageNode = objects[0] as JSONObject
                    val toNode: JSONObject? = if (!messageNode.isNull("to")) {
                        messageNode.getJSONObject("to")
                    } else {
                        null
                    }

                    val message = Peka2tvMessage(
                            id = messageIdGenerator.generate(),
                            peka2tvId = messageNode.getLong("id"),
                            fromUser = messageNode.getJSONObject("from").toUser(),
                            text = messageNode.getString("text"),
                            type = messageNode.getString("type"),
                            toUser = toNode?.toUser()
                    )

                    //filter message
                    messageFilters.forEach {
                        if (it.filterMessage(message)) return@on
                    }
                    //handle message
                    messageHandlers.forEach { it.handleMessage(message) }

                    historyLock.withLock { history.add(message) }
                    onChatMessage?.invoke(message)
                }

                // Message removal
                .on("/chat/message/remove") { objects ->
                    val removeMessage = objects[0] as JSONObject
                    val idToRemove = removeMessage.getLong("id")

                    val foundMessage = historyLock.withLock {
                        history.find { it.peka2tvId == idToRemove }
                    }
                    foundMessage?.let { onChatMessageDeleted?.invoke(it) }
                }

        return socket
    }

    private fun JSONObject.toUser(): Peka2tvUser {
        return Peka2tvUser(this.getString("name"), this.getLong("id"))
    }

}
