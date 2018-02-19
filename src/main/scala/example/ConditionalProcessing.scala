package example

import akka.NotUsed
import akka.actor.ActorSystem
import akka.kafka.scaladsl.Producer
import akka.kafka.{ProducerMessage, ProducerSettings}
import akka.stream.{ActorMaterializer, FlowShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, Sink, Source}
import org.apache.kafka.clients.producer.{ProducerConfig, ProducerRecord, RecordMetadata}
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.util.{Failure, Success}

object ConditionalProcessing extends App {

  implicit val ec: ExecutionContextExecutor = ExecutionContext.global
  implicit val system: ActorSystem = ActorSystem()
  implicit val mat: ActorMaterializer = ActorMaterializer()

  val KafkaIp = sys.env("KAFKA_IP")

  val flow = Flow.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
    import GraphDSL.Implicits._

    val isOdd = Flow[Int].filter(x => x % 2 != 0)
    val isEven = Flow[Int].filter(x => x % 2 == 0)

    val bcast = builder.add(Broadcast[Int](2))
    val merge = builder.add(Merge[Int](2))
    val producerProps = Map[String, String](
      ProducerConfig.CLIENT_ID_CONFIG -> "my-producer",
      ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG -> classOf[ByteArraySerializer].getName,
      ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG -> classOf[ByteArraySerializer].getName,
      ProducerConfig.BATCH_SIZE_CONFIG -> "0"
    )

    val producerSettings = ProducerSettings(system, new ByteArraySerializer, new StringSerializer)
      .withBootstrapServers(KafkaIp)
      .withProperties(producerProps)

    val kafkaProducer = producerSettings.createKafkaProducer()

    val handleOdd : Flow[Int, ProducerMessage.Message[Array[Byte], String, Int], NotUsed]  =
      Flow[Int].map {
        x =>
          /*if (x == 51) {
            throw new Exception("Problem, problem, intrusion in area 51!")
          }*/
          //Thread.sleep(1000)
          x
      }.map(
        x =>
          ProducerMessage.Message(
            new ProducerRecord[Array[Byte], String]("some-dummy-topic", x.toString), x)
      )

    val result2Int = Flow[ProducerMessage.Result[Array[Byte], String, Int]].map {
      x => x.message.record.value().toInt
    }

    val producerFlow: Flow[ProducerMessage.Message[Array[Byte], String, Int], ProducerMessage.Result[Array[Byte], String, Int], NotUsed] =
      Producer.flow(producerSettings)
    val fakeProducerFlow: Flow[ProducerMessage.Message[Array[Byte], String, Int], ProducerMessage.Result[Array[Byte], String, Int], NotUsed] =
      Flow[ProducerMessage.Message[Array[Byte], String, Int]].map(
        x=> {
          val metadata = new RecordMetadata(new TopicPartition("", 1),1,1,1,1,1,1)
          ProducerMessage.Result(metadata, x)
        }
      )

    val handleEven = Flow[Int].map(x=> {
      Thread.sleep(500)
      x
    })

    bcast.out(0) ~> isOdd ~> handleOdd ~> fakeProducerFlow ~> result2Int ~> merge.in(0)
    bcast.out(1) ~> isEven ~> handleEven ~> merge.in(1)

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
