package com.markglh.blog

import java.time.{LocalDateTime, ZoneOffset}
import java.util.{Date, UUID}

import com.markglh.blog.TrackingRepo.UsersByBeacon
import com.typesafe.scalalogging.LazyLogging
import io.getquill.{CassandraAsyncContext, SnakeCase}

import scala.concurrent.{ExecutionContext, Future}

object TrackingRepo extends App {

  case class UsersByBeacon(beaconId: UUID, timeLogged: LocalDateTime, userId: UUID, name: String)

}

class TrackingRepo[Repo <: CassandraAsyncContext[SnakeCase]]()(implicit dbContext: Repo, ec: ExecutionContext) extends LazyLogging {

  import dbContext._

  private implicit val decodeUUID = mappedEncoding[String, UUID](UUID.fromString)
  private implicit val encodeUUID = mappedEncoding[UUID, String](_.toString)

  //Quill uses java.util.Date - lets avoid this evil until we absolutely have to by mapping to Java 8 dates automatically
  private implicit val decodeLocalTime = mappedEncoding[Date, LocalDateTime](date =>
    LocalDateTime.ofInstant(date.toInstant, ZoneOffset.UTC))

  private implicit val encodeLocalTime = mappedEncoding[LocalDateTime, Date](time =>
    new Date(time.toEpochSecond(ZoneOffset.UTC) * 1000)) //Millis vs Seconds from epoch...


  //Quill doesn't seem to support range queries for dates out of the box (.isBefore/isAfter breaks it)
  //So we define an infix notation to get around this limitation
  private implicit class RichDateTime(a: LocalDateTime) {
    def >=(b: LocalDateTime) = quote(infix"$a >= $b".as[Boolean])

    def <(b: LocalDateTime) = quote(infix"$a < $b".as[Boolean])
  }

  def findUsersByBeaconWithinHour(beaconId: UUID, timeLogged: LocalDateTime): Future[List[UsersByBeacon]] = {
    val timeLoggedRoundedToHour = timeLogged.withMinute(0).withSecond(0)
    dbContext.run(
      quote {
        query[UsersByBeacon]
          .filter { result =>
            result.beaconId == lift(beaconId) &&
              result.timeLogged >= lift(timeLoggedRoundedToHour) &&
              result.timeLogged < lift(timeLoggedRoundedToHour.plusHours(1))
          }
      })
  }
}
