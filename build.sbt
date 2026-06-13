ThisBuild / scalaVersion := "3.3.3"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.edgeprogressivepaddling"

val Http4sVersion = "0.23.34"
val CirceVersion = "0.14.15"
val CirceYamlVersion = "0.16.1"
val CatsEffectVersion = "3.7.0"

lazy val root = (project in file("."))
  .settings(
    name := "edge-website-2.0",
    Compile / run / fork := true,
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-dsl" % Http4sVersion,
      "org.http4s" %% "http4s-ember-server" % Http4sVersion,
      "org.http4s" %% "http4s-server" % Http4sVersion,
      "org.http4s" %% "http4s-circe" % Http4sVersion,
      "org.typelevel" %% "cats-effect" % CatsEffectVersion,
      "io.circe" %% "circe-core" % CirceVersion,
      "io.circe" %% "circe-parser" % CirceVersion,
      "io.circe" %% "circe-generic" % CirceVersion,
      "io.circe" %% "circe-yaml" % CirceYamlVersion
    )
  )
