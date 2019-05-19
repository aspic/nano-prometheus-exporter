package no.mehl.nano.prometheus

import cats.effect.{ConcurrentEffect, ContextShift, Timer}
import cats.implicits._
import fs2.Stream
import io.prometheus.client.CollectorRegistry
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger

import scala.concurrent.ExecutionContext.global

object Server {

  def stream[F[_]: ConcurrentEffect](implicit T: Timer[F], C: ContextShift[F]): Stream[F, Nothing] = {
    for {
      client  <- BlazeClientBuilder[F](global).stream
      metrics <- Stream.eval(CollectorRegistryF.apply[F](new CollectorRegistry()))
      bas     <- NanoMetrics.apply(metrics, client)
      // Combine Service Routes into an HttpApp.
      // Can also be done via a Router if you
      // want to extract a segments not checked
      // in the underlying routes.
      httpApp = Routes.metrics[F](bas).orNotFound

      // With Middlewares in place
      finalHttpApp = Logger.httpApp(true, true)(httpApp)

      exitCode <- BlazeServerBuilder[F]
                   .bindHttp(8080, "0.0.0.0")
                   .withHttpApp(finalHttpApp)
                   .serve
    } yield exitCode
  }.drain
}
