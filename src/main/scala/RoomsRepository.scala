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
  def get(name: String): F[Option[Room]]
  def getAll: F[Vector[Room]]
  def update(room: Room): F[Unit]

object RoomsRepistory:
  def inMemory[F[_]: Sync]: F[RoomsRepistory[F]] =
    Ref[F].of(Rooms.empty).map { ref =>
      new RoomsRepistory[F]:
        def get(name: String) = ref.get.map(_.get(name))
        def getAll = ref.get.map(_.values.toVector)
        def update(room: Room) = ref.update(_.updated(room.name, room))
    }

type Rooms = Map[String, Room]
object Rooms:
  def empty: Rooms = Map.empty
