package darthwithap.com.routes

import darthwithap.com.data.Room
import darthwithap.com.data.models.BasicApiResponse
import darthwithap.com.data.models.CreateRoomRequest
import darthwithap.com.server
import darthwithap.com.utils.Constants.MAX_ROOM_SIZE
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.roomRouting() {
  route("/api/room") {

    post {
      val roomReq = kotlin.runCatching {
        call.receiveNullable<CreateRoomRequest>()
      }.getOrNull()

      if (roomReq == null) {
        call.respondText(
          text = "Invalid room request",
          status = HttpStatusCode.BadRequest
        )
      } else {
        if (server.rooms[roomReq.name] != null) {
          call.respond(
            HttpStatusCode.OK,
            BasicApiResponse(
              isSuccess = false,
              message = "Room with name already exists"
            )
          )
          return@post
        }
        if (roomReq.maxPlayers < 2) {
          call.respond(
            HttpStatusCode.OK,
            BasicApiResponse(
              isSuccess = false,
              message = "Minimum room size is 2"
            )
          )
          return@post
        }
        if (roomReq.maxPlayers > MAX_ROOM_SIZE) {
          call.respond(
            HttpStatusCode.OK,
            BasicApiResponse(
              isSuccess = false,
              message = "Maximum room size is ${MAX_ROOM_SIZE}}"
            )
          )
          return@post
        }
        val room = Room(
          name = roomReq.name,
          maxPlayers = roomReq.maxPlayers
        )
        server.rooms[roomReq.name] = room
        call.respond(HttpStatusCode.OK, BasicApiResponse(true))
      }
    }
  }
}