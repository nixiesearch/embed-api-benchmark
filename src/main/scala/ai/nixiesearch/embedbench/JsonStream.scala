package ai.nixiesearch.embedbench

import cats.effect.IO
import fs2.Stream
import io.circe.Codec
import io.circe.generic.semiauto.*
import io.circe.parser.*

import java.io.{File, FileInputStream}
import scala.util.Random

object JsonStream extends Logging {
  def fromFile(path: String, limit: Int = 1000000): Stream[IO, String] = for {
    texts <- Stream
      .eval(
        fs2.io
          .readInputStream(IO(new FileInputStream(new File(path))), chunkSize = 1024)
          .through(fs2.text.utf8.decode)
          .through(fs2.text.lines)
          .filter(_.nonEmpty)
          .take(limit)
          .through(Progress.tap("lines"))
          .evalMap(line =>
            IO(decode[BEIRPayload](line)).flatMap {
              case Left(value)  => IO.raiseError(value)
              case Right(value) => IO.pure(value.text)
            }
          )
          .compile
          .toList
      )
      .map(_.toArray)
    _    <- Stream.eval(info(s"Loaded ${texts.length} lines for ${path}"))
    next <- Stream.repeatEval(IO(Random.nextInt(texts.length))).map(index => texts(index))
  } yield {
    next
  }

  case class BEIRPayload(text: String)
  given payloadCodec: Codec[BEIRPayload] = deriveCodec
}
