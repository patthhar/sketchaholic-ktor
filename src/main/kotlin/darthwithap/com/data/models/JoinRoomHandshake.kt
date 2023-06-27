package darthwithap.com.data.models

import darthwithap.com.utils.Constants.TYPE_JOIN_ROOM_HANDSHAKE

data class JoinRoomHandshake(
  val username: String,
  val room: String,
  val clientId: String
): BaseModel(TYPE_JOIN_ROOM_HANDSHAKE)
