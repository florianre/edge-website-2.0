package com.edgeprogressivepaddling.booking.routes

import cats.effect.IO
import com.edgeprogressivepaddling.booking.domain.{CreateMembershipRequest, UpdateMembershipRequest}
import com.edgeprogressivepaddling.booking.service.{MembershipError, MembershipService}
import org.http4s.HttpRoutes
import org.http4s.circe.{CirceEntityCodec, jsonOf}
import org.http4s.dsl.io.*

final class MembershipRoutes(service: MembershipService[IO]):
  import CirceEntityCodec.*

  given org.http4s.EntityDecoder[IO, CreateMembershipRequest] = jsonOf[IO, CreateMembershipRequest]
  given org.http4s.EntityDecoder[IO, UpdateMembershipRequest] = jsonOf[IO, UpdateMembershipRequest]

  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "memberships" =>
      service.getAll.flatMap(Ok(_))

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
