package com.markglh.blog

import java.time.{LocalDateTime, ZoneOffset}
import java.util.UUID

import io.circe._
import io.circe.generic.auto._
import io.circe.java8.time._
import org.http4s.client.blaze.PooledHttp1Client
import org.http4s.dsl._
import org.http4s.{Uri, _}

import scala.concurrent.ExecutionContext
import scala.util.Try
import scalaz.concurrent.Task


object AggregatorService {

  case class BeaconsByLocation(locationId: UUID, beaconId: UUID, beaconName: String)

  case class UsersByBeacon(beaconId: UUID, timeLogged: LocalDateTime, userId: UUID, name: String)

  case class UsersByLocation(userId: UUID, name: String, timeLogged: LocalDateTime, beaconName: String)

  val client = PooledHttp1Client()

  //TODO re-use
  implicit def circeJsonDecoder[A](implicit decoder: Decoder[A]) = org.http4s.circe.jsonOf[A]

  implicit def circeJsonEncoder[A](implicit encoder: Encoder[A]) = org.http4s.circe.jsonEncoderOf[A]

  object LocalDateTimeVar {
    def unapply(timeLogged: String): Option[LocalDateTime] = {
      if (!timeLogged.isEmpty)
        Try(LocalDateTime.ofEpochSecond(timeLogged.toLong, 0, ZoneOffset.UTC)).toOption
      else
        None
    }
  }

  def callUri[A](uri: String)(implicit d: EntityDecoder[A]): Task[A] = {
    Task.fromDisjunction(Uri.fromString(uri).map {
      uri =>
        println(s"calling: ${uri.toString()}")
        client.expect[A](uri)
    }).flatMap(x => x)
  }

  def routes(trackingServiceHost: String, beaconServiceHost: String)(implicit ec: ExecutionContext) = HttpService {

    case request@GET -> Root / "aggregator" / "locations" / locationId / LocalDateTimeVar(timeLogged) =>

      //TODO this is horrible type-fu....
      val beaconTasks: Task[List[BeaconsByLocation]] =
        callUri[List[BeaconsByLocation]](s"http://$beaconServiceHost/beacons/locations/$locationId")

      val usersTypeFu: Task[List[Task[List[UsersByLocation]]]] = beaconTasks.map(_.map {
        beacon =>
          callUri[List[UsersByBeacon]](s"http://$trackingServiceHost/tracking/beacons/${beacon.beaconId}/${timeLogged.toEpochSecond(ZoneOffset.UTC)}")
            .map(_.map {
              userByBeacon =>
                UsersByLocation(userByBeacon.userId, userByBeacon.name, userByBeacon.timeLogged, beacon.beaconName)
            })
      })

      Ok(usersTypeFu.map { x =>
        Task.gatherUnordered(x).map(_.flatten)
      })
  }
}
