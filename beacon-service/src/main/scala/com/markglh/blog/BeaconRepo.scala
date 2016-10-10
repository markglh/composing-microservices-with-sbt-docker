package com.markglh.blog

import java.util.UUID

import com.markglh.blog.BeaconRepo.BeaconsByLocation
import io.getquill.{CassandraAsyncContext, SnakeCase}

import scala.concurrent.{ExecutionContext, Future}


object BeaconRepo {

  case class BeaconsByLocation(locationId: UUID, beaconId: UUID, beaconName: String)

}

//TODO really we should use the generic `CassandraContext` here, for testing etc...
//TODO figure out the type foo weirdness around the above
class BeaconRepo[Repo <: CassandraAsyncContext[SnakeCase]]()(implicit dbContext: Repo, ec: ExecutionContext) {

  import dbContext._

  private implicit val decodeUUID = mappedEncoding[String, UUID](UUID.fromString)
  private implicit val encodeUUID = mappedEncoding[UUID, String](_.toString)

  def findBeaconByLocation(locationId: UUID): Future[List[BeaconsByLocation]] = {
    dbContext.run(
      quote {
        query[BeaconsByLocation].filter(_.locationId == lift(locationId))
      })
  }
}
