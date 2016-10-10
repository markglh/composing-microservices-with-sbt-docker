package com.markglh.blog

import java.time.{LocalDateTime, ZoneOffset}
import java.util.UUID

import com.markglh.blog.TrackingRepo.UsersByBeacon
import com.typesafe.scalalogging.LazyLogging
import io.circe._
import io.circe.generic.auto._
import io.circe.java8.time._
import io.getquill.{CassandraAsyncContext, SnakeCase}
import org.http4s._
import org.http4s.dsl._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try


object TrackingService extends LazyLogging {

  implicit def circeJsonDecoder[A](implicit decoder: Decoder[A]) = org.http4s.circe.jsonOf[A]

  implicit def circeJsonEncoder[A](implicit encoder: Encoder[A]) = org.http4s.circe.jsonEncoderOf[A]

  //TODO re-use...
  object LocalDateTimeVar {
    def unapply(timeLogged: String): Option[LocalDateTime] = {
      if (!timeLogged.isEmpty)
        Try(LocalDateTime.ofEpochSecond(timeLogged.toLong, 0, ZoneOffset.UTC)).toOption
      else
        None
    }
  }

  def routes(trackingRepo: TrackingRepo[CassandraAsyncContext[SnakeCase]])(implicit ec: ExecutionContext) = HttpService {

    case request@GET -> Root / "tracking" / "beacons" / beaconId / LocalDateTimeVar(timeLogged) =>
      logger.debug(s"****Querying for beaconId:$beaconId & timeLogged:$timeLogged")
      Ok(trackingRepo.findUsersByBeaconWithinHour(UUID.fromString(beaconId), timeLogged))
  }

}
