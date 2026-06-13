package com.edgeprogressivepaddling.booking

import cats.effect.{IO, IOApp}
import com.comcast.ip4s.{Host, Port}
import com.edgeprogressivepaddling.booking.config.AppConfig
import com.edgeprogressivepaddling.booking.routes.{MembershipRoutes, StaticRoutes}
import com.edgeprogressivepaddling.booking.service.JsonMembershipService
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router

import java.nio.file.Path

object Main extends IOApp.Simple:
  override def run: IO[Unit] =
    for
      config <- AppConfig.load[IO]()
      membershipService <- JsonMembershipService.create(Path.of(config.membership.file))
      host <- IO.fromOption(Host.fromString(config.server.host))(
        IllegalArgumentException(s"Invalid host configured: ${config.server.host}")
      )
      port <- IO.fromOption(Port.fromInt(config.server.port))(
        IllegalArgumentException(s"Invalid port configured: ${config.server.port}")
      )
      httpApp = Router(
        "/api" -> MembershipRoutes(membershipService).routes,
        "/" -> StaticRoutes().routes
      ).orNotFound
      _ <- EmberServerBuilder
        .default[IO]
        .withHost(host)
        .withPort(port)
        .withHttpApp(httpApp)
        .build
        .useForever
    yield ()
