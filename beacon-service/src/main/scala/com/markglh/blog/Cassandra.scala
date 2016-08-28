package com.markglh.blog

import java.util.UUID

import io.getquill.{CassandraAsyncContext, SnakeCase}

trait Cassandra {
  implicit val ctx = new CassandraAsyncContext[SnakeCase]("cassandra")

  import ctx._

  implicit val decodeUUID = mappedEncoding[String, UUID](UUID.fromString)
  implicit val encodeUUID = mappedEncoding[UUID, String](_.toString)
}
