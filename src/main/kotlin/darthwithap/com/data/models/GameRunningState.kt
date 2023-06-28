package darthwithap.com.data.models

import darthwithap.com.utils.Constants.TYPE_GAME_RUNNING_STATE

data class GameRunningState(
  val drawingPlayer: String? = null,
  val word: String
): BaseModel(TYPE_GAME_RUNNING_STATE)
