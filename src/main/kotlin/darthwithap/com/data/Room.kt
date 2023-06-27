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

  fun containsPlayer(username: String) : Boolean {
    return players.find { it.username == username } != null
  }

  private fun waitingForPlayers() {

  }
  private fun waitingForStart() {

  }
  private fun newRound() {

  }
  private fun gameRunning() {

  }
  private fun showWord() {

  }
  private fun ended() {

  }

  enum class Phase{
    WAITING_FOR_PLAYERS,
    WAITING_FOR_START,
    NEW_ROUND,
    GAME_RUNNING,
    SHOW_WORD,
    ENDED
  }
}
