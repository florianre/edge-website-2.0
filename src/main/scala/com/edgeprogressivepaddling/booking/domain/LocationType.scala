package com.edgeprogressivepaddling.booking.domain

import io.circe.{Decoder, Encoder}

enum LocationType:
  case Canal
  case KewBridge
  case Other

object LocationType:
  def fromString(value: String): Either[String, LocationType] =
    value.trim.toLowerCase match
      case "canal"      => Right(LocationType.Canal)
      case "kew_bridge" => Right(LocationType.KewBridge)
      case "other"      => Right(LocationType.Other)
      case other        => Left(s"Unsupported location type: $other")

  given Encoder[LocationType] =
    Encoder.encodeString.contramap {
      case LocationType.Canal     => "canal"
      case LocationType.KewBridge => "kew_bridge"
      case LocationType.Other     => "other"
    }

  given Decoder[LocationType] =
    Decoder.decodeString.emap(fromString)
