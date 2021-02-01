# KtorChannel
[![](https://jitpack.io/v/shynline/ktorchannel.svg)](https://jitpack.io/#shynline/ktorchannel)
[![Twitter](https://img.shields.io/badge/Twitter-%40shynline-red?style=flat)](http://twitter.com/shynline)

## Usage
Using KtorChannel is super easy.

-First you need to a consumer

A consumer is a subclass of `WebSocketConsumer` and it will be created once per WebSocket connection
```
class MyConsumer: WebSocketConsumer(){}
```
Each connection came with an ID which is unique for every connection that made through entire system.

The ID will change on each connect event and it's accessible through consumer's property `channel`.

The consumer provides a hand full of callbacks that you can override if you need to

`onConnect`

`onByteMessage`

`onTextMessage`

`onError`

`onClose`

It also somes with a `webSocketScope` that respects the WebSocket lifecycle

And for the last but not least it offers you a few functions as such

`suspend fun sendText(message: String)`

`suspend fun sendByte(message: ByteArray)`

`suspend fun forwardText(channel: String, message: String)`

`suspend fun forwardByte(channel: String, message: ByteArray)`

`suspend fun close(reason: CloseReason)`

The forward methods are used to send messages between sockets with `channel` id.

-Second you just simply install to your Ktor project

```
install(Channels){
        redisHost = "redis://host:port"
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
```

`redisHost` is the only parameter used in KtorChannel. the rest are Standard Ktor WebSocket params

-Third

Just like you do in Ktor WebSocket

```
routing {
        channels("/", MyConsumer::class)
    }
```



### Adding to your project
In order to use KtorChannel, you need to add Jitpack to your build.gradle.kts (or build.gradle):

```
// build.gradle.kts
repositories {
    maven { setUrl("https://jitpack.io") }
}
```
and then, add the dependency:

```
// build.gradle.kts
implementation("com.github.shynline:ktorchannel:{version}")
```

This project is inspired by [Django Channel](https://github.com/django/channels)
