package com.example

import cats.*
import cats.effect.*
import cats.syntax.all.*
import org.http4s.circe.*
import org.http4s.*
import io.circe.*
import io.circe.syntax.*
import org.http4s.dsl.*

final case class Room(name: String) derives Codec.AsObject
final class RoomsService[F[_]](using roomsRepo: RoomsRepistory[F]) extends Http4sDsl[F]: 
  def routes(using Concurrent[F]): HttpRoutes[F] =
    given EntityDecoder[F, Room] = jsonOf[F, Room]

    HttpRoutes.of[F] {
      case GET -> Root / "rooms" / roomName =>
        for 
          room <- roomsRepo.get(roomName)
          res <- room
                .map(_.asJson)
                .fold(NotFound(s"Room $room not found"))(Ok(_))
        yield res

      case GET -> Root / "rooms" => 
        roomsRepo.getAll.map(_.asJson).flatMap(Ok(_))

      case req @ POST -> Root / "rooms" =>
        for
          room <- req.as[Room]
          _ <- roomsRepo.update(room)
          res <- Created(room.asJson)
        yield res
    }