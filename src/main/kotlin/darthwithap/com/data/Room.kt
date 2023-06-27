package darthwithap.com.data

import io.ktor.websocket.*
import kotlinx.coroutines.isActive

class Room(
  val name: String,
  var maxPlayers: Int,
  var players: List<Player> = listOf()
) {

  private var phaseChangedListener: ((Phase) -> Unit)? = null
  var phase = Phase.WAITING_FOR_PLAYERS
    set(value) {
      synchronized(field) {
        field = value
        phaseChangedListener?.let { change ->
          change.invoke(value)
        }
      }
    }

  private fun setPhaseChangedListener(listener: (Phase)-> Unit) {
    phaseChangedListener = listener
  }

  init {
    setPhaseChangedListener { phase ->
      when (phase) {
        Phase.WAITING_FOR_PLAYERS -> waitingForPlayers()
        Phase.WAITING_FOR_START -> waitingForPlayers()
        Phase.NEW_ROUND -> waitingForPlayers()
        Phase.GAME_RUNNING -> waitingForPlayers()
        Phase.SHOW_WORD -> waitingForPlayers()
        Phase.ENDED -> waitingForPlayers()
      }
    }
  }

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
