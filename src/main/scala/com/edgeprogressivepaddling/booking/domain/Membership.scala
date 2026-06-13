package com.edgeprogressivepaddling.booking.domain

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class Membership(
    membershipNumber: String,
    name: String,
    role: MembershipRole,
    status: MembershipStatus
)

object Membership:
  given Encoder[Membership] = deriveEncoder[Membership]
  given Decoder[Membership] = deriveDecoder[Membership]

final case class CreateMembershipRequest(
    membershipNumber: String,
    name: String,
    role: MembershipRole,
    status: MembershipStatus = MembershipStatus.Active
)

object CreateMembershipRequest:
  given Encoder[CreateMembershipRequest] = deriveEncoder[CreateMembershipRequest]
  given Decoder[CreateMembershipRequest] = deriveDecoder[CreateMembershipRequest]

final case class UpdateMembershipRequest(
    name: String,
    role: MembershipRole
)

object UpdateMembershipRequest:
  given Encoder[UpdateMembershipRequest] = deriveEncoder[UpdateMembershipRequest]
  given Decoder[UpdateMembershipRequest] = deriveDecoder[UpdateMembershipRequest]
