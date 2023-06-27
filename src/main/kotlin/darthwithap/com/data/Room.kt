package darthwithap.com.data

import darthwithap.com.data.models.Announcement
import darthwithap.com.gson
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

  suspend fun addPlayer(
    clientId: String,
    username: String,
    socket: WebSocketSession
  ): Player {
    val player = Player(username, socket, clientId)
    players += player

    if (players.size == 1) {
      phase = Phase.WAITING_FOR_PLAYERS
    } else if (players.size == 2 && phase == Phase.WAITING_FOR_PLAYERS) {
      phase = Phase.WAITING_FOR_START
      players = players.shuffled()
    } else if (phase == Phase.WAITING_FOR_START && players.size == maxPlayers) {
      phase = Phase.NEW_ROUND
      players = players.shuffled()
    }

    val announcement = Announcement(
      "$username joined",
      System.currentTimeMillis(),
      Announcement.TYPE_PLAYER_JOINED
    )
    broadcast(gson.toJson(announcement))

    return player
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
