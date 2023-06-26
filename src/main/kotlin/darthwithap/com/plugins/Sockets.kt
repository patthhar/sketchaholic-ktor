package darthwithap.com.plugins

import io.ktor.server.websocket.*
import io.ktor.server.application.*

fun Application.configureSockets() {
  install(WebSockets)
}