package com.edgeprogressivepaddling.booking.service

sealed trait EventError:
  def message: String

object EventError:
  final case class Validation(message: String) extends EventError
  final case class NotFound(id: Long) extends EventError:
    val message = s"Event $id was not found"
  final case class BookingNotFound(eventId: Long, membershipNumber: String) extends EventError:
    val message = s"No booking for membership $membershipNumber on event $eventId"
  final case class EventCancelled(id: Long) extends EventError:
    val message = s"Event $id has been cancelled"
  final case class EventFull(id: Long) extends EventError:
    val message = s"Event $id is full"
  final case class Persistence(message: String) extends EventError
