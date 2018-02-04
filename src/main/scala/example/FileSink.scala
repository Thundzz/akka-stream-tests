package example
import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, IOResult}
import akka.stream.scaladsl.{FileIO, Flow, Sink, Source}
import akka.util.ByteString
import akka.{Done, NotUsed}
import scala.concurrent.duration._
import scala.concurrent.Future

object FileSink extends App {

  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()


  val source = Source(1 to 10)
  val flow: Flow[Int, String, NotUsed] = Flow[Int].map(_.toString)

  val grouperFlow : Flow[String, String, NotUsed] = Flow[String].groupedWithin(3, 1.micros).map(lines => lines.mkString("\n"))

  /**
    * Should have a tmp directory on your desktop for this to work
    */
  val fileFlow: Flow[String, IOResult, NotUsed] =
    Flow[String].mapAsync(parallelism = 4){ s ⇒
      val homeDir = System.getProperty("user.home")
      val fileName = s"$homeDir\\Desktop\\tmp\\${s.replace("\n", "-")}"
      Source.single(ByteString(s)).runWith(FileIO.toPath(Paths.get(fileName)))
    }

  val fileSink: Sink[IOResult, Future[Done]] = Sink.foreach[IOResult]{println}

  val graph = source.via(flow).via(grouperFlow).via(fileFlow).to(fileSink)
  graph.run()
}
