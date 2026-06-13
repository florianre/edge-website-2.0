package com.edgeprogressivepaddling.booking.service

sealed trait MembershipError:
  def message: String

object MembershipError:
  final case class NotFound(membershipNumber: String) extends MembershipError:
    override val message: String = s"Membership $membershipNumber was not found"

  final case class DuplicateMembershipNumber(membershipNumber: String) extends MembershipError:
    override val message: String = s"Membership $membershipNumber already exists"

  final case class Validation(message: String) extends MembershipError

  final case class Persistence(message: String) extends MembershipError
