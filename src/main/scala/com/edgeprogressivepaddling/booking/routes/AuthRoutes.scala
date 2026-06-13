package com.edgeprogressivepaddling.booking.routes

import cats.effect.IO
import com.edgeprogressivepaddling.booking.auth.SessionCookieSupport
import com.edgeprogressivepaddling.booking.domain.*
import com.edgeprogressivepaddling.booking.service.AuthService
import org.http4s.HttpRoutes
import org.http4s.Response
import org.http4s.Status
import org.http4s.circe.{CirceEntityCodec, jsonOf}
import org.http4s.dsl.io.*

final class AuthRoutes(service: AuthService):
  import CirceEntityCodec.*

  given org.http4s.EntityDecoder[IO, LoginRequest] = jsonOf[IO, LoginRequest]
  given org.http4s.EntityDecoder[IO, CommitteePasswordRequest] = jsonOf[IO, CommitteePasswordRequest]

  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case request @ POST -> Root / "login" =>
      for
        payload <- request.as[LoginRequest]
        response <- service.login(payload).flatMap {
          case Left(error) =>
            unauthorized(error.message)
          case Right(session) =>
            Ok(SessionResponse(Some(session))).map(_.addCookie(SessionCookieSupport.setSessionCookie(session)))
        }
      yield response

    case request @ GET -> Root / "session" =>
      Ok(SessionResponse(SessionCookieSupport.sessionFromRequest(request)))

    case POST -> Root / "logout" =>
      Ok(SessionResponse(None)).map(_.addCookie(SessionCookieSupport.clearSessionCookie))

    case request @ POST -> Root / "committee" / "verify" =>
      SessionCookieSupport.sessionFromRequest(request) match
        case None => unauthorized("You must be logged in")
        case Some(session) =>
          for
            payload <- request.as[CommitteePasswordRequest]
            response <- service.verifyCommitteePassword(session, payload.password) match
              case Left(error) => Forbidden(Map("error" -> error.message))
              case Right(updatedSession) =>
                Ok(SessionResponse(Some(updatedSession))).map(
                  _.addCookie(SessionCookieSupport.setSessionCookie(updatedSession))
                )
          yield response
  }

  private def unauthorized(message: String): IO[Response[IO]] =
    IO.pure(Response[IO](status = Status.Unauthorized).withEntity(Map("error" -> message)))
