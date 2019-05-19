package no.mehl.nano.prometheus

import cats.effect.{ConcurrentEffect, ContextShift, IO, Timer}
import cats.implicits._
import fs2.Stream
import io.prometheus.client.{CollectorRegistry, Counter}
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger

import scala.concurrent.ExecutionContext.global

object Server {

  import scala.concurrent.duration._

  def stream[F[_]: ConcurrentEffect](implicit T: Timer[F], C: ContextShift[F]): Stream[F, Nothing] = {
    def updateMetrics(nano: NanoMetrics[F]): Stream[F, Unit] =
      for {
        _ <- fs2.Stream.awakeEvery[F](1.second)
        _ <- nano.update()
      } yield ()

    for {
      client        <- BlazeClientBuilder[F](global).stream
      registry      = new CollectorRegistry()
      nanoMetrics   <- NanoMetrics.apply(registry, client)
      pollingStream <- Stream.eval[F, Stream[F, Unit]](updateMetrics(nanoMetrics).pure[F])
      // Combine Service Routes into an HttpApp.
      // Can also be done via a Router if you
      // want to extract a segments not checked
      // in the underlying routes.
      httpApp = Routes.metrics[F](registry).orNotFound

      // With Middlewares in place
      finalHttpApp = Logger.httpApp(true, true)(httpApp)

      exitCode <- BlazeServerBuilder[F]
                   .bindHttp(8080, "0.0.0.0")
                   .withHttpApp(finalHttpApp)
                   .serve
                   .concurrently(pollingStream)
    } yield exitCode
  }.drain
}
