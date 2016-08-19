package com.markglh.blog

import org.http4s.client.blaze.PooledHttp1Client
import org.http4s.dsl._
import org.http4s.{Uri, _}

import scala.concurrent.ExecutionContext
import scalaz.concurrent.Task

/**
  * Created by markh on 19/08/2016.
  */
object AggregatorService {
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
