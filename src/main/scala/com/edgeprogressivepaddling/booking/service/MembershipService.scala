package com.edgeprogressivepaddling.booking.service

import com.edgeprogressivepaddling.booking.domain.{CreateMembershipRequest, Membership, UpdateMembershipRequest}

trait MembershipService[F[_]]:
  def search(criteria: MembershipSearchCriteria): F[List[Membership]]
  def getByMembershipNumber(membershipNumber: String): F[Option[Membership]]
  def create(request: CreateMembershipRequest): F[Either[MembershipError, Membership]]
  def update(membershipNumber: String, request: UpdateMembershipRequest): F[Either[MembershipError, Membership]]
  def delete(membershipNumber: String): F[Either[MembershipError, Unit]]
  def activate(membershipNumber: String): F[Either[MembershipError, Membership]]
  def deactivate(membershipNumber: String): F[Either[MembershipError, Membership]]
