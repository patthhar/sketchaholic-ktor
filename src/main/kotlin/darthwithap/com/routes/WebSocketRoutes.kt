package darthwithap.com.routes

import com.google.gson.JsonParser
import darthwithap.com.data.Room
import darthwithap.com.data.models.BaseModel
import darthwithap.com.data.models.ChatMessage
import darthwithap.com.data.models.DrawData
import darthwithap.com.gson
import darthwithap.com.server
import darthwithap.com.session.DrawingSession
import darthwithap.com.utils.Constants.TYPE_CHAT_MESSAGE
import darthwithap.com.utils.Constants.TYPE_DRAW_DATA
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach

fun Route.webSocketRouting() {
  route("/ws/draw") {
    standardWebSocket { socket, clientId, message, payload ->
      when(payload) {
        is DrawData -> {
          val room = server.rooms[payload.roomName] ?: return@standardWebSocket
          if (room.phase == Room.Phase.GAME_RUNNING) {
            room.broadcastToAllExcept(message, clientId)
          }
        }
        is ChatMessage -> {

        }
      }
    }
  }
}

fun Route.standardWebSocket(
  handleFrame: suspend (
    socket: DefaultWebSocketServerSession,
    clientId: String,
    message: String,
    payload: BaseModel
  ) -> Unit
) {
  webSocket {
    val session = call.sessions.get<DrawingSession>()
    if (session == null) {
      close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No session"))
      return@webSocket
    }
    try {
      incoming.consumeEach { frame ->
        if (frame is Frame.Text) {
          val message = frame.readText()
          val jsonObject = JsonParser.parseString(message).asJsonObject
          val type = when (jsonObject.get("type").asString) {
            TYPE_CHAT_MESSAGE -> ChatMessage::class.java
            TYPE_DRAW_DATA -> DrawData::class.java
            else -> BaseModel::class.java
          }
          val payload = gson.fromJson(message, type)
          handleFrame(this, session.clientId, message, payload)
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    } finally {
      // TODO: Handle logic when player disconnects
    }
  }
}