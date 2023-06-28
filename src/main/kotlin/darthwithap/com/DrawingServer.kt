package darthwithap.com

import darthwithap.com.data.Player
import darthwithap.com.data.Room
import java.util.concurrent.ConcurrentHashMap

class DrawingServer {
  val rooms = ConcurrentHashMap<String, Room>()
  val players = ConcurrentHashMap<String, Player>()

  fun playerJoined(player: Player) {
    players[player.clientId] = player
  }

  fun playerLeft(clientId: String, immediatelyDisconnect: Boolean = false) {
    val playersRoom = getRoomWithClientId(clientId)
    if (immediatelyDisconnect) {
      playersRoom?.removePlayer(clientId)
      players.remove(clientId)
    }
  }

  fun getRoomWithClientId(clientId: String): Room? {
    val filterRooms = rooms.filterValues { room ->
      room.players.find {
        it.clientId == clientId
      } != null
    }
    return if (filterRooms.values.isEmpty()) null
    else filterRooms.values.toList()[0]
  }
}