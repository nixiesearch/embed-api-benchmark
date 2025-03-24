package ai.nixiesearch.embedbench.providers

import ai.nixiesearch.embedbench.Logging
import ai.nixiesearch.embedbench.providers.OpenAI.{EmbedRequest, EmbedResponse, OpenAIResponse, requestJson}
import ai.nixiesearch.embedbench.providers.Provider.{Stats, Task}
import cats.effect.IO
import cats.effect.kernel.Resource
import org.http4s.{AuthScheme, Credentials, EntityDecoder, EntityEncoder, Headers, MediaType, Method, Request, Uri}
import org.http4s.client.Client
import io.circe.Codec
import io.circe.generic.semiauto.*
import org.http4s.circe.*
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.headers.{Authorization, `Content-Type`}

import scala.concurrent.duration.*

case class OpenAI(client: Client[IO], endpoint: Uri, key: String, model: String) extends Provider {
  val name = "openai"

  def request(task: Task) = {
    val req = Request[IO](
      method = Method.POST,
      uri = endpoint / "v1" / "embeddings",
      headers = Headers(
        Authorization(Credentials.Token(AuthScheme.Bearer, key)),
        `Content-Type`(MediaType.application.json)
      ),
      entity = OpenAI.requestJson.toEntity(EmbedRequest(task.text, model))
    )
    req
  }
  override def embed(task: Task): IO[Provider.Stats] = for {
    start    <- IO(System.currentTimeMillis())
    response <- client.expect[OpenAIResponse](request(task))
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

object OpenAI extends Logging {

  case class EmbedRequest(input: String, model: String)
  case class EmbedResponse(`object`: String, embedding: Array[Float], index: Int)
  case class Usage(prompt_tokens: Int, total_tokens: Int)
  case class OpenAIResponse(`object`: String, data: List[EmbedResponse], usage: Usage)

  given requestCodec: Codec[EmbedRequest]               = deriveCodec
  given usageCodec: Codec[Usage]                        = deriveCodec
  given requestJson: EntityEncoder[IO, EmbedRequest]    = jsonEncoderOf
  given embedResponseCodec: Codec[EmbedResponse]        = deriveCodec
  given openaiResponseCodec: Codec[OpenAIResponse]      = deriveCodec
  given responseJson: EntityDecoder[IO, OpenAIResponse] = jsonOf

  def create(model: String): Resource[IO, OpenAI] = for {
    client   <- EmberClientBuilder.default[IO].withTimeout(10.seconds).build
    endpoint <- Resource.eval(IO.fromEither(Uri.fromString("https://api.openai.com/")))
    key      <- Resource.eval(IO.fromOption(Option(System.getenv("OPENAI_KEY")))(Exception("OPENAI_KEY env var miss")))
    _        <- Resource.eval(info(s"Started OpenAI client, model=$model"))
  } yield {
    OpenAI(client, endpoint, key, model)
  }
}
