package darthwithap.com.data.models

import darthwithap.com.utils.Constants.TYPE_CHOSEN_WORD

data class ChosenWord(
  val chosenWord: String,
  val room: String
): BaseModel(TYPE_CHOSEN_WORD)
