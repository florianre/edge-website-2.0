package com.edgeprogressivepaddling.booking.routes

import cats.effect.IO
import com.edgeprogressivepaddling.booking.auth.SessionCookieSupport
import com.edgeprogressivepaddling.booking.domain.*
import com.edgeprogressivepaddling.booking.service.{AuthError, EventError, EventService}
import org.http4s.HttpRoutes
import org.http4s.{Response, Status}
import org.http4s.circe.{CirceEntityCodec, jsonOf}
import org.http4s.dsl.io.*
import io.circe.Encoder

final class EventRoutes(service: EventService):
  import CirceEntityCodec.*

  given org.http4s.EntityDecoder[IO, CreateSessionEventRequest] = jsonOf[IO, CreateSessionEventRequest]
  given org.http4s.EntityDecoder[IO, CreateTripEventRequest] = jsonOf[IO, CreateTripEventRequest]
  given org.http4s.EntityDecoder[IO, UpdateEventRequest] = jsonOf[IO, UpdateEventRequest]
  given org.http4s.EntityDecoder[IO, CreateBookingRequest] = jsonOf[IO, CreateBookingRequest]
  given org.http4s.EntityDecoder[IO, PaymentSentRequest] = jsonOf[IO, PaymentSentRequest]
  given org.http4s.EntityDecoder[IO, PaymentReceivedRequest] = jsonOf[IO, PaymentReceivedRequest]

  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root :? ScopeQueryParamMatcher(scope) =>
      scope match
        case Some("past") => service.listPast.flatMap(Ok(_))
        case _            => service.listUpcoming.flatMap(Ok(_))

    case request @ GET -> Root / LongVar(id) =>
      service.getEventDetail(id, currentSession(request)).flatMap {
        case Left(error)   => eventErrorResponse(error)
        case Right(detail) => Ok(detail)
      }

    case request @ POST -> Root / "sessions" =>
      requireSession(request) { session =>
        for
          payload <- request.as[CreateSessionEventRequest]
          response <- service.createSession(payload, session).flatMap(result => toEventResponse(result, Status.Created))
        yield response
      }

    case request @ POST -> Root / "trips" =>
      requireSession(request) { session =>
        for
          payload <- request.as[CreateTripEventRequest]
          response <- service.createTrip(payload, session).flatMap(result => toEventResponse(result, Status.Created))
        yield response
      }

    case request @ PUT -> Root / LongVar(id) =>
      requireSession(request) { session =>
        for
          payload <- request.as[UpdateEventRequest]
          response <- service.updateEvent(id, payload, session).flatMap(result => toEventResponse(result, Status.Ok))
        yield response
      }

    case request @ PATCH -> Root / LongVar(id) / "cancel" =>
      requireSession(request) { session =>
        service.cancelEvent(id, session).flatMap(result => toEventResponse(result, Status.Ok))
      }

    case request @ POST -> Root / LongVar(id) / "bookings" =>
      requireSession(request) { session =>
        for
          payload <- request.as[CreateBookingRequest]
          response <- service.bookEvent(id, payload, session).flatMap(result => toEventResponse(result, Status.Ok))
        yield response
      }

    case request @ DELETE -> Root / LongVar(id) / "bookings" / membershipNumber =>
      requireSession(request) { session =>
        service.removeBooking(id, membershipNumber, session).flatMap(result => toEventResponse(result, Status.Ok))
      }

    case request @ PATCH -> Root / LongVar(id) / "bookings" / membershipNumber / "payment-sent" =>
      requireSession(request) { session =>
        for
          payload <- request.as[PaymentSentRequest]
          response <- service.markPaymentSent(id, membershipNumber, payload.sent, session).flatMap(result => toEventResponse(result, Status.Ok))
        yield response
      }

    case request @ PATCH -> Root / LongVar(id) / "bookings" / membershipNumber / "payment-received" =>
      requireSession(request) { session =>
        for
          payload <- request.as[PaymentReceivedRequest]
          response <- service.markPaymentReceived(id, membershipNumber, payload.received, session).flatMap(result => toEventResponse(result, Status.Ok))
        yield response
      }
  }

  private object ScopeQueryParamMatcher extends OptionalQueryParamDecoderMatcher[String]("scope")

  private def currentSession(request: org.http4s.Request[IO]): Option[AuthenticatedSession] =
    SessionCookieSupport.sessionFromRequest(request)

  private def requireSession(request: org.http4s.Request[IO])(f: AuthenticatedSession => IO[org.http4s.Response[IO]]) =
    currentSession(request) match
      case None          => unauthorized(AuthError.LoginRequired.message)
      case Some(session) => f(session)

  private def toEventResponse[A: Encoder](
      result: Either[AuthError | EventError, A],
      successStatus: Status
  ): IO[org.http4s.Response[IO]] =
    result match
      case Right(value) => IO.pure(Response[IO](status = successStatus).withEntity(value))
      case Left(error: AuthError) =>
        error match
          case AuthError.LoginRequired                 => unauthorized(error.message)
          case AuthError.CoachOrCommitteeRequired      => Forbidden(Map("error" -> error.message))
          case AuthError.CommitteeRoleRequired         => Forbidden(Map("error" -> error.message))
          case AuthError.CommitteeVerificationRequired => Forbidden(Map("error" -> error.message))
          case AuthError.InvalidCommitteePassword      => Forbidden(Map("error" -> error.message))
          case AuthError.InactiveMembership            => Forbidden(Map("error" -> error.message))
          case AuthError.MembershipNotFound            => unauthorized(error.message)
          case AuthError.MemberRoleRequired            => Forbidden(Map("error" -> error.message))
      case Left(error: EventError) =>
        eventErrorResponse(error)

  private def eventErrorResponse(error: EventError): IO[org.http4s.Response[IO]] =
    error match
      case EventError.NotFound(_)          => NotFound(Map("error" -> error.message))
      case EventError.BookingNotFound(_, _) => NotFound(Map("error" -> error.message))
      case EventError.Validation(_)        => BadRequest(Map("error" -> error.message))
      case EventError.EventCancelled(_)    => Conflict(Map("error" -> error.message))
      case EventError.EventFull(_)         => Conflict(Map("error" -> error.message))
      case EventError.Persistence(_)       => InternalServerError(Map("error" -> error.message))

  private def unauthorized(message: String): IO[Response[IO]] =
    IO.pure(Response[IO](status = Status.Unauthorized).withEntity(Map("error" -> message)))
