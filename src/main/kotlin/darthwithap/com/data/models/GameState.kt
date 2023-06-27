package darthwithap.com.data.models

import darthwithap.com.utils.Constants.TYPE_GAME_STATE

data class GameState(
  val drawingPlayer: String? = null,
  val word: String
): BaseModel(TYPE_GAME_STATE)
