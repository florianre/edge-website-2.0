package com.edgeprogressivepaddling.booking.domain

import io.circe.{Decoder, Encoder}

enum MembershipRole:
  case Member
  case Coach
  case Committee

object MembershipRole:
  def fromString(value: String): Either[String, MembershipRole] =
    value.trim.toLowerCase match
      case "member"    => Right(MembershipRole.Member)
      case "coach"     => Right(MembershipRole.Coach)
      case "committee" => Right(MembershipRole.Committee)
      case other       => Left(s"Unsupported membership role: $other")

  given Encoder[MembershipRole] =
    Encoder.encodeString.contramap {
      case MembershipRole.Member    => "member"
      case MembershipRole.Coach     => "coach"
      case MembershipRole.Committee => "committee"
    }

  given Decoder[MembershipRole] =
    Decoder.decodeString.emap(fromString)
