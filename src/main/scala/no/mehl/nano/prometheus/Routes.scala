package no.mehl.nano.prometheus

import cats.effect.Sync
import cats.implicits._
import io.prometheus.client.Counter
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

object Routes {

  def metrics[F[_]: Sync](registry: NanoMetrics[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "metrics" =>
        registry.c.registry.flatMap(org.http4s.metrics.prometheus.PrometheusExportService.generateResponse(_))
    }
  }
}