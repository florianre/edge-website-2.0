package com.edgeprogressivepaddling.booking.config

import cats.effect.Async
import cats.syntax.all.*
import io.circe.Decoder
import io.circe.yaml.parser

import java.nio.charset.StandardCharsets
import scala.io.Source
import scala.util.Using

final case class AppConfig(server: ServerConfig, membership: MembershipConfig) derives Decoder

final case class ServerConfig(host: String, port: Int) derives Decoder

final case class MembershipConfig(file: String) derives Decoder

object AppConfig:

  def load[F[_]: Async](resourceName: String = "application.yaml"): F[AppConfig] =
    for
      rawYaml <- readResource[F](resourceName)
      json <- Async[F].fromEither(parser.parse(rawYaml).left.map(error =>
        IllegalStateException(s"Failed to parse $resourceName: ${error.getMessage}")
      ))
      config <- Async[F].fromEither(json.as[AppConfig].left.map(error =>
        IllegalStateException(s"Failed to decode $resourceName: ${error.getMessage}")
      ))
    yield config

  private def readResource[F[_]: Async](resourceName: String): F[String] =
    Async[F].blocking {
      val classLoader = Thread.currentThread().getContextClassLoader
      val stream = Option(classLoader.getResourceAsStream(resourceName)).getOrElse {
        throw IllegalStateException(s"Resource $resourceName was not found on the classpath")
      }

      Using.resource(stream) { inputStream =>
        Source.fromInputStream(inputStream, StandardCharsets.UTF_8.name()).mkString
      }
    }
