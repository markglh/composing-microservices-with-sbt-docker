package com.markglh.blog

import java.util.UUID

import com.typesafe.scalalogging.LazyLogging
import io.circe._
import io.circe.generic.auto._
import io.getquill.{CassandraAsyncContext, SnakeCase}
import org.http4s._
import org.http4s.client.blaze.PooledHttp1Client
import org.http4s.dsl._

import scala.concurrent.ExecutionContext


object BeaconService extends LazyLogging {


  val client = PooledHttp1Client()

  implicit def circeJsonDecoder[A](implicit decoder: Decoder[A]) = org.http4s.circe.jsonOf[A]

  implicit def circeJsonEncoder[A](implicit encoder: Encoder[A]) = org.http4s.circe.jsonEncoderOf[A]

  def routes(beaconRepo: BeaconRepo[CassandraAsyncContext[SnakeCase]])(implicit ec: ExecutionContext) = HttpService {

    case request@GET -> Root / "beacons" / "locations" / locationId =>
      logger.debug(s"****Querying for locationId:$locationId")
      Ok(beaconRepo.findBeaconByLocation(UUID.fromString(locationId)))
  }
}

