import http from 'k6/http';
import { group, sleep } from 'k6';
import { authHeaders, config } from '../lib/config.js';
import { hasCourierAuth } from '../lib/auth.js';
import { buildCreateFormData, buildEstimatePayload } from '../lib/package-payload.js';
import { asJson, ensureResponse, hasEnv, sleepRange } from '../lib/utils.js';
import { runCourierLifecycleForPackage, deliverPackageLifecycleStep } from './courier-lifecycle.js';
import { runTrackingLiveForPackage } from './tracking-live.js';

function fetchGuestPackage(packageReference, guestAccessToken) {
  return http.get(
    `${config.baseUrl}/packages/v1/getGuestPackage?reference=${encodeURIComponent(packageReference)}&guestAccessToken=${encodeURIComponent(guestAccessToken)}`,
    {
      tags: { flow: 'guest-consult' },
      responseCallback: http.expectedStatuses(200, 404),
    },
  );
}

function waitForGuestPackageStatus(packageReference, guestAccessToken, expectedStatus, attempts = 6) {
  let latestBody = null;

  for (let attempt = 0; attempt < attempts; attempt += 1) {
    const response = fetchGuestPackage(packageReference, guestAccessToken);
    latestBody = asJson(response);
    if (response.status === 200 && `${latestBody?.status || ''}` === expectedStatus) {
      return latestBody;
    }
    sleep(sleepRange(0.4, 0.8));
  }

  return latestBody;
}

export function runEndToEndDelivery() {
  if (!hasCourierAuth() || !hasEnv(config.courierId)) {
    console.warn('Skipping e2e delivery: courier token or courier id is missing.');
    return;
  }

  const estimatePayload = buildEstimatePayload();
  let packageId = null;
  let packageReference = null;
  let guestAccessToken = null;
  let packageStatus = '';

  group('e2e-estimate', () => {
    const response = http.post(
      `${config.baseUrl}/packages/v1/estimate-price`,
      JSON.stringify(estimatePayload),
      {
        headers: { 'Content-Type': 'application/json' },
        tags: { flow: 'guest-estimate' },
      },
    );
    ensureResponse(response, {
      'e2e estimate returns 200': (res) => res.status === 200,
      'e2e estimate returns delivery price': (res) => Number(asJson(res)?.deliveryPrice) > 0,
    }, { flow: 'guest-estimate' });
  });

  sleep(sleepRange(0.4, 0.9));

  group('e2e-create', () => {
    const response = http.post(
      `${config.baseUrl}/packages/v1/create`,
      buildCreateFormData(config.locale),
      {
        tags: { flow: 'guest-create' },
      },
    );

    const body = asJson(response);
    packageId = body?.id || null;
    packageReference = body?.reference || null;
    guestAccessToken = body?.guestAccessToken || null;
    packageStatus = `${body?.status || ''}`;

    ensureResponse(response, {
      'e2e create returns 200': (res) => res.status === 200,
      'e2e create returns package id': () => Number(packageId) > 0,
      'e2e create returns guest token': () => !!guestAccessToken,
      'e2e create returns reference': () => !!packageReference,
      'e2e create returns payment pending status': () => packageStatus === 'PAYMENTPENDING',
    }, { flow: 'guest-create' });
  });

  if (!packageId || !guestAccessToken || !packageReference) {
    return;
  }

  sleep(sleepRange(0.4, 0.8));

  if (packageStatus === 'PAYMENTPENDING') {
    group('e2e-payment', () => {
      const response = http.put(
        `${config.baseUrl}/packages/v1/confirm-guest-payment?packageID=${packageId}&guestAccessToken=${encodeURIComponent(guestAccessToken)}`,
        null,
        {
          tags: { flow: 'guest-payment' },
          responseCallback: http.expectedStatuses(200, 409),
        },
      );
      ensureResponse(response, {
        'e2e payment returns success or conflict': (res) => res.status === 200 || res.status === 409,
      }, { flow: 'guest-payment' });
    });
  }

  sleep(sleepRange(0.6, 1.0));

  group('e2e-wait-new', () => {
    const guestPackage = waitForGuestPackageStatus(packageReference, guestAccessToken, 'NEW');
    packageStatus = `${guestPackage?.status || ''}`;
    ensureResponse({ status: packageStatus === 'NEW' ? 200 : 409 }, {
      'e2e package becomes NEW after payment': () => packageStatus === 'NEW',
    }, { flow: 'guest-consult' });
  });

  if (packageStatus !== 'NEW') {
    return;
  }

  const lifecycleState = runCourierLifecycleForPackage({
    bearerToken: '',
    courierId: config.courierId,
    packageId,
    locale: config.locale,
  });

  if (!lifecycleState) {
    return;
  }

  sleep(sleepRange(0.4, 0.8));

  runTrackingLiveForPackage({
    bearerToken: '',
    deliveryPersonId: config.courierId,
    packageReference,
    guestAccessToken,
  });

  sleep(sleepRange(0.4, 0.8));

  deliverPackageLifecycleStep(lifecycleState);

  sleep(sleepRange(0.4, 0.8));

  group('e2e-consult', () => {
    const response = fetchGuestPackage(packageReference, guestAccessToken);
    const body = asJson(response);
    ensureResponse(response, {
      'e2e consult returns 200': (res) => res.status === 200,
      'e2e consult returns package': () => body?.reference === packageReference,
      'e2e consult returns delivered status': () => `${body?.status || ''}` === 'DELIVERED',
    }, { flow: 'guest-consult' });
  });
}

export default function e2eDeliveryDefault() {
  runEndToEndDelivery();
}
