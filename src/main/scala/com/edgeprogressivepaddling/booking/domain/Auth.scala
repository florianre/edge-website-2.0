package com.edgeprogressivepaddling.booking.domain

import io.circe.Codec

final case class AuthenticatedSession(
    membershipNumber: String,
    name: String,
    role: MembershipRole,
    committeeVerified: Boolean = false
) derives Codec.AsObject

final case class LoginRequest(membershipNumber: String) derives Codec.AsObject

final case class CommitteePasswordRequest(password: String) derives Codec.AsObject

final case class SessionResponse(currentUser: Option[AuthenticatedSession]) derives Codec.AsObject
