package ai.nixiesearch.embedbench.providers

import ai.nixiesearch.embedbench.Logging
import ai.nixiesearch.embedbench.providers.Provider.{Stats, Task}
import cats.effect.IO
import cats.effect.kernel.Resource
import com.google.cloud.aiplatform.v1.{EndpointName, PredictRequest, PredictionServiceClient, PredictionServiceSettings}
import com.google.protobuf.{Struct, Value}
import io.circe.Codec
import org.http4s.{AuthScheme, Credentials, EntityDecoder, EntityEncoder, Headers, MediaType, Method, Request, Uri}
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import io.circe.generic.semiauto.*
import org.http4s.circe.*
import org.http4s.headers.{Authorization, `Content-Type`}
import scala.jdk.CollectionConverters.*

import scala.concurrent.duration.*

case class Google(client: PredictionServiceClient, endpoint: EndpointName, model: String) extends Provider {
  val name = "google"

  def request(task: Task) =
    PredictRequest
      .newBuilder()
      .setEndpoint(endpoint.toString)
      .addInstances(
        Value
          .newBuilder()
          .setStructValue(
            Struct
              .newBuilder()
              .putFields("content", Value.newBuilder().setStringValue(task.text).build())
              .putFields("task_type", Value.newBuilder().setStringValue("SEMANTIC_SIMILARITY").build())
              .build()
          )
      )
      .build()

  override def embed(task: Provider.Task): IO[Provider.Stats] = for {
    start    <- IO(System.currentTimeMillis())
    response <- IO.blocking(client.predict(request(task)))
    embed    <- IO.fromOption(response.getPredictionsList.asScala.toList.headOption)(Exception("wtf"))
    end      <- IO(System.currentTimeMillis())
  } yield {
    Stats(
      dims = embed.getStructValue.getFieldsOrThrow("embeddings").getListValue.getValuesCount,
      millis = end - start,
      tokens = response.getMetadata.getStructValue
        .getFieldsOrThrow("billableCharacterCount")
        .getNumberValue
        .toInt
    )
  }
}

object Google extends Logging {

  def create(model: String, region: String = "us-east1"): Resource[IO, Google] = for {
    project <- Resource.eval(
      IO.fromOption(Option(System.getenv("GOOGLE_PROJECT")))(Exception("GOOGLE_PROJECT env var miss"))
    )
    endpoint <- Resource.eval(IO(EndpointName.ofProjectLocationPublisherModelName(project, region, "google", model)))
    settings <- Resource.eval(
      IO(
        PredictionServiceSettings.newBuilder().setEndpoint("us-central1-aiplatform.googleapis.com:443").build()
      )
    )
    client <- Resource.eval(IO(PredictionServiceClient.create(settings)))
    _      <- Resource.eval(info(s"Started Google client, model=$model"))
  } yield {
    Google(client, endpoint, model)
  }
}
