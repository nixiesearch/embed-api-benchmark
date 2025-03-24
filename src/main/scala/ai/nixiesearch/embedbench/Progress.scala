package ai.nixiesearch.embedbench

import cats.effect.IO
import fs2.Pipe

object Progress extends Logging {
  case class ProgressPeriod(
      start: Long = System.currentTimeMillis(),
      total: Long = 0L,
      batchTotal: Long = 0L
  ) {
    def inc(events: Int) =
      copy(total = total + events, batchTotal = batchTotal + events)
  }
  def tap[T](suffix: String): Pipe[IO, T, T] = input =>
    input.scanChunks(ProgressPeriod()) { case (pp @ ProgressPeriod(start, total, batch), next) =>
      val now = System.currentTimeMillis()
      if ((now - start > 1000)) {
        val timeDiffSeconds = (now - start) / 1000.0
        val perf            = math.round(batch / timeDiffSeconds)
        logger.info(
          s"processed ${total} $suffix, perf=${perf}rps"
        )
        (
          pp.copy(start = now, batchTotal = 0).inc(next.size),
          next
        )
      } else {
        (pp.inc(next.size), next)
      }
    }
}
