package ai.nixiesearch.embedbench.providers

import ai.nixiesearch.embedbench.Logging
import ai.nixiesearch.embedbench.providers.Jina.{EmbedRequest, EmbedResponse}
import ai.nixiesearch.embedbench.providers.Provider.{Stats, Task}
import cats.effect.{IO, Resource}
import io.circe.Codec
import org.http4s.{AuthScheme, Credentials, EntityDecoder, EntityEncoder, Headers, MediaType, Method, Request, Uri}
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.headers.{Authorization, `Content-Type`}
import io.circe.generic.semiauto.*
import org.http4s.circe.*

import scala.concurrent.duration.*

case class Jina(client: Client[IO], endpoint: Uri, key: String, model: String) extends Provider {
  val name = "jina"
  def request(task: Task) = Request[IO](
    method = Method.POST,
    uri = endpoint / "v1" / "embeddings",
    headers = Headers(
      Authorization(Credentials.Token(AuthScheme.Bearer, key)),
      `Content-Type`(MediaType.application.json)
    ),
    entity = Jina.requestJson.toEntity(
      EmbedRequest(
        model = model,
        task = "text-matching",
        input = List(task.text)
      )
    )
  )
  override def embed(task: Provider.Task): IO[Provider.Stats] = for {
    start    <- IO(System.currentTimeMillis())
    response <- client.expect[EmbedResponse](request(task))
    embed    <- IO.fromOption(response.data.headOption)(Exception("wtf"))
    end      <- IO(System.currentTimeMillis())
  } yield {
    Stats(
      dims = embed.embedding.length,
      millis = end - start,
      tokens = response.usage.total_tokens
    )
  }
}

object Jina extends Logging {
  case class EmbedRequest(model: String, task: String, input: List[String])
  given requestCodec: Codec[EmbedRequest]            = deriveCodec
  given requestJson: EntityEncoder[IO, EmbedRequest] = jsonEncoderOf

  case class Usage(total_tokens: Int, prompt_tokens: Int)
  case class Embedding(`object`: String, index: Int, embedding: Array[Float])
  case class EmbedResponse(model: String, `object`: String, usage: Usage, data: List[Embedding])
  given usageCodec: Codec[Usage]                       = deriveCodec
  given embeddingCodec: Codec[Embedding]               = deriveCodec
  given responseCodec: Codec[EmbedResponse]            = deriveCodec
  given responseJson: EntityDecoder[IO, EmbedResponse] = jsonOf

  def create(model: String): Resource[IO, Jina] = for {
    client   <- EmberClientBuilder.default[IO].withTimeout(10.seconds).build
    endpoint <- Resource.eval(IO.fromEither(Uri.fromString("https://api.jina.ai/")))
    key      <- Resource.eval(IO.fromOption(Option(System.getenv("JINA_KEY")))(Exception("JINA_KEY env var miss")))
    _        <- Resource.eval(info(s"Started Jina client, model=$model"))
  } yield {
    Jina(client, endpoint, key, model)
  }
}
