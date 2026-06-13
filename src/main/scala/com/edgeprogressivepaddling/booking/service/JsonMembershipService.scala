package com.edgeprogressivepaddling.booking.service

import cats.effect.IO
import cats.effect.kernel.Ref
import cats.effect.std.Semaphore
import com.edgeprogressivepaddling.booking.domain.{
  CreateMembershipRequest,
  Membership,
  MembershipStatus,
  UpdateMembershipRequest
}
import com.edgeprogressivepaddling.booking.service.MembershipError.*
import io.circe.parser.decode
import io.circe.syntax.EncoderOps
import io.circe.Printer

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

final class JsonMembershipService private (
    filePath: Path,
    state: Ref[IO, Vector[Membership]],
    writeGuard: Semaphore[IO]
) extends MembershipService[IO]:

  override def getAll: IO[List[Membership]] =
    state.get.map(_.sortBy(_.membershipNumber).toList)

  override def getByMembershipNumber(membershipNumber: String): IO[Option[Membership]] =
    state.get.map(_.find(_.membershipNumber == sanitizeMembershipNumber(membershipNumber)))

  override def create(request: CreateMembershipRequest): IO[Either[MembershipError, Membership]] =
    validateCreateRequest(request) match
      case Left(error) => IO.pure(Left(error))
      case Right(validRequest) =>
        mutate { current =>
          val newMembership = Membership(
            membershipNumber = sanitizeMembershipNumber(validRequest.membershipNumber),
            name = sanitizeName(validRequest.name),
            role = validRequest.role,
            status = validRequest.status
          )

          if current.exists(_.membershipNumber == newMembership.membershipNumber) then
            Left(DuplicateMembershipNumber(newMembership.membershipNumber))
          else
            Right((current :+ newMembership, newMembership))
        }

  override def update(
      membershipNumber: String,
      request: UpdateMembershipRequest
  ): IO[Either[MembershipError, Membership]] =
    validateUpdateRequest(request) match
      case Left(error) => IO.pure(Left(error))
      case Right(validRequest) =>
        val sanitizedMembershipNumber = sanitizeMembershipNumber(membershipNumber)

        mutate { current =>
          current.indexWhere(_.membershipNumber == sanitizedMembershipNumber) match
            case -1 => Left(NotFound(sanitizedMembershipNumber))
            case index =>
              val existing = current(index)
              val updated = existing.copy(
                name = sanitizeName(validRequest.name),
                role = validRequest.role
              )
              Right((current.updated(index, updated), updated))
        }

  override def delete(membershipNumber: String): IO[Either[MembershipError, Unit]] =
    val sanitizedMembershipNumber = sanitizeMembershipNumber(membershipNumber)

    mutate { current =>
      if current.exists(_.membershipNumber == sanitizedMembershipNumber) then
        Right((current.filterNot(_.membershipNumber == sanitizedMembershipNumber), ()))
      else
        Left(NotFound(sanitizedMembershipNumber))
    }

  override def activate(membershipNumber: String): IO[Either[MembershipError, Membership]] =
    updateStatus(membershipNumber, MembershipStatus.Active)

  override def deactivate(membershipNumber: String): IO[Either[MembershipError, Membership]] =
    updateStatus(membershipNumber, MembershipStatus.Inactive)

  private def updateStatus(
      membershipNumber: String,
      newStatus: MembershipStatus
  ): IO[Either[MembershipError, Membership]] =
    val sanitizedMembershipNumber = sanitizeMembershipNumber(membershipNumber)

    mutate { current =>
      current.indexWhere(_.membershipNumber == sanitizedMembershipNumber) match
        case -1 => Left(NotFound(sanitizedMembershipNumber))
        case index =>
          val updated = current(index).copy(status = newStatus)
          Right((current.updated(index, updated), updated))
    }

  private def mutate[A](
      transform: Vector[Membership] => Either[MembershipError, (Vector[Membership], A)]
  ): IO[Either[MembershipError, A]] =
    writeGuard.permit.use { _ =>
      for
        current <- state.get
        result <- transform(current) match
          case Left(error) => IO.pure(Left(error))
          case Right((updatedState, value)) =>
            persist(updatedState).flatMap {
              case Left(error) => IO.pure(Left(error))
              case Right(_)    => state.set(updatedState).as(Right(value))
            }
      yield result
    }

  private def persist(memberships: Vector[Membership]): IO[Either[MembershipError, Unit]] =
    IO.blocking {
      val parent = Option(filePath.getParent)
      parent.foreach(path => Files.createDirectories(path))

      val json = Printer.spaces2.print(memberships.asJson)
      Files.writeString(filePath, s"$json\n", StandardCharsets.UTF_8)
    }.attempt.map {
      case Left(exception) => Left(Persistence(s"Failed to persist membership data: ${exception.getMessage}"))
      case Right(_)        => Right(())
    }

  private def validateCreateRequest(
      request: CreateMembershipRequest
  ): Either[MembershipError, CreateMembershipRequest] =
    for
      membershipNumber <- requireNonEmpty(request.membershipNumber, "membershipNumber")
      name <- requireNonEmpty(request.name, "name")
    yield request.copy(
      membershipNumber = membershipNumber,
      name = name
    )

  private def validateUpdateRequest(
      request: UpdateMembershipRequest
  ): Either[MembershipError, UpdateMembershipRequest] =
    requireNonEmpty(request.name, "name").map(validName => request.copy(name = validName))

  private def requireNonEmpty(value: String, fieldName: String): Either[MembershipError, String] =
    val sanitizedValue = value.trim
    if sanitizedValue.nonEmpty then Right(sanitizedValue)
    else Left(Validation(s"$fieldName must not be empty"))

  private def sanitizeMembershipNumber(value: String): String =
    value.trim

  private def sanitizeName(value: String): String =
    value.trim

object JsonMembershipService:
  def create(filePath: Path): IO[JsonMembershipService] =
    for
      memberships <- loadMemberships(filePath)
      state <- Ref.of[IO, Vector[Membership]](memberships)
      writeGuard <- Semaphore[IO](1)
    yield JsonMembershipService(filePath, state, writeGuard)

  private def loadMemberships(filePath: Path): IO[Vector[Membership]] =
    IO.blocking {
      val parent = Option(filePath.getParent)
      parent.foreach(path => Files.createDirectories(path))

      if Files.notExists(filePath) then
        Files.writeString(filePath, "[]\n", StandardCharsets.UTF_8)

      Files.readString(filePath, StandardCharsets.UTF_8)
    }.flatMap { rawJson =>
      val content = rawJson.trim

      if content.isEmpty then IO.pure(Vector.empty)
      else
        decode[Vector[Membership]](content) match
          case Left(error) =>
            IO.raiseError(
              IllegalStateException(s"Failed to decode membership file ${filePath.toAbsolutePath}: ${error.getMessage}")
            )
          case Right(memberships) => IO.pure(memberships)
    }
