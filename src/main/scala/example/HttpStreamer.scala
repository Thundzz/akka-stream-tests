package example

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.ByteString

object HttpStreamer extends App {
  implicit val system: ActorSystem = ActorSystem()
  implicit val mat: ActorMaterializer = ActorMaterializer()

  def numbers: Source[ByteString, NotUsed] = {
    Source.unfold(0L) { n =>
      val next = n + 1
      Some((next, next))
    }.map(n => ByteString(n + "\n"))
  }

  val route =
    path("numbers") {
      get {
        complete(
          HttpResponse(entity=HttpEntity(`text/plain(UTF-8)`, numbers))
        )
      }
    }
  val futureBinding = Http().bindAndHandle(route, "127.0.0.1", 8080)
}
