package no.mehl.nano.prometheus

import cats.Applicative
import io.prometheus.client.{Collector, CollectorRegistry}
import cats.implicits._

class CollectorRegistryF[F[_] : Applicative](c: CollectorRegistry) {
  val registry: F[CollectorRegistry] = c.pure[F]

  def register(collector: Collector): F[Unit] = registry.map(_.register(collector))
}

object CollectorRegistryF {
  def apply[F[_]: Applicative](f: CollectorRegistry): F[CollectorRegistryF[F]] = {
    new CollectorRegistryF[F](f).pure[F]
  }
}
