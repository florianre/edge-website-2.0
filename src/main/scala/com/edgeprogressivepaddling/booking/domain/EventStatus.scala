package com.edgeprogressivepaddling.booking.domain

import io.circe.{Decoder, Encoder}

enum EventStatus:
  case Active
  case Cancelled

object EventStatus:
  def fromString(value: String): Either[String, EventStatus] =
    value.trim.toLowerCase match
      case "active"    => Right(EventStatus.Active)
      case "cancelled" => Right(EventStatus.Cancelled)
      case other       => Left(s"Unsupported event status: $other")

  given Encoder[EventStatus] =
    Encoder.encodeString.contramap {
      case EventStatus.Active    => "active"
      case EventStatus.Cancelled => "cancelled"
    }

  given Decoder[EventStatus] =
    Decoder.decodeString.emap(fromString)
