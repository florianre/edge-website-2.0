package com.edgeprogressivepaddling.booking.domain

import io.circe.{Decoder, Encoder}

enum MembershipStatus:
  case Active
  case Inactive

object MembershipStatus:
  def fromString(value: String): Either[String, MembershipStatus] =
    value.trim.toLowerCase match
      case "active"   => Right(MembershipStatus.Active)
      case "inactive" => Right(MembershipStatus.Inactive)
      case other      => Left(s"Unsupported membership status: $other")

  given Encoder[MembershipStatus] =
    Encoder.encodeString.contramap {
      case MembershipStatus.Active   => "active"
      case MembershipStatus.Inactive => "inactive"
    }

  given Decoder[MembershipStatus] =
    Decoder.decodeString.emap(fromString)
