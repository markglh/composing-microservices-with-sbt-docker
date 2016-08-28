package com.markglh.blog

import java.util.UUID

import com.markglh.blog.BeaconRepo.BeaconByLocation
import org.http4s.client.blaze.PooledHttp1Client
import org.http4s.dsl.{->, /, Root, _}
import org.http4s.{HttpService, Uri}

import scala.concurrent.{ExecutionContext, Future}
import scalaz.concurrent.Task

/**
  * Created by markh on 27/08/2016.
  */
object BeaconService {

  lazy val routes = {

    val beaconService = new BeaconRepo() with Cassandra

    val helloWorldService = HttpService {
      case GET -> Root / "hello" / name =>
        Ok(s"Hello, $name.")
    }

    val client = PooledHttp1Client()

    def routes(implicit executionContext: ExecutionContext = ExecutionContext.global) = HttpService {

      case request@GET -> Root / "users" / username =>
        //Ok(getUser(username))
        Ok(s"Hi $username")
      //Ok(Json.obj("origin" -> Json.fromString(request.remoteAddr.getOrElse("unknown"))))

      case request@GET -> Root / "beacons" =>
        val beacon: Future[List[BeaconByLocation]] = beaconService.beaconByLocation(UUID.randomUUID())
        println(beacon)
        Ok(s"Hi beacon!!!!")

      case request@GET -> Root / "callself" / name =>
        //val greetingList = Task.gatherUnordered(people.map(callSelf))
        Ok(callSelf(name))
    }

    def callSelf(name: String): Task[String] = {
      val target = Uri.uri("http://localhost:8080/users") / name
      client.expect[String](target)
    }

    def getUser(username: String): Task[String] = ???

    val people = Vector("Michael", "Jessica", "Ashley", "Christopher")
  }

}
