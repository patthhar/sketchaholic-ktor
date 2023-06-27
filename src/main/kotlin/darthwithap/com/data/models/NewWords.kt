package darthwithap.com.data.models

import darthwithap.com.utils.Constants.TYPE_NEW_WORDS

data class NewWords(
  val newWords: List<String>
): BaseModel(TYPE_NEW_WORDS)
