package example

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}


object TestStream extends App {
  implicit val system: ActorSystem = ActorSystem()
  implicit val mat: ActorMaterializer = ActorMaterializer()

  lessVerbose()

  def firstFlow(): Unit ={
    val source = Source(0 to 200000000)

    val flow = Flow[Int].map(_.toString())


    val sink = Sink.foreach[String](println(_))
    val runnable = source.via(flow).to(sink)

    runnable.run()
  }

  def lessVerbose(): Unit ={
    val source = Source(0 to 200000000)
      .map(_.toString())
      .runForeach(println)
  }
}

