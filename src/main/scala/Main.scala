package com.example

import cats.*
import cats.syntax.all.*
import cats.effect.*
import org.http4s.circe.*
import org.http4s.*
import io.circe.*
import io.circe.syntax.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.implicits.*
import org.http4s.server.*
import org.http4s.ember.server.EmberServerBuilder
import com.comcast.ip4s.Host
import com.comcast.ip4s.Port
import cats.effect.std.Queue
import fs2.concurrent.Topic

object WSExample extends IOApp.Simple with Http4sDsl[IO]:
  def run =
    for
      roomsRoutes <- RoomsRoutes.setup[IO]
      _ <- EmberServerBuilder
        .default[IO]
        .withHostOption(Host.fromString("0.0.0.0"))
        .withPort(Port.fromInt(8090).get)
        .withHttpWebSocketApp { wsb =>
          (roomsRoutes.routes
            .combineK(HealthCheckRoutes[IO].routes)
            .combineK(roomsRoutes.wsRoutes(wsb)))
            .orNotFound
        }
        .withErrorHandler { e =>
          IO.println("Could not handle a request" -> e) *> InternalServerError()
        }
        .build
        .use(_ => IO.never)
        .void
    yield ()
