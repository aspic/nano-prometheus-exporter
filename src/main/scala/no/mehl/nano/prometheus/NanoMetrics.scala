package no.mehl.nano.prometheus

import cats.Applicative
import cats.effect.Sync
import cats.implicits._
import io.prometheus.client.{CollectorRegistry, Gauge}
import org.http4s.client.Client

class NanoMetrics[F[_]: Applicative: Sync](c: CollectorRegistry, client: Client[F], config: Config[F]) {

  val votingWeightOfTotal: Gauge =
    Gauge.build("voting_weight_of_total_supply", "Percent of total voting weight").create().register(c)
  val votingWeightGauge: Gauge = Gauge.build("voting_weight", "Voting weight for this node").create().register(c)

  val representativesCount: Gauge =
    Gauge.build("representatives_count", "Counts all representatives").create().register(c)

  val representativesWeight: Gauge = Gauge
    .build("top_representatives_weight", "5 largest representatives and their weight")
    .labelNames(
      "rep"
    )
    .create()
    .register(c)

  val rrRepsCount: Gauge = Gauge.build("rebroadcasting_nodes", "Count of all rebroadcasting nodes").create().register(c)
  val almostThere: Gauge = Gauge
    .build("close_nodes", "Count 100 closest nodes to 0.001% voting weight")
    .labelNames("rep")
    .create()
    .register(c)

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

        val sortedReps = representatives.right.get.representatives.toList
          .map {
            case (address, weight) =>
              (address, (BigDecimal.apply(weight) / NanoMetrics.mNanoDivider / NanoMetrics.totalSupply).doubleValue())
          }
          .sortBy { case (_, b) => -b }

        sortedReps
          .take(5)
          .foreach {
            case (address, weightPercent) => representativesWeight.labels(address).set(weightPercent)
          }

        rrRepsCount
          .set(sortedReps.count(_._2 >= NanoMetrics.rrLimit))

        sortedReps
          .filter(_._2 < NanoMetrics.rrLimit)
          .take(10)
          .foreach {
            case (address, weightPercent) => almostThere.labels(address).set(weightPercent)
          }
        ()
      }
    } yield ()
}

object NanoMetrics {

  val mNanoDivider    = BigDecimal("1000000000000000000000000000000")
  val totalSupply     = BigDecimal(133248290)
  val rrLimit: Double = 0.001

  def apply[F[_]: Applicative: Sync](c: CollectorRegistry,
                                     client: Client[F],
                                     config: Config[F]): fs2.Stream[F, NanoMetrics[F]] =
    fs2.Stream.eval[F, NanoMetrics[F]](new NanoMetrics[F](c, client, config).pure[F])
}
