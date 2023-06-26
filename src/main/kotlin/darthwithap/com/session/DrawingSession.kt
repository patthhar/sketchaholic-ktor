package darthwithap.com.session

import io.ktor.server.sessions.*

data class DrawingSession(
  val clientId: String,
  val sessionId: String
) {}