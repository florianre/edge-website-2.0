package com.edgeprogressivepaddling.booking.service

import cats.effect.IO
import com.edgeprogressivepaddling.booking.domain.{AuthenticatedSession, LoginRequest, MembershipRole, MembershipStatus}

final class AuthService(
    membershipService: MembershipService[IO],
    committeePassword: String
):
  def login(request: LoginRequest): IO[Either[AuthError, AuthenticatedSession]] =
    membershipService.getByMembershipNumber(request.membershipNumber).map {
      case None => Left(AuthError.MembershipNotFound)
      case Some(member) if member.status != MembershipStatus.Active =>
        Left(AuthError.InactiveMembership)
      case Some(member) =>
        Right(
          AuthenticatedSession(
            membershipNumber = member.membershipNumber,
            name = member.name,
            role = member.role,
            committeeVerified = false
          )
        )
    }

  def verifyCommitteePassword(
      session: AuthenticatedSession,
      password: String
  ): Either[AuthError, AuthenticatedSession] =
    if session.role != MembershipRole.Committee then Left(AuthError.CommitteeRoleRequired)
    else if password == committeePassword then Right(session.copy(committeeVerified = true))
    else Left(AuthError.InvalidCommitteePassword)
