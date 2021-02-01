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

        val exceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            launch {
                webSocketInstance.onError(null, throwable)
                close(CloseReason(CloseReason.Codes.INTERNAL_ERROR, "Internal error"))
            }
        }
        val webSocketJob: CompletableJob = SupervisorJob()
        val webSocketScope = CoroutineScope(channels.coroutineContext + webSocketJob + exceptionHandler)

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
        channels.subscribe(channelId) { message, type ->
            launch {
                println("Incoming from channel")
                if (type == "text") {
                    println(message)
                    send(Frame.Text(message))
                } else if (type == "byte") {
                    send(Frame.Binary(true, Base64.getDecoder().decode(message)))
                }
            }
        }// forward check + exception handling

        try {
            // Notify consumer #onConnect
            webSocketInstance.onConnect()
            for (frame in incoming) {
                when (frame) {
                    is Frame.Binary -> webSocketScope.launch { webSocketInstance.onByteMessage(frame.readBytes()) }
                    is Frame.Text -> webSocketScope.launch { webSocketInstance.onTextMessage(frame.readText()) }
                    else -> {
                    }
                }
                yield()
                println("yield $isActive")
            }
        } catch (e: ClosedReceiveChannelException) {
            println("Close exception")
        } catch (e: CancellationException) {
            println("Ignored CancellationException")
        } catch (e: Throwable) {
            println("Exception $e")
            webSocketScope.launch { webSocketInstance.onError(closeReason.await(), e) }
            webSocketJob.complete()
        } finally {
            println("Finally")
            webSocketScope.launch { webSocketInstance.onClose(closeReason.await()) }
            webSocketJob.complete()
            channels.unsubscribe(channelId)
        }
    }
}