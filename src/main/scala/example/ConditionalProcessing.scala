package example

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source}

import scala.concurrent.Future


object ConditionalProcessing extends App {

  implicit val system: ActorSystem = ActorSystem()
  implicit val mat: ActorMaterializer = ActorMaterializer()

  val g = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
    import GraphDSL.Implicits._

    val in = Source(1 to 100)
    val out = Sink.foreach[Int](x => println(x))

    val isOdd = Flow[Int].filter(x => x % 2 != 0)
    val isEven = Flow[Int].filter(x => x % 2 == 0)

    val bcast = builder.add(Broadcast[Int](2))
    val merge = builder.add(Merge[Int](2))

    val f3 = Flow[Int].map(identity)

    val handleOdd = Flow[Int].mapAsync(10) {
      x =>
        Thread.sleep(5000)
        Future.successful(x)
    }

    val handleEven = Flow[Int].map(identity)

    in ~> bcast ~> isOdd ~> handleOdd ~> merge ~> f3 ~> out
          bcast ~> isEven ~> handleEven ~> merge
    ClosedShape

  })

  g.run()
  system.terminate()
}
