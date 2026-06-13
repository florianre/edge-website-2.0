package com.edgeprogressivepaddling.booking.routes

import cats.effect.IO
import com.edgeprogressivepaddling.booking.domain.{
  CreateMembershipRequest,
  MembershipRole,
  MembershipStatus,
  UpdateMembershipRequest
}
import com.edgeprogressivepaddling.booking.service.{MembershipError, MembershipSearchCriteria, MembershipService}
import org.http4s.HttpRoutes
import org.http4s.circe.{CirceEntityCodec, jsonOf}
import org.http4s.dsl.io.*

final class MembershipRoutes(service: MembershipService[IO]):
  import CirceEntityCodec.*

  given org.http4s.EntityDecoder[IO, CreateMembershipRequest] = jsonOf[IO, CreateMembershipRequest]
  given org.http4s.EntityDecoder[IO, UpdateMembershipRequest] = jsonOf[IO, UpdateMembershipRequest]

  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case request @ GET -> Root / "memberships" => {
      buildSearchCriteria(request.params) match
        case Left(error)     => BadRequest(errorBody(error))
        case Right(criteria) => service.search(criteria).flatMap(Ok(_))
    }

    case GET -> Root / "memberships" / membershipNumber =>
      service.getByMembershipNumber(membershipNumber).flatMap {
        case Some(membership) => Ok(membership)
        case None             => NotFound(errorBody(s"Membership $membershipNumber was not found"))
      }

    case request @ POST -> Root / "memberships" =>
      for
        payload <- request.as[CreateMembershipRequest]
        response <- service.create(payload).flatMap {
          case Right(membership) => Created(membership)
          case Left(error)       => toResponse(error)
        }
      yield response

    case request @ PUT -> Root / "memberships" / membershipNumber =>
      for
        payload <- request.as[UpdateMembershipRequest]
        response <- service.update(membershipNumber, payload).flatMap {
          case Right(membership) => Ok(membership)
          case Left(error)       => toResponse(error)
        }
      yield response

    case DELETE -> Root / "memberships" / membershipNumber =>
      service.delete(membershipNumber).flatMap {
        case Right(_)    => NoContent()
        case Left(error) => toResponse(error)
      }

    case PATCH -> Root / "memberships" / membershipNumber / "activate" =>
      service.activate(membershipNumber).flatMap {
        case Right(membership) => Ok(membership)
        case Left(error)       => toResponse(error)
      }

    case PATCH -> Root / "memberships" / membershipNumber / "deactivate" =>
      service.deactivate(membershipNumber).flatMap {
        case Right(membership) => Ok(membership)
        case Left(error)       => toResponse(error)
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
