import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob

abstract class WebSocketConsumer {
    val webSocketScope: CoroutineScope by lazy { internalChannelListener.getCoroutineScope() }
    open suspend fun onByteMessage(message: ByteArray){}
    open suspend fun onTextMessage(message: String){}
    open suspend fun onClose(reason: CloseReason?) {}
    open suspend fun onError(reason: CloseReason?, throwable: Throwable?) {}
    open suspend fun onConnect() {}
    internal lateinit var internalChannelListener: ChannelListener
    val channel: String by lazy { internalChannelListener.getChannelId() }
    suspend fun forwardByte(channel: String, message: ByteArray){
        internalChannelListener.forwardByte(channel, message)
    }
    suspend fun sendByte(message: ByteArray){
        internalChannelListener.sendByte(message)
    }
    suspend fun forwardText(channel: String, message: String){
        internalChannelListener.forwardText(channel, message)
    }
    suspend fun sendText(message: String){
        internalChannelListener.sendText(message)
    }
    suspend fun close(reason: CloseReason){
        internalChannelListener.internalClose(reason)
    }
}