package darthwithap.com.plugins

import darthwithap.com.routes.roomRouting
import io.ktor.server.routing.*
import io.ktor.server.application.*

fun Application.configureRouting() {
  routing {
    roomRouting()
  }
}
