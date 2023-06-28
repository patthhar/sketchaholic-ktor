package darthwithap.com.data

import darthwithap.com.data.models.*
import darthwithap.com.gson
import darthwithap.com.server
import darthwithap.com.utils.Constants.GUESS_SCORE_DEFAULT
import darthwithap.com.utils.Constants.GUESS_SCORE_DRAWER_MULTIPLIER
import darthwithap.com.utils.Constants.GUESS_SCORE_FOR_DRAWING_PLAYER
import darthwithap.com.utils.Constants.GUESS_SCORE_PERCENTAGE_MULTIPLIER
import darthwithap.com.utils.Constants.PENALTY_NOBODY_GUESSED
import darthwithap.com.utils.getRandomWords
import darthwithap.com.utils.matchesWord
import darthwithap.com.utils.transformToUnderscores
import darthwithap.com.utils.words
import io.ktor.websocket.*
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

class Room(
  val name: String,
  var maxPlayers: Int,
  var players: List<Player> = listOf()
) {

  private var timerJob: Job? = null
  private var drawingPlayer: Player? = null
  private var winningPlayers = listOf<String>()
  private var word: String? = null
  private var currWords: List<String>? = null
  private var drawingPlayerIndex = 0
  private var phaseStartTime = 0L

  private val playerRemoveJobs = ConcurrentHashMap<String, Job>()
  private val leftPlayers = ConcurrentHashMap<String, Pair<Player, Int>>()

  private var phaseChangedListener: ((Phase) -> Unit)? = null
  var phase = Phase.WAITING_FOR_PLAYERS
    set(value) {
      synchronized(field) {
        field = value
        phaseChangedListener?.invoke(value)
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
    sendWordToPlayerMidGame(player)
    broadcastPlayerStates()
    broadcast(gson.toJson(announcement))

    return player
  }

  @OptIn(DelicateCoroutinesApi::class)
  fun removePlayer(clientId: String) {
    val player = players.find { it.clientId == clientId } ?: return
    val index = players.indexOf(player)
    leftPlayers[clientId] = player to index
    players = players - player
    playerRemoveJobs[clientId] = GlobalScope.launch {
      delay(PLAYER_REMOVE_COOLDOWN)
      val playerToRemove = leftPlayers[clientId]
      leftPlayers.remove(clientId)
      playerToRemove?.let {
        players = players - it.first
      }
      playerRemoveJobs.remove(clientId)
    }.job
    val announcement = Announcement(
      message = "${player.username} left the room",
      timestamp = System.currentTimeMillis(),
      announcementType = Announcement.TYPE_PLAYER_LEFT
    )
    GlobalScope.launch {
      broadcastPlayerStates()
      broadcast(gson.toJson(announcement))
      if (players.size == 1) {
        phase = Phase.WAITING_FOR_PLAYERS
        timerJob?.cancel()
      } else if (players.isEmpty()) {
        kill()
        server.rooms.remove(name)
      }
    }
  }

  @OptIn(DelicateCoroutinesApi::class)
  private fun timeAndNotify(ms: Long) {
    timerJob?.cancel()
    timerJob = GlobalScope.launch {
      phaseStartTime = System.currentTimeMillis()
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

  @OptIn(DelicateCoroutinesApi::class)
  private fun newRound() {
    currWords = getRandomWords()
    val newWords = NewWords(currWords!!)
    nextDrawingPlayerInOrder()
    GlobalScope.launch {
      broadcastPlayerStates()
      drawingPlayer?.socket?.send(Frame.Text(gson.toJson(newWords)))
      timeAndNotify(TIMER_NEW_ROUND_TO_GAME_RUNNING)
    }
  }

  @OptIn(DelicateCoroutinesApi::class)
  private fun gameRunning() {
    winningPlayers = listOf()
    val wordToSend = word ?: currWords?.random() ?: words.random()
    val underscoresWord = wordToSend.transformToUnderscores()
    val drawingUsername = (drawingPlayer ?: players.random()).username
    val gameRunningStateForDrawingPlayer = GameRunningState(
      drawingUsername,
      wordToSend
    )
    val gameRunningStateForGuessingPlayers = GameRunningState(
      drawingUsername,
      underscoresWord
    )
    GlobalScope.launch {
      broadcastToAllExcept(
        gson.toJson(gameRunningStateForGuessingPlayers),
        drawingPlayer?.clientId ?: players.random().clientId
      )
      drawingPlayer?.socket?.send(Frame.Text(gson.toJson(gameRunningStateForDrawingPlayer)))

      timeAndNotify(TIMER_GAME_RUNNING_TO_SHOW_WORD)
      val phaseChange = PhaseChange(
        Phase.GAME_RUNNING, TIMER_GAME_RUNNING_TO_SHOW_WORD, drawingPlayer?.username
      )
    }
  }

  @OptIn(DelicateCoroutinesApi::class)
  private fun showWord() {
    GlobalScope.launch {
      if (winningPlayers.isEmpty()) {
        drawingPlayer?.let {
          it.score -= PENALTY_NOBODY_GUESSED
        }
      }
      broadcastPlayerStates()
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

  suspend fun checkAndNotifyPlayers(message: ChatMessage): Boolean {
    if (isCorrectGuess(message)) {
      val guessingTime = System.currentTimeMillis() - phaseStartTime
      val timePercentage = 1f - guessingTime / TIMER_GAME_RUNNING_TO_SHOW_WORD
      val score = GUESS_SCORE_DEFAULT + GUESS_SCORE_PERCENTAGE_MULTIPLIER * timePercentage
      val player = players.find { it.username == message.from }
      player?.let {
        it.score += score.toInt()
      }
      drawingPlayer?.let {
        it.score += ((GUESS_SCORE_FOR_DRAWING_PLAYER / players.size) + (1 - timePercentage) * GUESS_SCORE_DRAWER_MULTIPLIER).toInt()
      }
      broadcastPlayerStates()
      val announcement = Announcement(
        message = "${message.from} guessed the word",
        timestamp = System.currentTimeMillis(),
        announcementType = Announcement.TYPE_PLAYER_GUESSED_WORD
      )
      broadcast(gson.toJson(announcement))
      if (addWinningPlayer(message.from)) {
        val roundOverAnnouncement = Announcement(
          "All players guessed the word!",
          System.currentTimeMillis(),
          Announcement.TYPE_ALL_PLAYERS_GUESSED_WORD
        )
        broadcast(gson.toJson(roundOverAnnouncement))
      }
      return true
    }
    return false
  }

  private suspend fun broadcastPlayerStates() {
    val playerList = players.sortedByDescending { it.score }.map {
      PlayerData(it.username, it.isDrawing, it.score, it.rank)
    }
    playerList.forEachIndexed { index, playerData ->
      playerData.rank = index + 1
    }
    broadcast(gson.toJson(PlayersDataList(playerList)))
  }

  private suspend fun sendWordToPlayerMidGame(player: Player) {
    val delay = when (phase) {
      Phase.WAITING_FOR_PLAYERS -> TIMER_WAITING_FOR_PLAYERS
      Phase.WAITING_FOR_START -> TIMER_WAITING_FOR_START_TO_NEW_ROUND
      Phase.NEW_ROUND -> TIMER_NEW_ROUND_TO_GAME_RUNNING
      Phase.GAME_RUNNING -> TIMER_GAME_RUNNING_TO_SHOW_WORD
      Phase.SHOW_WORD -> TIMER_SHOW_WORD_TO_NEW_ROUND
      else -> 0L
    }
    val phaseChange = PhaseChange(phase, delay, drawingPlayer?.username)

    word?.let { currWord ->
      drawingPlayer?.let {
        val wordToSend = if (player.isDrawing || phase == Phase.SHOW_WORD) {
          currWord
        } else {
          currWord.transformToUnderscores()
        }
        val gameRunningState = GameRunningState(drawingPlayer?.username, wordToSend)
        player.socket.send(Frame.Text(gson.toJson(gameRunningState)))
      }
    }
    player.socket.send(Frame.Text(gson.toJson(phaseChange)))
  }

  private fun addWinningPlayer(username: String): Boolean {
    if (username != drawingPlayer?.username) winningPlayers += username
    if (winningPlayers.size == players.size - 1) {
      phase = Phase.SHOW_WORD
      return true
    }
    return false
  }

  private fun isCorrectGuess(guess: ChatMessage): Boolean {
    return guess.matchesWord(word ?: return false)
        && !winningPlayers.contains(guess.from) && guess.from != drawingPlayer?.username
        && phase == Phase.GAME_RUNNING
  }

  private fun nextDrawingPlayerInOrder() {
    drawingPlayer?.isDrawing = false
    if (players.isEmpty()) return
    drawingPlayer = if (drawingPlayerIndex <= players.size - 1) {
      players[drawingPlayerIndex]
    } else {
      players.last()
    }

    if (drawingPlayerIndex < players.size - 1) drawingPlayerIndex++
    else drawingPlayerIndex = 0
  }

  private fun kill() {
    playerRemoveJobs.values.forEach {
      it.cancel()
      timerJob?.cancel()
    }
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
    const val PLAYER_REMOVE_COOLDOWN = 60000L

    const val UPDATE_TIME_FREQUENCY = 1000L
    const val TIMER_WAITING_FOR_PLAYERS = 1000000L
    const val TIMER_WAITING_FOR_START_TO_NEW_ROUND = 30000L
    const val TIMER_NEW_ROUND_TO_GAME_RUNNING = 20000L
    const val TIMER_GAME_RUNNING_TO_SHOW_WORD = 60000L
    const val TIMER_SHOW_WORD_TO_NEW_ROUND = 10000L
  }
}
