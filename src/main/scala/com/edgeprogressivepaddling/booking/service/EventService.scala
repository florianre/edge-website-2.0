package com.edgeprogressivepaddling.booking.service

import cats.effect.IO
import com.edgeprogressivepaddling.booking.domain.*
import com.edgeprogressivepaddling.booking.repository.EventRepository

import java.time.LocalDate

final class EventService(
    repository: EventRepository[IO],
    membershipService: MembershipService[IO]
):
  def initialise: IO[Unit] = repository.initialise

  def listUpcoming: IO[List[Event]] =
    repository.listUpcoming(today)

  def listPast: IO[List[Event]] =
    repository.listPast(today)

  def getEventDetail(id: Long, session: Option[AuthenticatedSession]): IO[Either[EventError, EventDetail]] =
    repository.getById(id).flatMap {
      case None => IO.pure(Left(EventError.NotFound(id)))
      case Some(event) =>
        repository.listBookings(id).map { bookings =>
          val activeBookings = bookings.filter(_.bookingStatus == BookingStatus.Going)
          Right(
            EventDetail(
              event = event,
              attendees = activeBookings,
              spotsRemaining = (event.maxParticipants - activeBookings.size).max(0),
              currentUserBooking = session.flatMap(user =>
                activeBookings.find(_.membershipNumber == user.membershipNumber)
              )
            )
          )
        }
    }

  def createSession(
      request: CreateSessionEventRequest,
      session: AuthenticatedSession
  ): IO[Either[AuthError | EventError, Event]] =
    authorizeCoachOrCommittee(session) match
      case Left(error) => IO.pure(Left(error))
      case Right(_) =>
        validateSessionRequest(request).flatMap {
          case Left(error) => IO.pure(Left(error))
          case Right(validRequest) =>
            val now = System.currentTimeMillis()
            repository
              .create(
                Event(
                  id = 0L,
                  eventType = EventType.Session,
                  title = buildSessionTitle(validRequest),
                  eventDate = validRequest.eventDate,
                  endDate = None,
                  meetTime = Some(validRequest.meetTime),
                  endTime = Some(validRequest.endTime),
                  locationType = Some(validRequest.locationType),
                  locationText = validRequest.locationText,
                  destination = validRequest.destination,
                  tideInfo = validRequest.tideInfo,
                  maxParticipants = validRequest.maxParticipants,
                  description = None,
                  extraInfo = validRequest.extraInfo,
                  status = EventStatus.Active,
                  createdByMembershipNumber = session.membershipNumber,
                  createdAt = now,
                  updatedAt = now
                )
              )
              .map(Right(_))
        }

  def createTrip(
      request: CreateTripEventRequest,
      session: AuthenticatedSession
  ): IO[Either[AuthError | EventError, Event]] =
    authorizeCoachOrCommittee(session) match
      case Left(error) => IO.pure(Left(error))
      case Right(_) =>
        validateTripRequest(request).flatMap {
          case Left(error) => IO.pure(Left(error))
          case Right(validRequest) =>
            val now = System.currentTimeMillis()
            repository
              .create(
                Event(
                  id = 0L,
                  eventType = EventType.Trip,
                  title = validRequest.title.trim,
                  eventDate = validRequest.eventDate,
                  endDate = validRequest.endDate,
                  meetTime = None,
                  endTime = None,
                  locationType = None,
                  locationText = None,
                  destination = None,
                  tideInfo = None,
                  maxParticipants = validRequest.maxParticipants,
                  description = Some(validRequest.description.trim),
                  extraInfo = None,
                  status = EventStatus.Active,
                  createdByMembershipNumber = session.membershipNumber,
                  createdAt = now,
                  updatedAt = now
                )
              )
              .map(Right(_))
        }

  def updateEvent(
      id: Long,
      request: UpdateEventRequest,
      session: AuthenticatedSession
  ): IO[Either[AuthError | EventError, Event]] =
    authorizeCoachOrCommittee(session) match
      case Left(error) => IO.pure(Left(error))
      case Right(_) =>
        repository.getById(id).flatMap {
          case None => IO.pure(Left(EventError.NotFound(id)))
          case Some(event) =>
            val updated = event.copy(
              title = request.title.trim,
              eventDate = request.eventDate.trim,
              endDate = request.endDate.map(_.trim).filter(_.nonEmpty),
              meetTime = request.meetTime.map(_.trim).filter(_.nonEmpty),
              endTime = request.endTime.map(_.trim).filter(_.nonEmpty),
              locationType = request.locationType,
              locationText = request.locationText.map(_.trim).filter(_.nonEmpty),
              destination = request.destination.map(_.trim).filter(_.nonEmpty),
              tideInfo = request.tideInfo.map(_.trim).filter(_.nonEmpty),
              maxParticipants = request.maxParticipants,
              description = request.description.map(_.trim).filter(_.nonEmpty),
              extraInfo = request.extraInfo.map(_.trim).filter(_.nonEmpty),
              updatedAt = System.currentTimeMillis()
            )
            repository.update(updated).map(Right(_))
        }

  def cancelEvent(id: Long, session: AuthenticatedSession): IO[Either[AuthError | EventError, Event]] =
    authorizeCoachOrCommittee(session) match
      case Left(error) => IO.pure(Left(error))
      case Right(_) =>
        repository.getById(id).flatMap {
          case None => IO.pure(Left(EventError.NotFound(id)))
          case Some(event) =>
            repository.update(event.copy(status = EventStatus.Cancelled, updatedAt = System.currentTimeMillis())).map(Right(_))
        }

  def bookEvent(
      id: Long,
      request: CreateBookingRequest,
      session: AuthenticatedSession
  ): IO[Either[AuthError | EventError, EventBooking]] =
    authorizeLoggedIn(session) match
      case Left(error) => IO.pure(Left(error))
      case Right(_) =>
        repository.getById(id).flatMap {
          case None => IO.pure(Left(EventError.NotFound(id)))
          case Some(event) if event.status == EventStatus.Cancelled =>
            IO.pure(Left(EventError.EventCancelled(id)))
          case Some(event) =>
            for
              details <- getEventDetail(id, Some(session))
              result <- details match
                case Left(error) => IO.pure(Left(error))
                case Right(detail) if detail.spotsRemaining <= 0 && detail.currentUserBooking.isEmpty =>
                  IO.pure(Left(EventError.EventFull(id)))
                case Right(detail) =>
                  val now = System.currentTimeMillis()
                  val existingId = detail.currentUserBooking.map(_.id).getOrElse(0L)
                  val booking = EventBooking(
                    id = existingId,
                    eventId = id,
                    membershipNumber = session.membershipNumber,
                    memberName = session.name,
                    bookingStatus = BookingStatus.Going,
                    note = request.note.map(_.trim).filter(_.nonEmpty),
                    paymentSent = if event.eventType == EventType.Trip then Some(false) else None,
                    paymentReceived = if event.eventType == EventType.Trip then Some(false) else None,
                    createdAt = detail.currentUserBooking.map(_.createdAt).getOrElse(now),
                    updatedAt = now
                  )
                  repository.upsertBooking(booking).map(Right(_))
            yield result
        }

  def removeBooking(
      id: Long,
      membershipNumber: String,
      session: AuthenticatedSession
  ): IO[Either[AuthError | EventError, EventBooking]] =
    if session.membershipNumber != membershipNumber && !isCoachOrCommittee(session) then
      IO.pure(Left(AuthError.MemberRoleRequired))
    else
      repository.getBooking(id, membershipNumber).flatMap {
        case None => IO.pure(Left(EventError.BookingNotFound(id, membershipNumber)))
        case Some(booking) =>
          repository
            .upsertBooking(booking.copy(bookingStatus = BookingStatus.Cancelled, updatedAt = System.currentTimeMillis()))
            .map(Right(_))
      }

  def markPaymentSent(
      id: Long,
      membershipNumber: String,
      sent: Boolean,
      session: AuthenticatedSession
  ): IO[Either[AuthError | EventError, EventBooking]] =
    if session.membershipNumber != membershipNumber && !isCoachOrCommittee(session) then
      IO.pure(Left(AuthError.MemberRoleRequired))
    else
      updateBooking(id, membershipNumber) { booking =>
        booking.copy(paymentSent = Some(sent), updatedAt = System.currentTimeMillis())
      }

  def markPaymentReceived(
      id: Long,
      membershipNumber: String,
      received: Boolean,
      session: AuthenticatedSession
  ): IO[Either[AuthError | EventError, EventBooking]] =
    authorizeCoachOrCommittee(session) match
      case Left(error) => IO.pure(Left(error))
      case Right(_) =>
        updateBooking(id, membershipNumber) { booking =>
          booking.copy(paymentReceived = Some(received), updatedAt = System.currentTimeMillis())
        }

  private def updateBooking(
      id: Long,
      membershipNumber: String
  )(f: EventBooking => EventBooking): IO[Either[EventError, EventBooking]] =
    repository.getBooking(id, membershipNumber).flatMap {
      case None => IO.pure(Left(EventError.BookingNotFound(id, membershipNumber)))
      case Some(booking) => repository.upsertBooking(f(booking)).map(Right(_))
    }

  private def validateSessionRequest(request: CreateSessionEventRequest): IO[Either[EventError, CreateSessionEventRequest]] =
    IO.pure {
      if request.eventDate.trim.isEmpty then Left(EventError.Validation("eventDate must not be empty"))
      else if request.meetTime.trim.isEmpty then Left(EventError.Validation("meetTime must not be empty"))
      else if request.endTime.trim.isEmpty then Left(EventError.Validation("endTime must not be empty"))
      else if request.maxParticipants <= 0 then Left(EventError.Validation("maxParticipants must be greater than zero"))
      else Right(request)
    }

  private def validateTripRequest(request: CreateTripEventRequest): IO[Either[EventError, CreateTripEventRequest]] =
    IO.pure {
      if request.title.trim.isEmpty then Left(EventError.Validation("title must not be empty"))
      else if request.eventDate.trim.isEmpty then Left(EventError.Validation("eventDate must not be empty"))
      else if request.description.trim.isEmpty then Left(EventError.Validation("description must not be empty"))
      else if request.maxParticipants <= 0 then Left(EventError.Validation("maxParticipants must be greater than zero"))
      else Right(request)
    }

  private def buildSessionTitle(request: CreateSessionEventRequest): String =
    request.locationType match
      case LocationType.Canal     => "Canal Session"
      case LocationType.KewBridge => "Kew Bridge Session"
      case LocationType.Other     => request.locationText.filter(_.trim.nonEmpty).getOrElse("Club Session")

  private def today: String =
    LocalDate.now().toString

  private def authorizeLoggedIn(session: AuthenticatedSession): Either[AuthError, Unit] =
    Right(())

  private def authorizeCoachOrCommittee(session: AuthenticatedSession): Either[AuthError, Unit] =
    if isCoachOrCommittee(session) then Right(())
    else Left(AuthError.CoachOrCommitteeRequired)

  private def isCoachOrCommittee(session: AuthenticatedSession): Boolean =
    session.role == MembershipRole.Coach || session.role == MembershipRole.Committee
