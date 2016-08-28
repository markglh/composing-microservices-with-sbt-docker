package com.markglh.blog

import java.util.UUID

import com.markglh.blog.BeaconRepo.BeaconByLocation
import io.getquill.{CassandraAsyncContext, SnakeCase}

import scala.concurrent.Future


object BeaconRepo {

  case class BeaconByLocation(locationId: UUID, beaconId: UUID)

}

//TODO really we should use the generic `CassandraContext` here, for testing etc...
class BeaconRepo()(implicit dbContext: CassandraAsyncContext[SnakeCase]) {

  import dbContext._

  def beaconByLocation(locationId: UUID): Future[List[BeaconByLocation]] = {
    dbContext.run(
      quote {
        query[BeaconByLocation].filter(_.locationId == lift(locationId))
      })
  }
}
