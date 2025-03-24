package ai.nixiesearch.embedbench

import cats.effect.IO
import org.rogach.scallop.ScallopConf
import org.rogach.scallop.exceptions.{Help, ScallopException, ScallopResult, Version as ScallopVersion}
import org.rogach.scallop.{ScallopConf, ScallopOption, Subcommand, throwError, given}

import scala.util.{Try, Success, Failure}

case class ArgParser(arguments: List[String]) extends ScallopConf(arguments) with Logging {
  val queries = opt[String](name = "queries", required = true)
  val docs    = opt[String](name = "docs", required = true)
  val out     = opt[String](name = "out", required = false, default = Some("out.jsonl"))

  override protected def onError(e: Throwable): Unit = e match {
    case r: ScallopResult if !throwError.value =>
      r match {
        case Help("") =>
          logger.info("\n" + builder.getFullHelpString())
        case Help(subname) =>
          logger.info("\n" + builder.findSubbuilder(subname).get.getFullHelpString())
        case ScallopVersion =>
          "\n" + getVersionString().foreach(logger.info)
        case e @ ScallopException(message) => throw e
        // following should never match, but just in case
        case other: ScallopException => throw other
      }
    case e => throw e
  }

}

object ArgParser {
  case class Args(queries: String, docs: String, out: String)

  def parse(args: List[String]): IO[Args] = for {
    parser  <- IO(ArgParser(args))
    _       <- IO(parser.verify())
    queries <- parse(parser.queries)
    docs    <- parse(parser.docs)
    out     <- parse(parser.out)
  } yield {
    Args(queries, docs, out)
  }

  def parse[T](option: ScallopOption[T]): IO[T] = {
    Try(option.toOption) match {
      case Success(Some(value)) => IO.pure(value)
      case Success(None)        => IO.raiseError(new Exception(s"missing required option ${option.name}"))
      case Failure(ex)          => IO.raiseError(ex)
    }
  }
}
