package com.edgeprogressivepaddling.booking.service

import com.edgeprogressivepaddling.booking.domain.{MembershipRole, MembershipStatus}

final case class MembershipSearchCriteria(
    membershipNumber: Option[String] = None,
    name: Option[String] = None,
    role: Option[MembershipRole] = None,
    status: Option[MembershipStatus] = None
)
