import http from 'k6/http';
import { group, sleep } from 'k6';
import { authHeaders, config } from '../lib/config.js';
import { asJson, ensureResponse, hasEnv, sleepRange } from '../lib/utils.js';

function fetchCourierPackages(courierId, headers) {
  const response = http.get(
    `${config.baseUrl}/packages/v1/getPackagesByDeliveryPerson?deliveryPersonID=${encodeURIComponent(courierId)}`,
    {
      headers,
      tags: { flow: 'courier-packages' },
    },
  );

  return {
    response,
    body: asJson(response) || {},
  };
}

function findPackageInGroups(groups, packageId) {
  return Object.values(groups || {})
    .flatMap((items) => Array.isArray(items) ? items : [])
    .find((item) => Number(item?.id) === Number(packageId));
}

function findReservation(packageDTO, courierId) {
  return (packageDTO?.packageReservations || []).find((reservation) => reservation && reservation.status === 'ONGOING')
    || (packageDTO?.packageReservations || [])[0]
    || null;
}

export function runCourierLifecycle() {
  if (!hasEnv(
    config.courierBearerToken,
    config.courierId,
    config.courierPackageId,
  )) {
    console.warn('Skipping courier lifecycle: courier token, id or package id is missing.');
    return;
  }

  const headers = authHeaders(config.courierBearerToken);
  const packageId = encodeURIComponent(config.courierPackageId);
  const courierId = encodeURIComponent(config.courierId);
  const locale = encodeURIComponent(config.locale);
  let pickUpOtp = '';
  let deliveryOtp = '';

  group('courier-reserve', () => {
    const response = http.put(
      `${config.baseUrl}/packages/v1/reserve?packageID=${packageId}&deliveryPersonID=${courierId}&locale=${locale}`,
      null,
      { headers, tags: { flow: 'courier-reserve' } },
    );
    ensureResponse(response, {
      'reserve returns 200': (res) => res.status === 200,
    }, { flow: 'courier-reserve' });
  });

  sleep(sleepRange(0.5, 1.0));

  group('courier-read-pickup-otp', () => {
    const { response, body } = fetchCourierPackages(config.courierId, headers);
    const packageDTO = findPackageInGroups(body, config.courierPackageId);
    const reservation = findReservation(packageDTO, config.courierId);
    pickUpOtp = reservation?.pickUpOTP || '';
    ensureResponse(response, {
      'packages by delivery person returns 200': (res) => res.status === 200,
      'pickup otp resolved after reserve': () => !!pickUpOtp,
    }, { flow: 'courier-read-pickup-otp' });
  });

  if (!pickUpOtp) {
    return;
  }

  sleep(sleepRange(0.5, 1.0));

  group('courier-pickup', () => {
    const response = http.put(
      `${config.baseUrl}/packages/v1/pickup?packageID=${packageId}&deliveryPersonID=${courierId}&pickUpOTP=${encodeURIComponent(pickUpOtp)}&locale=${locale}`,
      null,
      { headers, tags: { flow: 'courier-pickup' } },
    );
    ensureResponse(response, {
      'pickup returns 200': (res) => res.status === 200,
    }, { flow: 'courier-pickup' });
  });

  sleep(sleepRange(0.5, 1.0));

  group('courier-read-delivery-otp', () => {
    const { response, body } = fetchCourierPackages(config.courierId, headers);
    const packageDTO = findPackageInGroups(body, config.courierPackageId);
    const reservation = findReservation(packageDTO, config.courierId);
    deliveryOtp = reservation?.deliveryOTP || '';
    ensureResponse(response, {
      'packages by delivery person returns 200 after pickup': (res) => res.status === 200,
      'delivery otp resolved after pickup': () => !!deliveryOtp,
    }, { flow: 'courier-read-delivery-otp' });
  });

  if (!deliveryOtp) {
    return;
  }

  sleep(sleepRange(0.5, 1.0));

  group('courier-deliver', () => {
    const response = http.put(
      `${config.baseUrl}/packages/v1/deliver?packageID=${packageId}&deliveryPersonID=${courierId}&deliveryOTP=${encodeURIComponent(deliveryOtp)}&locale=${locale}`,
      null,
      { headers, tags: { flow: 'courier-deliver' } },
    );
    ensureResponse(response, {
      'deliver returns 200': (res) => res.status === 200,
    }, { flow: 'courier-deliver' });
  });
}

export default function courierLifecycleDefault() {
  runCourierLifecycle();
}
