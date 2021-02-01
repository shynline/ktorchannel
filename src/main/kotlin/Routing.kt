import io.ktor.application.*
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import java.util.*
import kotlin.reflect.KClass

fun Route.channels(path: String, consumerClass: KClass<out WebSocketConsumer>) {
    val channels = application.feature(Channels)

    webSocket(path = path) {
        // Create a unique channel id
        val channelId = UUID.randomUUID().toString()

        // Instantiating a user provided consumer class which represent a single socket life cycle
        val webSocketInstance = consumerClass.constructors.first().call()

        val webSocketJob : CompletableJob = SupervisorJob()
        val webSocketScope = CoroutineScope(channels.coroutineContext + webSocketJob)

        // Configuring the consumer channel listener
        // Note that the reason we don't use constructor is to make base class constructor clean for the user
        // It is basically the user commands which we have to react upon
        webSocketInstance.internalChannelListener = object : ChannelListener {

            override fun getCoroutineScope(): CoroutineScope {
                return webSocketScope
            }

            override suspend fun forwardByte(channel: String, message: ByteArray) {
                channels.sendToChannelByte(channel, message)
            }

            override suspend fun forwardText(channel: String, message: String) {
                channels.sendToChannelString(channel, message)
            }

            override suspend fun sendByte(message: ByteArray) {
                send(Frame.Binary(true, message))
            }

            override suspend fun sendText(message: String) {
                send(Frame.Text(message))
            }

            override suspend fun internalClose(reason: CloseReason) {
                close(reason)
            }

            override fun getChannelId(): String {
                return channelId
            }
        }


        // Subscribe this connection
        // Here we receive all forwarded messages
        channels.subscribe(channelId){message, type ->
            launch {
                val messageByteArray = Base64.getDecoder().decode(message)
                if (type == "text") {
                    send(Frame.Text(true, messageByteArray))
                } else if (type == "byte"){
                    send(Frame.Binary(true, messageByteArray))
                }
            }
        }

        // Notify consumer #onConnect
        webSocketInstance.onConnect()
        try {
            for (frame in incoming) {
                when (frame) {
                    is Frame.Binary -> webSocketInstance.onByteMessage(frame.readBytes())
                    is Frame.Text -> webSocketInstance.onTextMessage(frame.readText())
                    else -> {}
                }
            }
        } catch (e: ClosedReceiveChannelException) {

        } catch (e: Throwable) {
            webSocketInstance.onError(closeReason.await(), e)
        } finally {
            webSocketInstance.onClose(closeReason.await())
            webSocketJob.complete()
            channels.unsubscribe(channelId)
        }
    }
}