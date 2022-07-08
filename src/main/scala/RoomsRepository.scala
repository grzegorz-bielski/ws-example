package com.example

import cats.*
import cats.effect.*
import cats.syntax.all.*
import org.http4s.circe.*
import org.http4s.*
import io.circe.*
import io.circe.syntax.*
import org.http4s.dsl.*

trait RoomsRepistory[F[_]]:
  def get(name: String): F[Option[Room[F]]]
  def getAll: F[Vector[Room[F]]]
  def update(room: Room[F]): F[Unit]

object RoomsRepistory:
  def inMemory[F[_]: Sync]: F[RoomsRepistory[F]] =
    Ref[F].of(Vector.empty[Room[F]]).map { ref =>
      new RoomsRepistory[F]:
        def get(name: String): F[Option[Room[F]]] =
          ref.get.map(_.find(_.name == name))
        def getAll = ref.get
        def update(room: Room[F]) =
          ref.update(
            _.filterNot(_.name == room.name) :+ room
          )
    }
