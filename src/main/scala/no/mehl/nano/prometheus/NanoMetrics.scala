package no.mehl.nano.prometheus

import cats.Applicative
import cats.effect.Sync
import cats.implicits._
import io.prometheus.client.{CollectorRegistry, Counter}
import org.http4s.client.Client

class NanoMetrics[F[_]: Applicative : Sync](c: CollectorRegistry, client: Client[F]) {

  val counter: Counter = Counter.build("kjetil", "bar").create()
  c.register(counter)

  def update(): fs2.Stream[F, Unit] = for {
    _ <- fs2.Stream.eval[F, Unit](counter.inc().pure[F])
    count <- fs2.Stream.attemptEval(NanoRPC.impl(client).votingWeight)
    test = {
      println(count.right.get)
      ()
    }
  } yield ()
}

object NanoMetrics {
  def apply[F[_]: Applicative: Sync](c: CollectorRegistry, client: Client[F]): fs2.Stream[F, NanoMetrics[F]] = {
    fs2.Stream.eval[F, NanoMetrics[F]](new NanoMetrics[F](c, client).pure[F])
  }
}
