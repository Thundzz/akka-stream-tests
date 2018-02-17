package example

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, Sink, Source}
import akka.stream.{ActorMaterializer, FlowShape}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}


object ConditionalProcessing extends App {

  implicit val ec: ExecutionContextExecutor = ExecutionContext.global
  implicit val system: ActorSystem = ActorSystem()
  implicit val mat: ActorMaterializer = ActorMaterializer()

  val flow = Flow.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
    import GraphDSL.Implicits._

    val isOdd = Flow[Int].filter(x => x % 2 != 0)
    val isEven = Flow[Int].filter(x => x % 2 == 0)

    val bcast = builder.add(Broadcast[Int](2))
    val merge = builder.add(Merge[Int](2))

    val handleOdd = Flow[Int].mapAsync(10) {
      x =>
        //Thread.sleep(500)
        if (x == 51) {
          throw new Exception("Problem, problem, a 51 was found !!!")
        }
        Future.successful(x)
    }

    val handleEven = Flow[Int].map(identity)
    bcast.out(0) ~> isOdd ~> handleOdd.async ~> merge.in(0)
    bcast.out(1) ~> isEven ~> handleEven.async ~> merge.in(1)

    FlowShape(bcast.in, merge.out)
  })
  val out = Sink.foreach[Int](x => println(x))

  val future = Source(1 to 100).via(flow).runWith(out)

  future.onComplete {
    case Success(_) =>
      println("finished.")
      system.terminate()
    case Failure(e) => println(e)
      system.terminate()
  }
}
