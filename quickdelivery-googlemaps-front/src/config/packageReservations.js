function sameIdentifier(left, right) {
  if (left === null || left === undefined || right === null || right === undefined) {
    return false;
  }
  return String(left) === String(right);
}

function getReservationDeliveryPersonId(reservation) {
  return reservation?.deliveryPersonId
    ?? reservation?.deliveryPersonID
    ?? reservation?.deliveryPerson?.id
    ?? null;
}

function isActiveReservation(reservation) {
  return ['ONGOING', 'RESERVED'].includes(String(reservation?.status || '').toUpperCase());
}

export function isPackageReservedByDeliveryPerson(package_, deliveryPersonId) {
  if (!package_ || package_.status !== 'RESERVED' || !deliveryPersonId) {
    return false;
  }

  const reservations = Array.isArray(package_.packageReservations)
    ? package_.packageReservations
    : [];

  return reservations.some((reservation) => (
    isActiveReservation(reservation)
    && sameIdentifier(getReservationDeliveryPersonId(reservation), deliveryPersonId)
  ));
}
