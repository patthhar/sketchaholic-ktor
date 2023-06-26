package darthwithap.com.data

import io.ktor.websocket.*
import kotlinx.coroutines.isActive

class Room(
  val name: String,
  var maxPlayers: Int,
  var players: List<Player> = listOf()
) {
  suspend fun broadcast(message: String) {
    players.forEach { player ->
      if (player.socket.isActive) {
        player.socket.send(Frame.Text(message))
      }
    }
  }

  suspend fun broadcastToAllExcept(message: String, clientId: String) {
    players.forEach { player ->
      if (player.clientId != clientId && player.socket.isActive) {
        player.socket.send(Frame.Text(message))
      }
    }
  }

}
