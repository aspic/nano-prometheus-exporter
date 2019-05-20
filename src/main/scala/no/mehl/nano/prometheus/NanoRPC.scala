package no.mehl.nano.prometheus

import cats.effect.Sync
import cats.implicits._
import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}
import org.http4s.Method._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.{EntityDecoder, EntityEncoder, Uri}

trait NanoRPC[F[_]] {
  def blockCount: F[NanoRPC.BlockCount]
  def votingWeight: F[NanoRPC.AccountWeight]
}

object NanoRPC {
  def apply[F[_]](implicit ev: NanoRPC[F]): NanoRPC[F] = ev

  type NanoAddress = String
  def NanoAddress(a: String): NanoAddress = new NanoAddress(a)

  final case class Action(action: String, account: Option[NanoAddress] = None)
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

  final case class AccountWeight(weight: String)
  object AccountWeight {
    implicit val accountWeightDecoder: Decoder[AccountWeight] = deriveDecoder[AccountWeight]
    implicit def accountWeightEntityDecoder[F[_]: Sync]: EntityDecoder[F, AccountWeight] =
      jsonOf
  }

  final case class JokeError(e: Throwable) extends RuntimeException

  def impl[F[_]: Sync](C: Client[F], config: Config[F]): NanoRPC[F] = new NanoRPC[F] {
    val dsl = new Http4sClientDsl[F] {}
    import dsl._

    override def blockCount: F[BlockCount] =
      C.expect[BlockCount](POST(Action("block_count"), config.nodeUrl))
        .adaptError { case t => JokeError(t) } // Prevent Client Json Decoding Failure Leaking

    override def votingWeight: F[AccountWeight] =
      C.expect[AccountWeight](
          POST(Action("account_weight", Some(config.repAddress)), config.nodeUrl)
        )
        .adaptError {
          case t => {
            println(t.printStackTrace())
            JokeError(t)
          }
        }
  }
}
