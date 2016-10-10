package com.markglh.blog

import java.io.File

import com.typesafe.config.ConfigFactory
import org.http4s.server.ServerApp
import org.http4s.server.blaze.BlazeBuilder

import scala.concurrent.ExecutionContext

object Bootstrap extends ServerApp {
  implicit val executionContext = ExecutionContext.global

  // This looks to (an optional) boot-configuration.conf first, then falls back to application.conf for any values not found
  // The idea is we can easily define an env specific config at runtime using volume mounts at $APP_CONF
  lazy val config = ConfigFactory
    .parseFile(new File(s"${sys.env.getOrElse("APP_CONF", ".")}/boot-configuration.conf"))
    .withFallback(ConfigFactory.load())

  override def server(args: List[String]) = BlazeBuilder.bindHttp(80, "0.0.0.0")
    .mountService(AggregatorService.routes(config.getString("tracking.service.host"),
      config.getString("beacon.service.host")), "/")
    .start
}
