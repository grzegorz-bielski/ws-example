package com.example

import cats.*
import cats.effect.*
import cats.syntax.all.*
import org.http4s.circe.*
import org.http4s.*
import io.circe.syntax.*
import org.http4s.dsl.*
import org.http4s.server.websocket.WebSocketBuilder
import fs2.*
import org.http4s.websocket.WebSocketFrame
import cats.effect.std.Queue
import fs2.concurrent.Topic
import org.http4s.server.websocket.WebSocketBuilder2

// todo: multiple rooms
// https://github.dev/MartinSnyder/http4s-chatserver/blob/master/src/main/scala/com/martinsnyder/chatserver/ChatRoutes.scala

final class RoomsRoutes[F[_]](rooms: RoomsService[F]) extends Http4sDsl[F]:
  def wsRoutes(using Async[F])(builder: WebSocketBuilder2[F]): HttpRoutes[F] =
    HttpRoutes.of[F] { case GET -> Root / "rooms" / roomName / "connect" / id =>
      val out: Stream[F, WebSocketFrame] =
        rooms.subscribe(roomName).map(msg => WebSocketFrame.Text(msg.content))
      val in: Pipe[F, WebSocketFrame, Unit] =
        _.evalMap(a => Sync[F].delay(scribe.info(a.toString)).as(a))
          .collect {
            // case Close(_) => ???
            case WebSocketFrame.Text(text, _) => RoomMessage(id, text)
          }
          .foreach(rooms.publish(roomName))

      builder.build(out, in)
    }

  def routes(using Async[F]): HttpRoutes[F] =
    given EntityDecoder[F, RoomView] = jsonOf[F, RoomView]

    HttpRoutes.of[F] {
      case GET -> Root / "rooms" / roomName =>
        for
          room <- rooms.getRoom(roomName)
          res <- Ok(room.toView.asJson)
        yield res

      case GET -> Root / "rooms" =>
        rooms.getAllRooms.map(_.map(_.toView).asJson).flatMap(Ok(_))

      case req @ POST -> Root / "rooms" =>
        for
          roomView <- req.as[RoomView]
          _ <- rooms.createRoom(roomView.name)
          res <- Created(roomView.asJson)
        yield res

    }

object RoomsRoutes:
  def setup[F[_]: Async]: F[RoomsRoutes[F]] =
    for service <- RoomsService.create[F]
    yield RoomsRoutes[F](service)
