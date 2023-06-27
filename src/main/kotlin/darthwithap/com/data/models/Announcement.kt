package darthwithap.com.data.models

import darthwithap.com.utils.Constants.TYPE_ANNOUNCEMENT

data class Announcement(
  val message: String,
  val timestamp: Long,
  val announcementType: Int
): BaseModel(TYPE_ANNOUNCEMENT) {
  companion object {
    const val TYPE_PLAYER_GUESSED_WORD = 0
    const val TYPE_PLAYER_JOINED = 1
    const val TYPE_PLAYER_LEFT = 2
    const val TYPE_PLAYER_DISCONNECTED = 2
    const val TYPE_ALL_PLAYERS_GUESSED_WORD = 2
  }
}
