package com.edgeprogressivepaddling.booking.domain

import io.circe.Codec

final case class Event(
    id: Long,
    eventType: EventType,
    title: String,
    eventDate: String,
    endDate: Option[String],
    meetTime: Option[String],
    endTime: Option[String],
    locationType: Option[LocationType],
    locationText: Option[String],
    destination: Option[String],
    tideInfo: Option[String],
    maxParticipants: Int,
    description: Option[String],
    extraInfo: Option[String],
    status: EventStatus,
    createdByMembershipNumber: String,
    createdAt: Long,
    updatedAt: Long
) derives Codec.AsObject

final case class CreateSessionEventRequest(
    eventDate: String,
    meetTime: String,
    endTime: String,
    locationType: LocationType,
    locationText: Option[String],
    destination: Option[String],
    tideInfo: Option[String],
    maxParticipants: Int,
    extraInfo: Option[String]
) derives Codec.AsObject

final case class CreateTripEventRequest(
    title: String,
    eventDate: String,
    endDate: Option[String],
    maxParticipants: Int,
    description: String
) derives Codec.AsObject

final case class UpdateEventRequest(
    title: String,
    eventDate: String,
    endDate: Option[String],
    meetTime: Option[String],
    endTime: Option[String],
    locationType: Option[LocationType],
    locationText: Option[String],
    destination: Option[String],
    tideInfo: Option[String],
    maxParticipants: Int,
    description: Option[String],
    extraInfo: Option[String]
) derives Codec.AsObject

final case class CreateBookingRequest(note: Option[String]) derives Codec.AsObject

final case class PaymentSentRequest(sent: Boolean) derives Codec.AsObject

final case class PaymentReceivedRequest(received: Boolean) derives Codec.AsObject

final case class EventBooking(
    id: Long,
    eventId: Long,
    membershipNumber: String,
    memberName: String,
    bookingStatus: BookingStatus,
    note: Option[String],
    paymentSent: Option[Boolean],
    paymentReceived: Option[Boolean],
    createdAt: Long,
    updatedAt: Long
) derives Codec.AsObject

final case class EventDetail(
    event: Event,
    attendees: List[EventBooking],
    spotsRemaining: Int,
    currentUserBooking: Option[EventBooking]
) derives Codec.AsObject
