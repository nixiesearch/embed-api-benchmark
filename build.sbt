version := "0.1.0-SNAPSHOT"

scalaVersion := "3.6.4"

name := "embed-bench"

lazy val http4sVersion = "1.0.0-M44"
lazy val circeVersion  = "0.14.12"
lazy val fs2Version    = "3.11.0"

libraryDependencies ++= Seq(
  "io.circe"        %% "circe-core"              % circeVersion,
  "io.circe"        %% "circe-generic"           % circeVersion,
  "io.circe"        %% "circe-parser"            % circeVersion,
  "org.http4s"      %% "http4s-dsl"              % http4sVersion,
  "org.http4s"      %% "http4s-ember-server"     % http4sVersion,
  "org.http4s"      %% "http4s-ember-client"     % http4sVersion,
  "org.http4s"      %% "http4s-circe"            % http4sVersion,
  "co.fs2"          %% "fs2-core"                % fs2Version,
  "co.fs2"          %% "fs2-io"                  % fs2Version,
  "org.typelevel"   %% "log4cats-slf4j"          % "2.7.0",
  "ch.qos.logback"   % "logback-classic"         % "1.5.18",
  "org.rogach"      %% "scallop"                 % "5.2.0",
  "com.google.cloud" % "google-cloud-aiplatform" % "3.61.0"
)
Compile / mainClass := Some("ai.nixiesearch.embedbench.Main")

ThisBuild / assemblyMergeStrategy := {
  case PathList("module-info.class")                                         => MergeStrategy.discard
  case "META-INF/io.netty.versions.properties"                               => MergeStrategy.first
  case "META-INF/MANIFEST.MF"                                                => MergeStrategy.discard
  case x if x.startsWith("META-INF/versions/")                               => MergeStrategy.first
  case x if x.startsWith("META-INF/services/")                               => MergeStrategy.concat
  case "META-INF/native-image/reflect-config.json"                           => MergeStrategy.concat
  case "META-INF/native-image/io.netty/netty-common/native-image.properties" => MergeStrategy.first
  case "META-INF/okio.kotlin_module"                                         => MergeStrategy.first
  case "findbugsExclude.xml"                                                 => MergeStrategy.discard
  case "log4j2-test.properties"                                              => MergeStrategy.discard
  case x if x.endsWith("/module-info.class")                                 => MergeStrategy.discard
  case x =>
    val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
    oldStrategy(x)
}
