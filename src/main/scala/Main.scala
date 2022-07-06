package com.example

import cats.*
import cats.effect.*
import cats.syntax.all.*
import org.http4s.circe.*
import org.http4s.*
import io.circe.*
import io.circe.syntax.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.implicits.*
import org.http4s.server.*
import com.comcast.ip4s.Host
import org.http4s.ember.server.EmberServerBuilder
import com.comcast.ip4s.Port

object WSExample extends IOApp.Simple with Http4sDsl[IO]:
  def run =
    EmberServerBuilder
      .default[IO]
      .withHostOption(Host.fromString("0.0.0.0"))
      .withPort(Port.fromInt(8090).get)
      .withHttpApp(app)
      .withErrorHandler { e =>
        IO.println("Could not handle a request" -> e) *> InternalServerError()
      }
      .build
      .use(_ => IO.never)
      .void

  def app[F[_]: Concurrent]: HttpApp[F] =
    (RoomsService[F].routes combineK HealthCheckService[F].routes).orNotFound


