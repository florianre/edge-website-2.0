package com.edgeprogressivepaddling.booking.domain

import io.circe.Codec

final case class Membership(
    membershipNumber: String,
    name: String,
    role: MembershipRole,
    status: MembershipStatus
) derives Codec.AsObject

final case class CreateMembershipRequest(
    name: String,
    role: MembershipRole,
    status: MembershipStatus = MembershipStatus.Active
) derives Codec.AsObject

final case class UpdateMembershipRequest(
    name: String,
    role: MembershipRole
) derives Codec.AsObject
