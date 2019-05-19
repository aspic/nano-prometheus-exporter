package no.mehl.nano.prometheus

import cats.effect.Sync
import cats.implicits._
import io.prometheus.client.CollectorRegistry
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

object Routes {

  def metrics[F[_]: Sync](registry: CollectorRegistry): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "metrics" =>
        org.http4s.metrics.prometheus.PrometheusExportService.generateResponse(registry)
    }
  }
}