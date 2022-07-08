package com.example

import cats.*
import cats.effect.*
import cats.syntax.all.*
import cats.effect.std.Queue
import fs2.concurrent.Topic
import io.circe.*
import fs2.*

final class RoomsService[F[_]](
    val repo: RoomsRepistory[F],
    private val q: Queue[F, Room[F]]
):
  def subscribe(name: String)(using MonadThrow[F]): Stream[F, RoomMessage] =
    Stream.eval(getRoom(name)).flatMap(_.subscribe)

  def publish(name: String)(m: RoomMessage)(using MonadThrow[F]): F[Unit] = 
    getRoom(name).flatMap(_.publish(m))

  def getAllRooms: F[Vector[Room[F]]] = 
    repo.getAll

  def getRoom(name: String)(using MonadThrow[F]): F[Room[F]] =
    repo
      .get(name)
      .flatMap(
        MonadThrow[F]
          .fromOption(_, new IllegalArgumentException(s"No $name room"))
      )

  def createRoom(name: String)(using Concurrent[F]): F[Room[F]] =
    for
      room <- Room.create[F](name)
      _ <- repo.update(room)
      _ <- q.offer(room)
    yield room

  protected def deamon(using Concurrent[F]): F[Unit] =
    // todo: is that safe, should we track started fibers?
    def next: F[Unit] = q.take.flatMap { room =>
      Spawn[F].start(room.deamon).void
    } >> next

    Spawn[F].start(next).void

object RoomsService:
  def create[F[_]: Async]: F[RoomsService[F]] = 
    for
       repo <- RoomsRepistory.inMemory[F]
       q <- Queue.unbounded[F, Room[F]]
       service = RoomsService(repo, q)
       _ <- service.deamon
    yield service

final class Room[F[_]](
    val name: String,
    private val topic: Topic[F, RoomMessage],
    private val q: Queue[F, Option[RoomMessage]]
):
  def toView: RoomView = RoomView(name)
  def publish(m: RoomMessage): F[Unit] = q.offer(Some(m))
  def subscribe: Stream[F, RoomMessage] = topic.subscribe(100)

  def stop: F[Unit] = q.offer(None)

  // todo: use resource ?
  // close room after some time
  def deamon(using Concurrent[F]): F[Unit] =
    Stream
      .fromQueueNoneTerminated(q)
      .through(topic.publish)
      .compile
      .drain

object Room:
  def create[F[_]: Concurrent](name: String): F[Room[F]] =
    for
      q <- Queue.unbounded[F, Option[RoomMessage]]
      topic <- Topic[F, RoomMessage]
    yield Room(name, topic, q)

final case class RoomView(name: String) derives Codec.AsObject
final case class RoomMessage(id: String, content: String) derives Codec.AsObject