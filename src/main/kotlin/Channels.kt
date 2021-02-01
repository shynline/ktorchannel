import io.ktor.application.*
import io.ktor.http.cio.websocket.*
import io.ktor.response.*
import io.ktor.util.*
import io.ktor.websocket.*
import io.lettuce.core.RedisClient
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import java.time.Duration
import java.util.*
import kotlin.collections.HashMap
import kotlin.coroutines.CoroutineContext

class Channels @ExperimentalWebSocketExtensionApi constructor(
    val webSockets: WebSockets,
    redisHost: String
) : CoroutineScope {
    private val parent: CompletableJob = Job()
    override val coroutineContext: CoroutineContext
        get() = parent

    private val subscriptions : HashMap<String, (String, String) -> Unit> = hashMapOf()
    private val redisClient: RedisClient = RedisClient.create(redisHost)
    private val connection = redisClient.connectPubSub()

    init {
        require(redisHost.isNotBlank())
        val reactive = connection.reactive()
        reactive.subscribe("default").subscribe()
        reactive.observeChannels().doOnNext {
            handleMessage(it.message)
        }.subscribe()
    }

    private fun handleMessage(dataString: String){
        val data = dataString.deserializeToHashMap()
        println("received: $dataString")
        val channel = data["channel"] ?: return
        val type = data["type"] ?: return
        val encodedMessage = data["encodedMessage"] ?: return
        subscriptions[channel]?.invoke(String(Base64.getDecoder().decode(encodedMessage)), type)
    }

    internal fun subscribe(channel: String, callback: (message: String, type: String) -> Unit){
        subscriptions[channel] = callback
    }
    internal fun unsubscribe(channel: String){
        subscriptions.remove(channel)
    }

    internal fun sendToChannelByte(channel: String, message: ByteArray){
        val encodedMessage = Base64.getEncoder().encode(message)
        val data = HashMap<String, String>().apply {
            put("channel", channel)
            put("type", "byte")
            put("message", String(encodedMessage))
        }
        println("Publishing: ${data.serialize()}")
        connection.sync().publish("default", data.serialize())
    }

    internal fun sendToChannelString(channel: String, message: String){
        val encodedMessage = Base64.getEncoder().encode(message.toByteArray())
        val data = HashMap<String, String>().apply {
            put("channel", channel)
            put("type", "text")
            put("message", String(encodedMessage))
        }
        println("Publishing: ${data.serialize()}")
        connection.sync().publish("default", data.serialize())
    }

    private fun shutdown() {
        parent.complete()
    }

    /**
     * Websockets configuration options
     */
    class ChannelsOptions {

        var redisHost: String = ""

        /**
         * Duration between pings or `0` to disable pings
         */
        var pingPeriodMillis: Long = 0

        var pingPeriod: Duration
            get() = Duration.ofMillis(pingPeriodMillis)
            set(new) {
                pingPeriodMillis = new.toMillis()
            }

        /**
         * write/ping timeout after that a connection will be closed
         */
        var timeoutMillis: Long = 15000L


        var timeout: Duration
            get() = Duration.ofMillis(timeoutMillis)
            set(new) {
                timeoutMillis = new.toMillis()
            }

        /**
         * Maximum frame that could be received or sent
         */
        var maxFrameSize: Long = Long.MAX_VALUE

        /**
         * Whether masking need to be enabled (useful for security)
         */
        var masking: Boolean = false


        @OptIn(ExperimentalWebSocketExtensionApi::class)
        internal var extensionsBlock: (WebSocketExtensionsConfig.() -> Unit) = {}
        /**
         * Configure WebSocket extensions.
         */
        @ExperimentalWebSocketExtensionApi
        fun extensions(block: WebSocketExtensionsConfig.() -> Unit) {
            extensionsBlock = block
        }
    }

    /**
     * Feature installation object.
     */
    companion object Feature : ApplicationFeature<Application, ChannelsOptions, Channels> {
        override val key: AttributeKey<Channels> = AttributeKey("Channels")

        override fun install(
            pipeline: Application,
            configure: ChannelsOptions.() -> Unit
        ): Channels {
            val config = ChannelsOptions().also(configure)

            val webSockets = pipeline.install(WebSockets) {
                pingPeriod = Duration.ofMillis(config.pingPeriodMillis)
                timeout = Duration.ofMillis(config.timeoutMillis)
                maxFrameSize = config.maxFrameSize
                masking = config.masking
                @OptIn(ExperimentalWebSocketExtensionApi::class)
                extensions{
                    config.extensionsBlock(this)
                }
            }
            @OptIn(ExperimentalWebSocketExtensionApi::class)
            val channels =  Channels(webSockets, config.redisHost)

            pipeline.environment.monitor.subscribe(ApplicationStopPreparing) {
                channels.shutdown()
            }

            return channels
        }
    }
}
