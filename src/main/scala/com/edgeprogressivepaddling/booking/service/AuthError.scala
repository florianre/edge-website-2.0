package com.edgeprogressivepaddling.booking.service

sealed trait AuthError:
  def message: String

object AuthError:
  case object LoginRequired extends AuthError:
    val message = "You must be logged in to perform this action"

  case object InactiveMembership extends AuthError:
    val message = "Membership is inactive"

  case object MembershipNotFound extends AuthError:
    val message = "Membership number was not found"

  case object CommitteeRoleRequired extends AuthError:
    val message = "Committee role is required for this action"

  case object CommitteeVerificationRequired extends AuthError:
    val message = "Committee password verification is required"

  case object InvalidCommitteePassword extends AuthError:
    val message = "Committee password was invalid"

  case object CoachOrCommitteeRequired extends AuthError:
    val message = "Coach or committee role is required for this action"

  case object MemberRoleRequired extends AuthError:
    val message = "A valid member login is required for this action"
