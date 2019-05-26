package no.mehl.nano.prometheus

import cats.Applicative
import org.http4s.Uri
import cats.implicits._
import no.mehl.nano.rpc.NanoRPC.NanoAddress

case class Config[F[_]](nodeUrl: Uri, repAddress: NanoAddress)

object Config {
  def apply[F[_]: Applicative](nodeUrl: String, repAddress: String): fs2.Stream[F, Config[F]] =
    for {
      nodeAddress <- fs2.Stream.eval(Uri.unsafeFromString(nodeUrl).pure[F])
    } yield Config(nodeAddress, NanoAddress(repAddress))
}
