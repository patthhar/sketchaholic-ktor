package darthwithap.com.data.models

import darthwithap.com.utils.Constants.TYPE_CURRENT_ROUND_DRAW_INPUTS

data class DrawInputs(
  val data: List<String>
): BaseModel(TYPE_CURRENT_ROUND_DRAW_INPUTS)
