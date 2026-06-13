package com.edgeprogressivepaddling.booking.repository

import cats.effect.IO
import com.edgeprogressivepaddling.booking.domain.*
import slick.jdbc.H2Profile.api.*

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

final class SlickEventRepository(db: Database)(using ExecutionContext) extends EventRepository[IO]:
  import SlickEventRepository.*

  override def initialise: IO[Unit] =
    runIO((events.schema ++ bookings.schema).createIfNotExists)

  override def listUpcoming(today: String): IO[List[Event]] =
    runIO(
      events
        .filter(row => row.eventDate >= today)
        .sortBy(row => (row.eventDate.asc, row.meetTime.asc))
        .result
        .map(_.toList)
    )

  override def listPast(today: String): IO[List[Event]] =
    runIO(
      events
        .filter(_.eventDate < today)
        .sortBy(row => (row.eventDate.desc, row.meetTime.desc))
        .result
        .map(_.toList)
    )

  override def getById(id: Long): IO[Option[Event]] =
    runIO(events.filter(_.id === id).result.headOption)

  override def create(event: Event): IO[Event] =
    runIO((events returning events.map(_.id) into ((row, id) => row.copy(id = id))) += event)

  override def update(event: Event): IO[Event] =
    runIO(events.filter(_.id === event.id).update(event).map(_ => event))

  override def listBookings(eventId: Long): IO[List[EventBooking]] =
    runIO(
      bookings
        .filter(_.eventId === eventId)
        .sortBy(_.createdAt.asc)
        .result
        .map(_.toList)
    )

  override def upsertBooking(booking: EventBooking): IO[EventBooking] =
    if booking.id == 0 then
      runIO((bookings returning bookings.map(_.id) into ((row, id) => row.copy(id = id))) += booking)
    else
      runIO(bookings.filter(_.id === booking.id).update(booking).map(_ => booking))

  override def getBooking(eventId: Long, membershipNumber: String): IO[Option[EventBooking]] =
    runIO(
      bookings
        .filter(row => row.eventId === eventId && row.membershipNumber === membershipNumber)
        .result
        .headOption
    )

  private def runIO[A](action: DBIO[A]): IO[A] =
    IO.fromFuture(IO(db.run(action)))

object SlickEventRepository:
  private def decodeEventType(value: String): EventType =
    EventType.fromString(value).fold(message => throw IllegalArgumentException(message), identity)

  private def decodeLocationType(value: String): LocationType =
    LocationType.fromString(value).fold(message => throw IllegalArgumentException(message), identity)

  private def decodeEventStatus(value: String): EventStatus =
    EventStatus.fromString(value).fold(message => throw IllegalArgumentException(message), identity)

  private def decodeBookingStatus(value: String): BookingStatus =
    BookingStatus.fromString(value).fold(message => throw IllegalArgumentException(message), identity)

  private final class EventsTable(tag: Tag) extends Table[Event](tag, "events"):
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def eventType = column[String]("event_type")
    def title = column[String]("title")
    def eventDate = column[String]("event_date")
    def endDate = column[Option[String]]("end_date")
    def meetTime = column[Option[String]]("meet_time")
    def endTime = column[Option[String]]("end_time")
    def locationType = column[Option[String]]("location_type")
    def locationText = column[Option[String]]("location_text")
    def destination = column[Option[String]]("destination")
    def tideInfo = column[Option[String]]("tide_info")
    def maxParticipants = column[Int]("max_participants")
    def description = column[Option[String]]("description")
    def extraInfo = column[Option[String]]("extra_info")
    def status = column[String]("status")
    def createdByMembershipNumber = column[String]("created_by_membership_number")
    def createdAt = column[Long]("created_at")
    def updatedAt = column[Long]("updated_at")

    def * =
      (
        id,
        eventType,
        title,
        eventDate,
        endDate,
        meetTime,
        endTime,
        locationType,
        locationText,
        destination,
        tideInfo,
        maxParticipants,
        description,
        extraInfo,
        status,
        createdByMembershipNumber,
        createdAt,
        updatedAt
      ).<>(
        {
          case (
                id,
                eventType,
                title,
                eventDate,
                endDate,
                meetTime,
                endTime,
                locationType,
                locationText,
                destination,
                tideInfo,
                maxParticipants,
                description,
                extraInfo,
                status,
                createdByMembershipNumber,
                createdAt,
                updatedAt
              ) =>
            Event(
              id = id,
              eventType = decodeEventType(eventType),
              title = title,
              eventDate = eventDate,
              endDate = endDate,
              meetTime = meetTime,
              endTime = endTime,
              locationType = locationType.map(decodeLocationType),
              locationText = locationText,
              destination = destination,
              tideInfo = tideInfo,
              maxParticipants = maxParticipants,
              description = description,
              extraInfo = extraInfo,
              status = decodeEventStatus(status),
              createdByMembershipNumber = createdByMembershipNumber,
              createdAt = createdAt,
              updatedAt = updatedAt
            )
        },
        event =>
          Some(
            (
              event.id,
              event.eventType match
                case EventType.Session => "session"
                case EventType.Trip    => "trip",
              event.title,
              event.eventDate,
              event.endDate,
              event.meetTime,
              event.endTime,
              event.locationType.map {
                case LocationType.Canal     => "canal"
                case LocationType.KewBridge => "kew_bridge"
                case LocationType.Other     => "other"
              },
              event.locationText,
              event.destination,
              event.tideInfo,
              event.maxParticipants,
              event.description,
              event.extraInfo,
              event.status match
                case EventStatus.Active    => "active"
                case EventStatus.Cancelled => "cancelled",
              event.createdByMembershipNumber,
              event.createdAt,
              event.updatedAt
            )
          )
        
      )

  private final class BookingsTable(tag: Tag) extends Table[EventBooking](tag, "bookings"):
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def eventId = column[Long]("event_id")
    def membershipNumber = column[String]("membership_number")
    def memberName = column[String]("member_name")
    def bookingStatus = column[String]("booking_status")
    def note = column[Option[String]]("note")
    def paymentSent = column[Option[Boolean]]("payment_sent")
    def paymentReceived = column[Option[Boolean]]("payment_received")
    def createdAt = column[Long]("created_at")
    def updatedAt = column[Long]("updated_at")

    def * =
      (
        id,
        eventId,
        membershipNumber,
        memberName,
        bookingStatus,
        note,
        paymentSent,
        paymentReceived,
        createdAt,
        updatedAt
      ).<>(
        {
          case (id, eventId, membershipNumber, memberName, bookingStatus, note, paymentSent, paymentReceived, createdAt, updatedAt) =>
            EventBooking(
              id = id,
              eventId = eventId,
              membershipNumber = membershipNumber,
              memberName = memberName,
              bookingStatus = decodeBookingStatus(bookingStatus),
              note = note,
              paymentSent = paymentSent,
              paymentReceived = paymentReceived,
              createdAt = createdAt,
              updatedAt = updatedAt
            )
        },
        booking =>
          Some(
            (
              booking.id,
              booking.eventId,
              booking.membershipNumber,
              booking.memberName,
              booking.bookingStatus match
                case BookingStatus.Going     => "going"
                case BookingStatus.Cancelled => "cancelled",
              booking.note,
              booking.paymentSent,
              booking.paymentReceived,
              booking.createdAt,
              booking.updatedAt
            )
          )
        
      )

  private val events = TableQuery[EventsTable]
  private val bookings = TableQuery[BookingsTable]
