package com.edgeprogressivepaddling.booking.repository

import com.edgeprogressivepaddling.booking.domain.{Event, EventBooking}

trait EventRepository[F[_]]:
  def initialise: F[Unit]
  def listUpcoming(today: String): F[List[Event]]
  def listPast(today: String): F[List[Event]]
  def getById(id: Long): F[Option[Event]]
  def create(event: Event): F[Event]
  def update(event: Event): F[Event]
  def listBookings(eventId: Long): F[List[EventBooking]]
  def upsertBooking(booking: EventBooking): F[EventBooking]
  def getBooking(eventId: Long, membershipNumber: String): F[Option[EventBooking]]
