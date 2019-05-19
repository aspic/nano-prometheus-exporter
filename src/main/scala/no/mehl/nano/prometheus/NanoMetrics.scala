package no.mehl.nano.prometheus

import cats.Applicative
import org.http4s.client.Client
import cats.implicits._
import io.prometheus.client.{Counter, Gauge}

case class NanoMetrics[F[_]: Applicative](c: CollectorRegistryF[F], client: Client[F]) {}

object NanoMetrics {
  def apply[F[_]: Applicative](c: CollectorRegistryF[F], client: Client[F]): fs2.Stream[F, NanoMetrics[F]] = {
    val counter = Counter.build("foobar", "as").create()
    val res = for {
      _ <- c.register(counter)
      _ = counter.inc(10)
    } yield new NanoMetrics[F](c, client)
    fs2.Stream.eval(res)
  }
}
