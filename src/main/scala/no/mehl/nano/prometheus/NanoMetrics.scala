package no.mehl.nano.prometheus

import cats.Applicative
import cats.effect.Sync
import cats.implicits._
import io.prometheus.client.{CollectorRegistry, Gauge}
import no.mehl.nano.rpc.NanoRPC
import no.mehl.nano.rpc.NanoRPC.NanoAddress
import org.http4s.client.Client

case class Representative(address: NanoAddress, votingWeightRaw: BigDecimal) {

  val votingWeightNano: BigDecimal          = votingWeightRaw / NanoMetrics.mNanoDivider
  val votingWeightOfTotalSupply: BigDecimal = votingWeightNano / NanoMetrics.totalSupply

}

object Representative {
  def apply(address: String, votingWeight: String): Representative =
    Representative(NanoAddress(address), BigDecimal.apply(votingWeight))
}

class NanoMetrics[F[_]: Applicative: Sync](c: CollectorRegistry, client: Client[F], config: Config[F]) {

  val votingWeightOfTotal: Gauge =
    Gauge.build("voting_weight_of_total_supply", "Percent of total voting weight").create().register(c)
  val votingWeightGauge: Gauge = Gauge.build("voting_weight", "Voting weight for this node").create().register(c)

  val allNodesVotingWeightOfSupply: Gauge = Gauge
    .build("representatives_with_weight", "All representatives with their voting weight of total supply")
    .labelNames("rep")
    .create()
    .register(c)

  val nanoClient: NanoRPC[F] = NanoRPC.impl(client, config)

  def update(): fs2.Stream[F, Unit] =
    for {
      weightResponse <- fs2.Stream.attemptEval(nanoClient.votingWeight(config.repAddress))
      repResponse    <- fs2.Stream.attemptEval(nanoClient.listRepresentatives)
      _ = {

        (for {
          nanoWeight      <- weightResponse
          representatives <- repResponse
        } yield {

          val targetNode = Representative.apply(config.repAddress, nanoWeight.weight)
          votingWeightOfTotal.set(targetNode.votingWeightOfTotalSupply.doubleValue())
          votingWeightGauge.set(targetNode.votingWeightNano.doubleValue())

          val sortedReps = representatives.representatives.toList
            .map {
              case (address, repWeight) =>
                Representative.apply(address, repWeight)
            }
            .filter(_.votingWeightOfTotalSupply > 0)
            .sortBy(node => -node.votingWeightNano)

          sortedReps.foreach(rep => {
            allNodesVotingWeightOfSupply.labels(rep.address).set(rep.votingWeightOfTotalSupply.doubleValue())
          })

          ()
        }).fold(t => {
          println(s"Error getting nano response")
          t.printStackTrace()
        }, _ => ())
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
