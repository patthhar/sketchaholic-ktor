package darthwithap.com.utils

import java.io.File

val words = readWordList("src/main/resources/wordlist.txt")

fun readWordList(filename: String): List<String> {
  val inputStream = File(filename).inputStream()
  val words = mutableListOf<String>()
  inputStream.bufferedReader().forEachLine {
    words.add(it)
  }
  return words
}

fun getRandomWords(amount: Int = 3): List<String> {
  var currAmount = 0
  val result = mutableListOf<String>()
  if (words.isNotEmpty()){
    while (currAmount < amount) {
      val word = words.random()
      if (!result.contains(word)) {
        result.add(word)
        currAmount++
      }
    }
  }
  return result
}

fun String.transformToUnderscores(): String {
  return toCharArray().map {
    if (it != ' ') '_' else ' '
  }.joinToString(" ")
}