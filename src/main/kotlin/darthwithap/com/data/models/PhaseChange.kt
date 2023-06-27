package darthwithap.com.data.models

import darthwithap.com.data.Room
import darthwithap.com.utils.Constants.TYPE_PHASE_CHANGE

data class PhaseChange(
  var phase: Room.Phase? = null,
  var time: Long,
  val drawingPlayer: String? = null
): BaseModel(TYPE_PHASE_CHANGE)
