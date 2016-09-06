package com.markglh.blog

import java.time.{LocalDateTime, ZoneOffset}
import java.util.UUID

import com.markglh.blog.TrackingRepo.UsersByBeacon
import io.circe._
import io.circe.generic.auto._
import io.circe.java8.time._
import io.getquill.{CassandraAsyncContext, SnakeCase}
import org.http4s._
import org.http4s.client.blaze.PooledHttp1Client
import org.http4s.dsl._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try


/**
  * Created by markh on 27/08/2016.
  */
object TrackingService {

  val client = PooledHttp1Client()

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

  def routes(trackingRepo: TrackingRepo[CassandraAsyncContext[SnakeCase]])(implicit ec: ExecutionContext) = HttpService {


    case request@GET -> Root / "tracking" / "users" / beaconId / LocalDateTimeVar(timeLogged) =>
      val response: Future[List[UsersByBeacon]] = trackingRepo.findUsersByBeaconWithinHour(UUID.fromString(beaconId), timeLogged)
      Ok(response)
  }

}
