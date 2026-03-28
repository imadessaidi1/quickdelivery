import http from 'k6/http';
import { group, sleep } from 'k6';
import { config } from '../lib/config.js';
import { buildCreateFormData, buildEstimatePayload } from '../lib/package-payload.js';
import { asJson, ensureResponse, sleepRange } from '../lib/utils.js';

export function runGuestCheckout() {
  const estimatePayload = buildEstimatePayload();
  let packageId = null;
  let packageReference = null;
  let guestAccessToken = null;

  group('guest-checkout-estimate', () => {
    const response = http.post(
      `${config.baseUrl}/packages/v1/estimate-price`,
      JSON.stringify(estimatePayload),
      {
        headers: { 'Content-Type': 'application/json' },
        tags: { flow: 'guest-estimate' },
      },
    );
    ensureResponse(response, {
      'estimate returns 200': (res) => res.status === 200,
      'estimate returns delivery price': (res) => Number(asJson(res)?.deliveryPrice) > 0,
    }, { flow: 'guest-estimate' });
  });

  sleep(sleepRange(0.5, 1.2));

  group('guest-checkout-create', () => {
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

    ensureResponse(response, {
      'create returns 200': (res) => res.status === 200,
      'create returns package id': () => Number(packageId) > 0,
      'create returns guest token': () => !!guestAccessToken,
      'create returns reference': () => !!packageReference,
    }, { flow: 'guest-create' });
  });

  if (!packageId || !guestAccessToken || !packageReference) {
    return;
  }

  sleep(sleepRange(0.5, 1.0));

  group('guest-checkout-payment', () => {
    const response = http.put(
      `${config.baseUrl}/packages/v1/confirm-guest-payment?packageID=${packageId}&guestAccessToken=${encodeURIComponent(guestAccessToken)}`,
      null,
      {
        tags: { flow: 'guest-payment' },
        responseCallback: http.expectedStatuses(200, 409),
      },
    );
    ensureResponse(response, {
      'guest payment returns success or conflict': (res) => res.status === 200 || res.status === 409,
    }, { flow: 'guest-payment' });
  });

  sleep(sleepRange(0.4, 0.8));

  group('guest-checkout-consult', () => {
    const response = http.get(
      `${config.baseUrl}/packages/v1/getGuestPackage?reference=${encodeURIComponent(packageReference)}&guestAccessToken=${encodeURIComponent(guestAccessToken)}`,
      {
        tags: { flow: 'guest-consult' },
      },
    );
    const body = asJson(response);
    ensureResponse(response, {
      'guest consult returns 200': (res) => res.status === 200,
      'guest consult returns package': () => body?.reference === packageReference,
    }, { flow: 'guest-consult' });
  });
}

export default function guestCheckoutDefault() {
  runGuestCheckout();
}
