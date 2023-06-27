package darthwithap.com.data

import darthwithap.com.data.models.Announcement
import darthwithap.com.data.models.ChosenWord
import darthwithap.com.data.models.PhaseChange
import darthwithap.com.gson
import darthwithap.com.utils.Constants.PENALTY_NOBODY_GUESSED
import io.ktor.websocket.*
import kotlinx.coroutines.*

class Room(
  val name: String,
  var maxPlayers: Int,
  var players: List<Player> = listOf()
) {

  private var timerJob: Job? = null
  private var drawingPlayer: Player? = null
  private var winningPlayers = listOf<String>()
  private var word: String? = null

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

  private fun setPhaseChangedListener(listener: (Phase) -> Unit) {
    phaseChangedListener = listener
  }

  init {
    setPhaseChangedListener { phase ->
      when (phase) {
        Phase.WAITING_FOR_PLAYERS -> waitingForPlayers()
        Phase.WAITING_FOR_START -> waitingForStart()
        Phase.NEW_ROUND -> newRound()
        Phase.GAME_RUNNING -> gameRunning()
        Phase.SHOW_WORD -> showWord()
        Phase.ENDED -> ended()
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

  @OptIn(DelicateCoroutinesApi::class)
  private fun timeAndNotify(ms: Long) {
    timerJob?.cancel()
    timerJob = GlobalScope.launch {
      val phaseChange = PhaseChange(
        phase,
        ms,
        drawingPlayer?.username
      )
      repeat((ms / UPDATE_TIME_FREQUENCY).toInt()) {
        if (it != 0) {
          phaseChange.phase = null
        }
        broadcast(gson.toJson(phase))
        phaseChange.time -= UPDATE_TIME_FREQUENCY
        delay(UPDATE_TIME_FREQUENCY)
      }
      phase = when (phase) {
        Phase.ENDED -> Phase.WAITING_FOR_START
        Phase.WAITING_FOR_START -> Phase.NEW_ROUND
        Phase.NEW_ROUND -> Phase.GAME_RUNNING
        Phase.GAME_RUNNING -> Phase.SHOW_WORD
        Phase.SHOW_WORD -> Phase.NEW_ROUND
        else -> Phase.WAITING_FOR_PLAYERS
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

  fun containsPlayer(username: String): Boolean {
    return players.find { it.username == username } != null
  }

  fun setWordAndSwitchToGameRunning(word: String) {
    this.word = word
    phase = Phase.GAME_RUNNING
  }

  @OptIn(DelicateCoroutinesApi::class)
  private fun waitingForPlayers() {
    GlobalScope.launch {
      val phaseChange = PhaseChange(
        Phase.WAITING_FOR_PLAYERS,
        TIMER_WAITING_FOR_PLAYERS
      )
      broadcast(gson.toJson(phaseChange))
    }
  }

  @OptIn(DelicateCoroutinesApi::class)
  private fun waitingForStart() {
    GlobalScope.launch {
      timeAndNotify(TIMER_WAITING_FOR_START_TO_NEW_ROUND)
      val phaseChange = PhaseChange(
        Phase.WAITING_FOR_PLAYERS,
        TIMER_WAITING_FOR_START_TO_NEW_ROUND
      )
      broadcast(gson.toJson(phaseChange))
    }
  }

  private fun newRound() {

  }

  private fun gameRunning() {

  }

  @OptIn(DelicateCoroutinesApi::class)
  private fun showWord() {
    GlobalScope.launch {
      if (winningPlayers.isEmpty()) {
        drawingPlayer?.let {
          it.score -= PENALTY_NOBODY_GUESSED
        }
      }
      word?.let {
        val chosenWord = ChosenWord(it, name)
        broadcast(gson.toJson(chosenWord))
      }
      timeAndNotify(TIMER_SHOW_WORD_TO_NEW_ROUND)
      val phaseChange = PhaseChange(Phase.SHOW_WORD, TIMER_SHOW_WORD_TO_NEW_ROUND)
      broadcast(gson.toJson(phaseChange))
    }
  }

  private fun ended() {

  }

  enum class Phase {
    WAITING_FOR_PLAYERS,
    WAITING_FOR_START,
    NEW_ROUND,
    GAME_RUNNING,
    SHOW_WORD,
    ENDED
  }

  companion object {
    const val UPDATE_TIME_FREQUENCY = 1000L
    const val TIMER_WAITING_FOR_PLAYERS = 1000000L
    const val TIMER_WAITING_FOR_START_TO_NEW_ROUND = 30000L
    const val TIMER_NEW_ROUND_TO_GAME_RUNNING = 20000L
    const val TIMER_GAME_RUNNING_TO_SHOW_WORD = 60000L
    const val TIMER_SHOW_WORD_TO_NEW_ROUND = 10000L
  }
}
