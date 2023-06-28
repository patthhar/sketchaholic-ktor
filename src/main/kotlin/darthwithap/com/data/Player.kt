package darthwithap.com.data

import darthwithap.com.data.models.Ping
import darthwithap.com.gson
import darthwithap.com.server
import darthwithap.com.utils.Constants.PING_FREQUENCY
import io.ktor.websocket.*
import kotlinx.coroutines.*

data class Player(
  val username: String,
  var socket: WebSocketSession,
  val clientId: String,
  var isDrawing: Boolean = false,
  var score: Int = 0,
  var rank: Int = 0
) {
  private var ping: Job? = null

  private var pingTime = 0L
  private var pongTime = 0L

  var isOnline = true

  @OptIn(DelicateCoroutinesApi::class)
  fun startPinging() {
    ping?.cancel()
    ping = GlobalScope.launch {
      while (true) {
        sendPing()
        delay(PING_FREQUENCY)
      }
    }
  }

  private suspend fun sendPing() {
    pingTime = System.currentTimeMillis()
    socket.send(Frame.Text(gson.toJson(Ping())))
    delay(PING_FREQUENCY)
    if (pingTime - pongTime > PING_FREQUENCY) {
      isOnline = false
      server.playerLeft(clientId)
      ping?.cancel()
    }
  }

  fun receivedPong() {
    pongTime = System.currentTimeMillis()
    isOnline = true
  }

  fun disconnect() {
    ping?.cancel()
  }
}
