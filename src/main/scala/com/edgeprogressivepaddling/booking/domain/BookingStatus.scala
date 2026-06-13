package com.edgeprogressivepaddling.booking.domain

import io.circe.{Decoder, Encoder}

enum BookingStatus:
  case Going
  case Cancelled

object BookingStatus:
  def fromString(value: String): Either[String, BookingStatus] =
    value.trim.toLowerCase match
      case "going"     => Right(BookingStatus.Going)
      case "cancelled" => Right(BookingStatus.Cancelled)
      case other       => Left(s"Unsupported booking status: $other")

  given Encoder[BookingStatus] =
    Encoder.encodeString.contramap {
      case BookingStatus.Going     => "going"
      case BookingStatus.Cancelled => "cancelled"
    }

  given Decoder[BookingStatus] =
    Decoder.decodeString.emap(fromString)
