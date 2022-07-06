package com.example

import cats.*
import org.http4s.*
import org.http4s.dsl.*

final class HealthCheckService[F[_]: Monad] extends Http4sDsl[F]:
  def routes: HttpRoutes[F] =
    HttpRoutes.of[F] { case GET -> Root / "ping" => Ok("pong") }
