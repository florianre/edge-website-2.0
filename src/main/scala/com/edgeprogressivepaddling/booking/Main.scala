package com.edgeprogressivepaddling.booking

import cats.effect.{IO, IOApp}
import com.comcast.ip4s.{Host, Port}
import com.edgeprogressivepaddling.booking.config.AppConfig
import com.edgeprogressivepaddling.booking.repository.SlickEventRepository
import com.edgeprogressivepaddling.booking.routes.{AuthRoutes, EventRoutes, MembershipRoutes, StaticRoutes}
import com.edgeprogressivepaddling.booking.service.{AuthService, EventService, JsonMembershipService}
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import cats.syntax.semigroupk.*
import slick.jdbc.H2Profile.api.Database

import java.nio.file.Path
import scala.concurrent.ExecutionContext

object Main extends IOApp.Simple:
  override def run: IO[Unit] =
    for
      config <- AppConfig.load[IO]()
      membershipService <- JsonMembershipService.create(Path.of(config.membership.file))
      authService = AuthService(membershipService, config.committee.password)
      database = Database.forURL(
        url = config.database.url,
        driver = config.database.driver,
        user = config.database.user,
        password = config.database.password
      )
      eventRepository = SlickEventRepository(database)(using ExecutionContext.global)
      eventService = EventService(eventRepository, membershipService)
      _ <- eventService.initialise
      host <- IO.fromOption(Host.fromString(config.server.host))(
        IllegalArgumentException(s"Invalid host configured: ${config.server.host}")
      )
      port <- IO.fromOption(Port.fromInt(config.server.port))(
        IllegalArgumentException(s"Invalid port configured: ${config.server.port}")
      )
      httpApp = Router(
        "/api/auth" -> AuthRoutes(authService).routes,
        "/api/events" -> EventRoutes(eventService).routes,
        "/api/memberships" -> MembershipRoutes(membershipService).routes,
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
