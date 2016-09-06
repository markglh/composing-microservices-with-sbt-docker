package com.markglh.blog

import java.util.UUID

import com.markglh.blog.BeaconRepo.BeaconByLocation
import io.circe._
import io.circe.generic.auto._
import io.getquill.{CassandraAsyncContext, SnakeCase}
import org.http4s.client.blaze.PooledHttp1Client
import org.http4s.dsl._
import org.http4s.{Uri, _}

import scala.concurrent.{ExecutionContext, Future}
import scalaz.concurrent.Task


/**
  * Created by markh on 27/08/2016.
  */
object BeaconService {

  val client = PooledHttp1Client()

  implicit def circeJsonDecoder[A](implicit decoder: Decoder[A]) = org.http4s.circe.jsonOf[A]

  implicit def circeJsonEncoder[A](implicit encoder: Encoder[A]) = org.http4s.circe.jsonEncoderOf[A]

  def routes(beaconRepo: BeaconRepo[CassandraAsyncContext[SnakeCase]])(implicit ec: ExecutionContext) = HttpService {

    case request@GET -> Root / "beacons" / "locations" / locationId =>

      println(s"****RetQuerying for $locationId")
      val beacons = beaconRepo.findBeaconByLocation(UUID.fromString(locationId))

      //println(s"****Returning $beacons")

      /*val beaconFut: Future[List[BeaconByLocation]] = Future {
        List(BeaconByLocation(UUID.randomUUID(), UUID.randomUUID()))
      }//.map(_.headOption.map(_.toString))*/

      Ok(beacons)
  }

  def callSelf(name: String): Task[String] = {
    val target = Uri.uri("http://localhost:8080/users") / name
    client.expect[String](target)
  }

  def getUser(username: String): Task[String] = ???

  val people = Vector("Michael", "Jessica", "Ashley", "Christopher")

}
