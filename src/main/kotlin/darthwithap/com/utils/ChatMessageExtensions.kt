package darthwithap.com.utils

import darthwithap.com.data.models.ChatMessage
import io.ktor.util.*

fun ChatMessage.matchesWord(word: String) =
  message.toLowerCasePreservingASCIIRules().trim() == word.toLowerCasePreservingASCIIRules().trim()
