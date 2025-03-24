package ai.nixiesearch.embedbench.providers

import ai.nixiesearch.embedbench.Logging
import ai.nixiesearch.embedbench.providers.Cohere.{EmbedRequest, EmbedResponse}
import ai.nixiesearch.embedbench.providers.Provider.{Stats, Task}
import cats.effect.{IO, Resource}
import io.circe.Codec
import org.http4s.{AuthScheme, Credentials, EntityDecoder, EntityEncoder, Headers, MediaType, Method, Request, Uri}
import org.http4s.client.Client
import org.http4s.circe.*
import org.http4s.ember.client.EmberClientBuilder
import io.circe.generic.semiauto.*
import org.http4s.headers.{Accept, Authorization, `Content-Type`}

import scala.concurrent.duration.*

case class Cohere(client: Client[IO], endpoint: Uri, key: String, model: String) extends Provider {
  val name = "cohere"
  def request(task: Task) = Request[IO](
    method = Method.POST,
    uri = endpoint / "v2" / "embed",
    headers = Headers(
      Authorization(Credentials.Token(AuthScheme.Bearer, key)),
      `Content-Type`(MediaType.application.json),
      Accept(MediaType.application.json)
    ),
    entity = Cohere.requestJson.toEntity(
      EmbedRequest(
        model = model,
        texts = List(task.text),
        input_type = "classification",
        embedding_types = List("float")
      )
    )
  )

  override def embed(task: Task): IO[Provider.Stats] = for {
    start    <- IO(System.currentTimeMillis())
    response <- client.expect[EmbedResponse](request(task))
    embed    <- IO.fromOption(response.embeddings.float.headOption)(Exception("wtf"))
    end      <- IO(System.currentTimeMillis())
  } yield {
    Stats(
      dims = embed.length,
      millis = end - start,
      tokens = response.meta.billed_units.input_tokens
    )
  }

}

object Cohere extends Logging {
  case class EmbedRequest(model: String, texts: List[String], input_type: String, embedding_types: List[String])
  given requestCodec: Codec[EmbedRequest]            = deriveCodec
  given requestJson: EntityEncoder[IO, EmbedRequest] = jsonEncoderOf

  case class Embeddings(float: Array[Array[Float]])
  case class BilledUnits(input_tokens: Int)
  case class Meta(billed_units: BilledUnits)
  case class EmbedResponse(id: String, embeddings: Embeddings, texts: List[String], meta: Meta)
  given embeddingsCodec: Codec[Embeddings]             = deriveCodec
  given billedUnitsCodec: Codec[BilledUnits]           = deriveCodec
  given metaCodec: Codec[Meta]                         = deriveCodec
  given responseCodec: Codec[EmbedResponse]            = deriveCodec
  given responseJson: EntityDecoder[IO, EmbedResponse] = jsonOf

  def create(model: String): Resource[IO, Cohere] = for {
    client   <- EmberClientBuilder.default[IO].withTimeout(10.seconds).build
    endpoint <- Resource.eval(IO.fromEither(Uri.fromString("https://api.cohere.com/")))
    key      <- Resource.eval(IO.fromOption(Option(System.getenv("COHERE_KEY")))(Exception("COHERE_KEY env var miss")))
    _        <- Resource.eval(info(s"Started Cohere client, model=$model"))
  } yield {
    Cohere(client, endpoint, key, model)
  }

}
