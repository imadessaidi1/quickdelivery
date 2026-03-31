import http from 'k6/http';
import { group, sleep } from 'k6';
import { authHeaders, config } from '../lib/config.js';
import { courierHeaders, hasCourierAuth, resolveCourierToken } from '../lib/auth.js';
import { asJson, ensureResponse, hasEnv, sleepRange } from '../lib/utils.js';

export function findPackageInGroups(groups, packageId) {
  return Object.values(groups || {})
    .flatMap((items) => Array.isArray(items) ? items : [])
    .find((item) => Number(item?.id) === Number(packageId));
}

export function findReservation(packageDTO, courierId) {
  return (packageDTO?.packageReservations || []).find((reservation) => reservation && reservation.status === 'ONGOING')
    || (packageDTO?.packageReservations || [])[0]
    || null;
}

export function runCourierLifecycleForPackage({ bearerToken, courierId, packageId, locale = config.locale }) {
  const resolvedToken = resolveCourierToken(bearerToken);
  if (!hasEnv(resolvedToken, courierId, packageId)) {
    console.warn('Skipping courier lifecycle: courier token, id or package id is missing.');
    return null;
  }

  const headers = bearerToken ? authHeaders(bearerToken) : courierHeaders();
  const encodedPackageId = encodeURIComponent(packageId);
  const encodedCourierId = encodeURIComponent(courierId);
  const encodedLocale = encodeURIComponent(locale);
  let pickUpOtp = '';
  let deliveryOtp = '';

  group('courier-reserve', () => {
    const response = http.put(
      `${config.baseUrl}/packages/v1/reserve?packageID=${encodedPackageId}&deliveryPersonID=${encodedCourierId}&locale=${encodedLocale}`,
      null,
      { headers, tags: { flow: 'courier-reserve' } },
    );
    const body = asJson(response) || {};
    pickUpOtp = body?.pickUpOTP || '';
    ensureResponse(response, {
      'reserve returns 200': (res) => res.status === 200,
      'pickup otp returned by reserve': () => !!pickUpOtp,
    }, { flow: 'courier-reserve' });
  });

  if (!pickUpOtp) {
    return null;
  }

  sleep(sleepRange(0.5, 1.0));

  group('courier-pickup', () => {
    const response = http.put(
      `${config.baseUrl}/packages/v1/pickup?packageID=${encodedPackageId}&deliveryPersonID=${encodedCourierId}&pickUpOTP=${encodeURIComponent(pickUpOtp)}&locale=${encodedLocale}`,
      null,
      { headers, tags: { flow: 'courier-pickup' } },
    );
    const body = asJson(response) || {};
    deliveryOtp = body?.deliveryOTP || '';
    ensureResponse(response, {
      'pickup returns 200': (res) => res.status === 200,
      'delivery otp returned by pickup': () => !!deliveryOtp,
    }, { flow: 'courier-pickup' });
  });

  if (!deliveryOtp) {
    return null;
  }

  return {
    headers,
    courierId,
    packageId,
    locale,
    pickUpOtp,
    deliveryOtp,
  };
}

export function deliverPackageLifecycleStep(state) {
  if (!state?.deliveryOtp) {
    return;
  }

  const packageId = encodeURIComponent(state.packageId);
  const courierId = encodeURIComponent(state.courierId);
  const locale = encodeURIComponent(state.locale || config.locale);

  group('courier-deliver', () => {
    const response = http.put(
      `${config.baseUrl}/packages/v1/deliver?packageID=${packageId}&deliveryPersonID=${courierId}&deliveryOTP=${encodeURIComponent(state.deliveryOtp)}&locale=${locale}`,
      null,
      { headers: state.headers, tags: { flow: 'courier-deliver' } },
    );
    ensureResponse(response, {
      'deliver returns 200': (res) => res.status === 200,
    }, { flow: 'courier-deliver' });
  });
}

export function runCourierLifecycle() {
  if (!hasCourierAuth()) {
    console.warn('Skipping courier lifecycle: courier authentication is missing.');
    return;
  }
  const state = runCourierLifecycleForPackage({
    bearerToken: '',
    courierId: config.courierId,
    packageId: config.courierPackageId,
    locale: config.locale,
  });
  if (!state) {
    return;
  }

  sleep(sleepRange(0.5, 1.0));
  deliverPackageLifecycleStep(state);
}

export default function courierLifecycleDefault() {
  runCourierLifecycle();
}
