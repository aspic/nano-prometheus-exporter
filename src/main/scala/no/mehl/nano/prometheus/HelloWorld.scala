package no.mehl.nano.prometheus

import java.io.{BufferedWriter, PrintWriter, StringWriter, Writer}

import cats.Applicative
import cats.implicits._
import cats.syntax.writer
import io.circe.{Decoder, Encoder, Json}
import io.prometheus.client.{Collector, CollectorRegistry, Counter}
import io.prometheus.client
import io.prometheus.client.exporter.common.TextFormat
import no.mehl.nano.prometheus.HelloWorld.Greeting
import org.http4s.EntityEncoder
import org.http4s.circe._

trait HelloWorld[F[_]]{
  def hello(n: HelloWorld.Name): F[HelloWorld.Greeting]
  def metrics(collector: CollectorRegistry): F[Metrics]
}

object HelloWorld {
  implicit def apply[F[_]](implicit ev: HelloWorld[F]): HelloWorld[F] = ev

  final case class Name(name: String) extends AnyVal
  /**
    * More generally you will want to decouple your edge representations from
    * your internal data structures, however this shows how you can
    * create encoders for your data.
    **/
  final case class Greeting(greeting: String) extends AnyVal
  object Greeting {
    implicit val greetingEncoder: Encoder[Greeting] = new Encoder[Greeting] {
      final def apply(a: Greeting): Json = Json.obj(
        ("message", Json.fromString(a.greeting)),
      )
    }
    implicit def greetingEntityEncoder[F[_]: Applicative]: EntityEncoder[F, Greeting] =
      jsonEncoderOf[F, Greeting]
  }

  def impl[F[_]: Applicative]: HelloWorld[F] = new HelloWorld[F]{
    def hello(n: HelloWorld.Name): F[HelloWorld.Greeting] =
        Greeting("Hello, " + n.name).pure[F]

    override def metrics(collectorRegistry: CollectorRegistry): F[Metrics] = {
      val foor = Counter.build("foobar", "bas").create()
      collectorRegistry.register(foor)

      foor.inc()

      val writer: Writer = new StringWriter()
      TextFormat.write004(writer, collectorRegistry.metricFamilySamples())
      writer.close()
      println(writer.toString)
      Metrics().pure[F]
    }
  }
}

case class Metrics()

object Metrics {

    implicit val metricsEncoder: Encoder[Metrics] = new Encoder[Metrics] {
      override def apply(a: Metrics): Json = Json.obj(
        ("message", Json.fromString("foobar")),
      )
    }

  implicit def greetingEntityEncoder[F[_]: Applicative]: EntityEncoder[F, Metrics] =
    jsonEncoderOf[F, Metrics]

}