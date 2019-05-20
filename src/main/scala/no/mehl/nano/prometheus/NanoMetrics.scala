package no.mehl.nano.prometheus

import cats.Applicative
import cats.effect.Sync
import cats.implicits._
import io.prometheus.client.{CollectorRegistry, Gauge}
import org.http4s.client.Client

class NanoMetrics[F[_]: Applicative: Sync](c: CollectorRegistry, client: Client[F], config: Config[F]) {

  val votingWeightOfTotal: Gauge = Gauge.build("voting_weight_of_total_supply", "Percent of total voting weight").create()
  c.register(votingWeightOfTotal)
  val votingWeightGauge: Gauge = Gauge.build("voting_weight", "Voting weight for this node").create()
  c.register(votingWeightGauge)


  def update(): fs2.Stream[F, Unit] =
    for {
      votingWeight <- fs2.Stream.attemptEval(NanoRPC.impl(client, config).votingWeight)
      _ = {

        val nodeWeight   = BigDecimal.apply(votingWeight.right.get.weight) / NanoMetrics.mNanoDivider
        val weightOfTotalSupply = nodeWeight / NanoMetrics.totalSupply
        votingWeightOfTotal.set(weightOfTotalSupply.doubleValue())
        votingWeightGauge.set(nodeWeight.doubleValue())
        ()
      }
    } yield ()
}

object NanoMetrics {

  val mNanoDivider = BigDecimal("1000000000000000000000000000000")
  val totalSupply  = BigDecimal(133248290)

  def apply[F[_]: Applicative: Sync](c: CollectorRegistry, client: Client[F], config: Config[F]): fs2.Stream[F, NanoMetrics[F]] =
    fs2.Stream.eval[F, NanoMetrics[F]](new NanoMetrics[F](c, client, config).pure[F])
}
