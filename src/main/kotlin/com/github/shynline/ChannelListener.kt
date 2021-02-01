package com.github.shynline

import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.CoroutineScope

internal interface ChannelListener {
    suspend fun forwardByte(channel: String, message: ByteArray)
    suspend fun forwardText(channel: String,  message: String)
    suspend fun sendByte(message: ByteArray)
    suspend fun sendText(message: String)
    suspend fun internalClose(reason: CloseReason)
    fun getChannelId(): String
    fun getCoroutineScope(): CoroutineScope
}