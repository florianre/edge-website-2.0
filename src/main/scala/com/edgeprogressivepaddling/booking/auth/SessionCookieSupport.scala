package com.edgeprogressivepaddling.booking.auth

import com.edgeprogressivepaddling.booking.domain.AuthenticatedSession
import io.circe.parser.decode
import io.circe.syntax.EncoderOps
import org.http4s.{Request, ResponseCookie}

import java.nio.charset.StandardCharsets
import java.util.Base64

object SessionCookieSupport:
  val SessionCookieName = "edge_session"

  def sessionFromRequest[F[_]](request: Request[F]): Option[AuthenticatedSession] =
    request.cookies
      .find(_.name == SessionCookieName)
      .flatMap(cookie => decodeCookie(cookie.content))

  def setSessionCookie(session: AuthenticatedSession): ResponseCookie =
    ResponseCookie(
      name = SessionCookieName,
      content = encodeCookie(session),
      path = Some("/"),
      httpOnly = true,
      sameSite = Some(org.http4s.SameSite.Lax)
    )

  def clearSessionCookie: ResponseCookie =
    ResponseCookie(
      name = SessionCookieName,
      content = "",
      path = Some("/"),
      maxAge = Some(0L)
    )

  private def encodeCookie(session: AuthenticatedSession): String =
    Base64.getUrlEncoder.encodeToString(session.asJson.noSpaces.getBytes(StandardCharsets.UTF_8))

  private def decodeCookie(raw: String): Option[AuthenticatedSession] =
    val decoded = String(Base64.getUrlDecoder.decode(raw), StandardCharsets.UTF_8)
    decode[AuthenticatedSession](decoded).toOption
