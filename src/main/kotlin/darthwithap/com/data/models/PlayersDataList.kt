package darthwithap.com.data.models

import darthwithap.com.utils.Constants.TYPE_PLAYERS_DATA_LIST

data class PlayersDataList(
  val players: List<PlayerData>
): BaseModel(TYPE_PLAYERS_DATA_LIST)
