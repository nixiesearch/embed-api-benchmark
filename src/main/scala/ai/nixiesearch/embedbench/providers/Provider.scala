package ai.nixiesearch.embedbench.providers

import ai.nixiesearch.embedbench.providers.Provider.{Stats, Task}
import cats.effect.IO
import io.circe.{Codec, Encoder}
import io.circe.generic.semiauto.*

trait Provider {
  def model: String
  def name: String
  def embed(task: Task): IO[Stats]
}

object Provider {
  case class Task(tpe: String, text: String)
  case class Result(
      provider: String,
      tpe: String,
      model: String,
      text: String,
      status: String,
      ts: Long,
      stats: Option[Stats]
  )
  case class Stats(
      dims: Int,
      millis: Long,
      tokens: Int
  )

  given statsCodec: Codec[Stats]   = deriveCodec
  given resultCodec: Codec[Result] = deriveCodec
}
