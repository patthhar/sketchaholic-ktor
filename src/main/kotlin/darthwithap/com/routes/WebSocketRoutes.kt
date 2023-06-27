package darthwithap.com.routes

import com.google.gson.JsonParser
import darthwithap.com.data.Player
import darthwithap.com.data.Room
import darthwithap.com.data.models.*
import darthwithap.com.data.models.GameError.Companion.ERROR_ROOM_NOT_FOUND
import darthwithap.com.gson
import darthwithap.com.server
import darthwithap.com.session.DrawingSession
import darthwithap.com.utils.Constants.TYPE_ANNOUNCEMENT
import darthwithap.com.utils.Constants.TYPE_CHAT_MESSAGE
import darthwithap.com.utils.Constants.TYPE_DRAW_DATA
import darthwithap.com.utils.Constants.TYPE_JOIN_ROOM_HANDSHAKE
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach

fun Route.webSocketRouting() {
  route("/ws/draw") {
    standardWebSocket { socket, clientId, message, payload ->
      when (payload) {
        is JoinRoomHandshake -> {
          val room = server.rooms[payload.room]
          if (room == null) {
            val gameError = GameError(ERROR_ROOM_NOT_FOUND)
            socket.send(Frame.Text(gson.toJson(gameError)))
            return@standardWebSocket
          }
          val player = Player(
            payload.username,
            socket,
            payload.clientId
          )
          server.playerJoined(player)
          if (!room.containsPlayer(player.username)) {
            room.addPlayer(player.clientId, player.username, socket)
          }
        }

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
            TYPE_ANNOUNCEMENT -> Announcement::class.java
            TYPE_JOIN_ROOM_HANDSHAKE -> JoinRoomHandshake::class.java
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