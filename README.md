# Embedding API latency benchmark harness

Created to collect raw data for a tiny research project [TODO: add link](about:blank) on availability and resilience of SaaS embedding APIs.

## Supported APIs

The tool supports following APIs:

* OpenAI embeddings
* Google VertexAI embeddings
* Jina.ai embeddings
* Cohere embeddings.

To authenticate, you need to provide API keys using env vars:

```yaml
OPENAI_KEY=xxx
COHERE_KEY=xxx
JINA_KEY=xxx
GOOGLE_KEY=xxx
```

## Building 

Simplest option would be to take an `IntelliJ IDEA Community` and load the `SBT` project as is.

You can also do it with pure `SBT`:

```shell
sbt assembly
java -jar target/<scala-dir>/embed-bench-assembly-0.1.0-SNAPSHOT.jar --help

13:35:49.524 INFO  ai.nixiesearch.embedbench.Main$ - starting app
13:35:49.565 INFO  ai.nixiesearch.embedbench.ArgParser - 
  -d, --docs  <arg>
  -q, --queries  <arg>
  -h, --help             Show help message
java.lang.Exception: missing required option queries
```

## FAQ

Q: OMG it's written in Scala!
A: Yes I like it, why not?

## License

Apache 2.0