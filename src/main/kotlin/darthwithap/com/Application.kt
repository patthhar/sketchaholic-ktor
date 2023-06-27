package darthwithap.com

import com.google.gson.Gson
import io.ktor.server.application.*
import darthwithap.com.plugins.*
import darthwithap.com.session.DrawingSession
import io.ktor.server.application.ApplicationCallPipeline.ApplicationPhase.Plugins
import io.ktor.server.sessions.*
import io.ktor.util.*

val server = DrawingServer()
val gson = Gson()

fun main(args: Array<String>): Unit =
  io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
  install(Sessions) {
    cookie<DrawingSession>("SESSION")
  }
  intercept(Plugins) {
    if (call.sessions.get<DrawingSession>() == null) {
      val clientId = call.parameters["client_id"] ?: ""
      call.sessions.set(
        DrawingSession(
          clientId = clientId,
          sessionId = generateNonce()
        )
      )
    }
  }

  configureSerialization()
  configureSockets()
  configureMonitoring()
  configureRouting()
}
