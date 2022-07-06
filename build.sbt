val scala3Version = "3.1.3"

val Http4sVersion = "0.23.13"
val CirceVersion = "0.14.2"

lazy val root = project
  .in(file("."))
  .settings(
    name := "ws-example",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % "0.7.29" % Test,
      "org.http4s"      %% "http4s-ember-server" % Http4sVersion,
      "org.http4s"      %% "http4s-circe"        % Http4sVersion,
      "org.http4s"      %% "http4s-dsl"          % Http4sVersion,
      "io.circe"        %% "circe-core"       % CirceVersion,
    )
  )
