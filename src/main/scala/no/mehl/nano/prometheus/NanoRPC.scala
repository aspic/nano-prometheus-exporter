package no.mehl.nano.prometheus

import cats.effect.Sync
import cats.implicits._
import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}
import org.http4s.Method._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.{EntityDecoder, EntityEncoder}

trait NanoRPC[F[_]] {
  def blockCount: F[NanoRPC.BlockCount]
  def votingWeight: F[NanoRPC.AccountWeight]
  def listRepresentatives: F[NanoRPC.Representatives]
}

object NanoRPC {
  def apply[F[_]](implicit ev: NanoRPC[F]): NanoRPC[F] = ev

  type NanoAddress = String
  def NanoAddress(a: String): NanoAddress = new NanoAddress(a)
  type VotingWeight = String
  def VotingWeight(a: String): VotingWeight = new VotingWeight(a)

  final case class Action(action: String, account: Option[NanoAddress] = None, sorting: Option[Boolean] = None)
  object Action {
    implicit val actionEncoder: Encoder[Action]                            = deriveEncoder[Action]
    implicit def actionEntityEncoder[F[_]: Sync]: EntityEncoder[F, Action] = jsonEncoderOf
  }

  final case class BlockCount(count: String, unchecked: String)
  object BlockCount {
    implicit val blockCountDecoder: Decoder[BlockCount] = deriveDecoder[BlockCount]
    implicit def blockCountEntityDecoder[F[_]: Sync]: EntityDecoder[F, BlockCount] =
      jsonOf
  }

  final case class AccountWeight(weight: VotingWeight)
  object AccountWeight {
    implicit val accountWeightDecoder: Decoder[AccountWeight] = deriveDecoder[AccountWeight]
    implicit def accountWeightEntityDecoder[F[_]: Sync]: EntityDecoder[F, AccountWeight] =
      jsonOf
  }

  final case class Representatives(representatives: Map[NanoAddress, VotingWeight])
  object Representatives {
    implicit val representativesDecoder: Decoder[Representatives] = deriveDecoder[Representatives]
    implicit def representativesEntityDecoder[F[_]: Sync]: EntityDecoder[F, Representatives] =
      jsonOf
  }

  final case class RPCError(e: Throwable) extends RuntimeException

  def impl[F[_]: Sync](C: Client[F], config: Config[F]): NanoRPC[F] = new NanoRPC[F] {
    val dsl: Http4sClientDsl[F] = new Http4sClientDsl[F] {}
    import dsl._

    override def blockCount: F[BlockCount] =
      C.expect[BlockCount](POST(Action("block_count"), config.nodeUrl))
        .adaptError { case t => RPCError(t) } // Prevent Client Json Decoding Failure Leaking

    override def votingWeight: F[AccountWeight] =
      C.expect[AccountWeight](
          POST(Action("account_weight", Some(config.repAddress)), config.nodeUrl)
        )
        .adaptError {
          case t =>
            println(t.printStackTrace())
            RPCError(t)
        }
    override def listRepresentatives: F[Representatives] =
      C.expect[Representatives](POST(Action("representatives"), config.nodeUrl))
        .adaptError { case t => RPCError(t) } // Prevent Client Json Decoding Failure Leaking
  }
}
