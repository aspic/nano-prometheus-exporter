package no.mehl.nano.prometheus

import cats.Applicative
import cats.implicits._
import io.prometheus.client.{CollectorRegistry, Counter}
import org.http4s.client.Client

class NanoMetrics[F[_]: Applicative](c: CollectorRegistry, client: Client[F]) {

  val counter: Counter = Counter.build("kjetil", "bar").create()
  c.register(counter)

  def update(): fs2.Stream[F, Unit] =
    fs2.Stream.eval[F, Unit](counter.inc().pure[F])
}

object NanoMetrics {
  def apply[F[_]: Applicative](c: CollectorRegistry, client: Client[F]): fs2.Stream[F, NanoMetrics[F]] = {
    val counter = Counter.build("foobar", "as").create()
    val foo = c.register(counter)
    counter.inc(5)
    fs2.Stream.eval[F, NanoMetrics[F]](new NanoMetrics[F](c, client).pure[F])
  }
}
