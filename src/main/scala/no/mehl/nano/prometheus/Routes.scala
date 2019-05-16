package no.mehl.nano.prometheus

import cats.effect.Sync
import cats.implicits._
import io.circe.Json
import io.prometheus.client.{CollectorRegistry, Counter}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.HttpRoutes

object Routes {

  def metrics[F[_]: Sync](H: HelloWorld[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "metrics" =>
        for {
          greeting <- H.metrics(new CollectorRegistry())
          resp <- Ok(greeting)
        } yield resp
    }
  }
}