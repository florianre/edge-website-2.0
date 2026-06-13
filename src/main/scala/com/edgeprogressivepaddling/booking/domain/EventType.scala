package com.edgeprogressivepaddling.booking.domain

import io.circe.{Decoder, Encoder}

enum EventType:
  case Session
  case Trip

object EventType:
  def fromString(value: String): Either[String, EventType] =
    value.trim.toLowerCase match
      case "session" => Right(EventType.Session)
      case "trip"    => Right(EventType.Trip)
      case other     => Left(s"Unsupported event type: $other")

  given Encoder[EventType] =
    Encoder.encodeString.contramap {
      case EventType.Session => "session"
      case EventType.Trip    => "trip"
    }

  given Decoder[EventType] =
    Decoder.decodeString.emap(fromString)
