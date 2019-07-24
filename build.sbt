val Http4sVersion     = "0.20.0"
val CirceVersion      = "0.11.1"
val Specs2Version     = "4.1.0"
val LogbackVersion    = "1.2.3"
val PrometheusVersion = "0.6.0"

enablePlugins(DockerPlugin, JavaAppPackaging)

dockerExposedPorts := Seq(8080, 8080)
packageName in Docker := packageName.value
version in Docker := version.value

lazy val root = (project in file("."))
  .settings(
    organization := "no.mehl",
    name := "nano-prometheus-exporter",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.12.8",
    scalacOptions ++= Seq("-Ypartial-unification"),
    libraryDependencies ++= Seq(
      "org.http4s"     %% "http4s-blaze-server"       % Http4sVersion,
      "org.http4s"     %% "http4s-blaze-client"       % Http4sVersion,
      "org.http4s"     %% "http4s-circe"              % Http4sVersion,
      "org.http4s"     %% "http4s-dsl"                % Http4sVersion,
      "org.http4s"     %% "http4s-prometheus-metrics" % Http4sVersion,
      "io.circe"       %% "circe-generic"             % CirceVersion,
      "org.specs2"     %% "specs2-core"               % Specs2Version % "test",
      "ch.qos.logback" % "logback-classic"            % LogbackVersion,
      "io.prometheus"  % "simpleclient"               % PrometheusVersion,
      "io.prometheus"  % "simpleclient_common"        % PrometheusVersion
    ),
    addCompilerPlugin("org.spire-math" %% "kind-projector"     % "0.9.6"),
    addCompilerPlugin("com.olegpy"     %% "better-monadic-for" % "0.2.4")
  )

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-language:higherKinds",
  "-language:postfixOps",
  "-feature",
  "-Ypartial-unification",
  "-Xfatal-warnings"
)
