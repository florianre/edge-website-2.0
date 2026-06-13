package com.edgeprogressivepaddling.booking.routes

import cats.effect.IO
import org.http4s.StaticFile
import org.http4s.HttpRoutes
import org.http4s.dsl.io.*

final class StaticRoutes:
  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case request @ GET -> Root =>
      StaticFile.fromResource("/public/index.html", Some(request)).getOrElseF(NotFound())

    case request @ GET -> Root / "docs" =>
      StaticFile.fromResource("/public/docs.html", Some(request)).getOrElseF(NotFound())

    case request @ GET -> Root / "app.js" =>
      StaticFile.fromResource("/public/app.js", Some(request)).getOrElseF(NotFound())

    case request @ GET -> Root / "styles.css" =>
      StaticFile.fromResource("/public/styles.css", Some(request)).getOrElseF(NotFound())

    case request @ GET -> Root / "openapi.yaml" =>
      StaticFile.fromResource("/openapi.yaml", Some(request)).getOrElseF(NotFound())
  }
