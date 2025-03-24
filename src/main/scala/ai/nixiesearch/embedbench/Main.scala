package ai.nixiesearch.embedbench

import ai.nixiesearch.embedbench.providers.{Cohere, Google, Jina, OpenAI}
import ai.nixiesearch.embedbench.providers.Provider.{Result, Task}
import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits.*
import fs2.{Chunk, Stream}
import io.circe.syntax.*

import scala.concurrent.duration.*
import java.io.{File, FileOutputStream}
import java.nio.ByteBuffer

object Main extends IOApp with Logging {
  override def run(args: List[String]): IO[ExitCode] = for {
    _    <- info("starting app")
    args <- ArgParser.parse(args)
    _ <- List(
      OpenAI.create("text-embedding-3-small"),
      OpenAI.create("text-embedding-3-large"),
      Cohere.create("embed-english-v3.0"),
      Jina.create("jina-embeddings-v3"),
      Google.create("text-embedding-005")
    ).sequence.use(providers =>
      JsonStream
        .fromFile(args.queries)
        .map(txt => Task("query", txt))
        .interleave(JsonStream.fromFile(args.docs).map(txt => Task("doc", txt)))
        .flatMap(task =>
          Stream
            .emits(providers)
            .evalMap(provider =>
              provider.embed(task).attempt.flatMap {
                case Left(err) =>
                  error("oops", err) *> IO.pure(
                    Result(
                      provider.name,
                      task.tpe,
                      provider.model,
                      task.text,
                      "error",
                      System.currentTimeMillis(),
                      None
                    )
                  )
                case Right(value) =>
                  IO.pure(
                    Result(
                      provider.name,
                      task.tpe,
                      provider.model,
                      task.text,
                      "ok",
                      System.currentTimeMillis(),
                      Some(value)
                    )
                  )
              }
            )
        )
        .metered(1.second)
        .flatTap(result =>
          Stream.eval(
            info(
              s"${result.provider}: ${result.tpe} ${result.model} status=${result.status} millis=${result.stats.map(_.millis)}"
            )
          )
        )
        .flatMap(result => Stream.chunk(Chunk.byteBuffer(ByteBuffer.wrap((result.asJson.noSpaces + "\n").getBytes))))
        .through(fs2.io.writeOutputStream(IO(new FileOutputStream(new File(args.out)))))
        .compile
        .drain
    )
  } yield {
    ExitCode.Success
  }
}
