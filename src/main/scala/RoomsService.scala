package com.example

import cats.effect.*
import cats.syntax.all.*
import org.http4s.circe.*
import org.http4s.*
import io.circe.*
import io.circe.syntax.*
import org.http4s.dsl.*

final case class Room(name: String) derives Codec.AsObject

final class RoomsService[F[_]: Concurrent] extends Http4sDsl[F]: 
  given EntityDecoder[F, Room] = jsonOf[F, Room]
    // todo: use ref
  private var rooms = collection.mutable.Map[String, Room]()

  def routes: HttpRoutes[F] =
    HttpRoutes.of[F] {
      case GET -> Root / "rooms" / room =>
        rooms
          .get(room)
          .map(_.asJson)
          .fold(NotFound(s"Room $room not found"))(Ok(_))

      case req @ POST -> Root / "rooms" =>
        for
          room <- req.as[Room]
          _ = rooms.update(room.name, room)
          res <- Created(room.asJson)
        yield res
    }