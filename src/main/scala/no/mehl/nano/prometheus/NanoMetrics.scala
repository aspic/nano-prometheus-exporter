package no.mehl.nano.prometheus

import cats.Applicative
import cats.effect.Sync
import cats.implicits._
import io.prometheus.client.{CollectorRegistry, Gauge}
import org.http4s.client.Client

class NanoMetrics[F[_]: Applicative: Sync](c: CollectorRegistry, client: Client[F], config: Config[F]) {

  val votingWeightOfTotal: Gauge =
    Gauge.build("voting_weight_of_total_supply", "Percent of total voting weight").create()
  c.register(votingWeightOfTotal)
  val votingWeightGauge: Gauge = Gauge.build("voting_weight", "Voting weight for this node").create()
  c.register(votingWeightGauge)

  val representativesCount: Gauge = Gauge.build("representatives_count", "Counts all representatives").create()
  c.register(representativesCount)

  val representativesWeight: Gauge = Gauge
    .build("representatives_weight", "100 largest representatives and their weight")
    .labelNames(
      "rep"
    )
    .create()
  c.register(representativesWeight)

  def update(): fs2.Stream[F, Unit] =
    for {
      nanoClient      <- fs2.Stream.eval(NanoRPC.impl(client, config).pure[F])
      votingWeight    <- fs2.Stream.attemptEval(nanoClient.votingWeight)
      representatives <- fs2.Stream.attemptEval(nanoClient.listRepresentatives)
      _ = {

        val nodeWeight          = BigDecimal.apply(votingWeight.right.get.weight) / NanoMetrics.mNanoDivider
        val weightOfTotalSupply = nodeWeight / NanoMetrics.totalSupply
        votingWeightOfTotal.set(weightOfTotalSupply.doubleValue())
        votingWeightGauge.set(nodeWeight.doubleValue())

        representativesCount.set(representatives.right.get.representatives.size)

        representatives.right.get.representatives.toList
          .map {
            case (address, weight) => (address, (BigDecimal.apply(weight) / NanoMetrics.mNanoDivider).doubleValue())
          }
          .sortBy { case (_, b) => -b }
          .take(200)
          .foreach {
            case (address, weight) => representativesWeight.labels(address).set(weight)
          }
        ()
      }
    } yield ()
}

object NanoMetrics {

  val mNanoDivider = BigDecimal("1000000000000000000000000000000")
  val totalSupply  = BigDecimal(133248290)

  def apply[F[_]: Applicative: Sync](c: CollectorRegistry,
                                     client: Client[F],
                                     config: Config[F]): fs2.Stream[F, NanoMetrics[F]] =
    fs2.Stream.eval[F, NanoMetrics[F]](new NanoMetrics[F](c, client, config).pure[F])
}
