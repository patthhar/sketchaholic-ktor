package darthwithap.com.utils

object Constants {
  const val MAX_ROOM_SIZE = 10;
  const val TYPE_CHAT_MESSAGE = "TYPE_CHAT_MESSAGE"
  const val TYPE_DRAW_DATA = "TYPE_DRAW_DATA"
  const val TYPE_ANNOUNCEMENT = "TYPE_ANNOUNCEMENT"
  const val TYPE_JOIN_ROOM_HANDSHAKE = "TYPE_JOIN_ROOM_HANDSHAKE"
  const val TYPE_GAME_ERROR = "TYPE_GAME_ERROR"
  const val TYPE_PHASE_CHANGE = "TYPE_PHASE_CHANGE"
  const val TYPE_CHOSEN_WORD = "TYPE_CHOSEN_WORD"
  const val TYPE_GAME_RUNNING_STATE = "TYPE_GAME_RUNNING_STATE"
  const val TYPE_NEW_WORDS = "TYPE_NEW_WORDS"
  const val TYPE_PLAYERS_DATA_LIST = "TYPE_PLAYERS_DATA_LIST"
  const val TYPE_PING = "TYPE_PING"
  const val TYPE_PONG = "TYPE_PONG"

  const val PENALTY_NOBODY_GUESSED = 40
  const val GUESS_SCORE_DEFAULT = 80
  const val GUESS_SCORE_PERCENTAGE_MULTIPLIER = 60
  const val GUESS_SCORE_DRAWER_MULTIPLIER = 40
  const val GUESS_SCORE_FOR_DRAWING_PLAYER = 60

  const val PING_FREQUENCY = 2500L
}