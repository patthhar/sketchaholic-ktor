package darthwithap.com.plugins

import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.application.*

fun Application.configureSerialization() {
  install(ContentNegotiation)
}
