package com.edgeprogressivepaddling.booking.routes

import cats.effect.IO
import com.edgeprogressivepaddling.booking.auth.SessionCookieSupport
import com.edgeprogressivepaddling.booking.domain.AuthenticatedSession
import com.edgeprogressivepaddling.booking.domain.{
  CreateMembershipRequest,
  MembershipRole,
  MembershipStatus,
  UpdateMembershipRequest
}
import com.edgeprogressivepaddling.booking.service.{AuthError, MembershipError, MembershipSearchCriteria, MembershipService}
import org.http4s.HttpRoutes
import org.http4s.{Response, Status}
import org.http4s.circe.{CirceEntityCodec, jsonOf}
import org.http4s.dsl.io.*

final class MembershipRoutes(service: MembershipService[IO]):
  import CirceEntityCodec.*

  given org.http4s.EntityDecoder[IO, CreateMembershipRequest] = jsonOf[IO, CreateMembershipRequest]
  given org.http4s.EntityDecoder[IO, UpdateMembershipRequest] = jsonOf[IO, UpdateMembershipRequest]

  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case request @ GET -> Root => {
      requireCoachOrCommittee(request) {
        buildSearchCriteria(request.params) match
          case Left(error)     => BadRequest(errorBody(error))
          case Right(criteria) => service.search(criteria).flatMap(Ok(_))
      }
    }

    case request @ GET -> Root / membershipNumber =>
      requireCoachOrCommittee(request) {
        service.getByMembershipNumber(membershipNumber).flatMap {
          case Some(membership) => Ok(membership)
          case None             => NotFound(errorBody(s"Membership $membershipNumber was not found"))
        }
      }

    case request @ POST -> Root =>
      requireCommitteeVerified(request) {
        for
          payload <- request.as[CreateMembershipRequest]
          response <- service.create(payload).flatMap {
            case Right(membership) => Created(membership)
            case Left(error)       => toResponse(error)
          }
        yield response
      }

    case request @ PUT -> Root / membershipNumber =>
      requireCommitteeVerified(request) {
        for
          payload <- request.as[UpdateMembershipRequest]
          response <- service.update(membershipNumber, payload).flatMap {
            case Right(membership) => Ok(membership)
            case Left(error)       => toResponse(error)
          }
        yield response
      }

    case request @ DELETE -> Root / membershipNumber =>
      requireCommitteeVerified(request) {
        service.delete(membershipNumber).flatMap {
          case Right(_)    => NoContent()
          case Left(error) => toResponse(error)
        }
      }

    case request @ PATCH -> Root / membershipNumber / "activate" =>
      requireCommitteeVerified(request) {
        service.activate(membershipNumber).flatMap {
          case Right(membership) => Ok(membership)
          case Left(error)       => toResponse(error)
        }
      }

    case request @ PATCH -> Root / membershipNumber / "deactivate" =>
      requireCommitteeVerified(request) {
        service.deactivate(membershipNumber).flatMap {
          case Right(membership) => Ok(membership)
          case Left(error)       => toResponse(error)
        }
      }
  }

  private def toResponse(error: MembershipError) =
    error match
      case MembershipError.NotFound(_)                  => NotFound(errorBody(error.message))
      case MembershipError.DuplicateMembershipNumber(_) => Conflict(errorBody(error.message))
      case MembershipError.Validation(_)                => BadRequest(errorBody(error.message))
      case MembershipError.Persistence(_)               => InternalServerError(errorBody(error.message))

  private def errorBody(message: String): Map[String, String] =
    Map("error" -> message)

  private def requireCoachOrCommittee(request: org.http4s.Request[IO])(onAuthorized: => IO[org.http4s.Response[IO]]) =
    sessionFromRequest(request) match
      case None => unauthorized(AuthError.LoginRequired.message)
      case Some(session) if session.role == MembershipRole.Coach || session.role == MembershipRole.Committee =>
        onAuthorized
      case Some(_) =>
        Forbidden(errorBody(AuthError.CoachOrCommitteeRequired.message))

  private def requireCommitteeVerified(request: org.http4s.Request[IO])(onAuthorized: => IO[org.http4s.Response[IO]]) =
    sessionFromRequest(request) match
      case None => unauthorized(AuthError.LoginRequired.message)
      case Some(session) if session.role != MembershipRole.Committee =>
        Forbidden(errorBody(AuthError.CommitteeRoleRequired.message))
      case Some(session) if !session.committeeVerified =>
        Forbidden(errorBody(AuthError.CommitteeVerificationRequired.message))
      case Some(_) => onAuthorized

  private def sessionFromRequest(request: org.http4s.Request[IO]): Option[AuthenticatedSession] =
    SessionCookieSupport.sessionFromRequest(request)

  private def unauthorized(message: String): IO[Response[IO]] =
    IO.pure(Response[IO](status = Status.Unauthorized).withEntity(errorBody(message)))

  // The service deals with typed search criteria; the route owns parsing and validating
  // raw query strings into domain values.
  private def buildSearchCriteria(
      params: Map[String, String]
  ): Either[String, MembershipSearchCriteria] =
    for
      role <- parseOptionalRole(params.get("role"))
      status <- parseOptionalStatus(params.get("status"))
    yield MembershipSearchCriteria(
      membershipNumber = sanitizeOptional(params.get("membershipNumber")),
      name = sanitizeOptional(params.get("name")),
      role = role,
      status = status
    )

  private def parseOptionalRole(value: Option[String]): Either[String, Option[MembershipRole]] =
    value match
      case None => Right(None)
      case Some(rawValue) =>
        MembershipRole.fromString(rawValue).left.map(identity).map(Some(_))

  private def parseOptionalStatus(value: Option[String]): Either[String, Option[MembershipStatus]] =
    value match
      case None => Right(None)
      case Some(rawValue) =>
        MembershipStatus.fromString(rawValue).left.map(identity).map(Some(_))

  private def sanitizeOptional(value: Option[String]): Option[String] =
    value.map(_.trim).filter(_.nonEmpty)
