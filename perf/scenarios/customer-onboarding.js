import http from 'k6/http';
import { group, sleep } from 'k6';
import { config } from '../lib/config.js';
import { adminHeaders, hasAdminAuth } from '../lib/auth.js';
import { buildCustomerUserPayload } from '../lib/user-payload.js';
import { asJson, ensureResponse, sleepRange } from '../lib/utils.js';

export function runCustomerOnboarding() {
  const userPayload = buildCustomerUserPayload();

  let createdUserId = null;

  group('customer-onboarding-create', () => {
    const response = http.post(
      `${config.baseUrl}/users/v1/create-account`,
      JSON.stringify({
        user: userPayload,
        locale: config.locale,
      }),
      {
        headers: {
          'Content-Type': 'application/json',
        },
        tags: { flow: 'customer-create' },
      },
    );

    const body = asJson(response);
    createdUserId = body?.id || null;

    ensureResponse(response, {
      'customer create returns 200': (res) => res.status === 200,
      'customer create returns id': () => Number(createdUserId) > 0,
      'customer create echoes email': () => body?.emailAddress === userPayload.emailAddress,
    }, { flow: 'customer-create' });
  });

  if (!createdUserId || !hasAdminAuth()) {
    return;
  }

  sleep(sleepRange(0.3, 0.8));

  group('customer-onboarding-verify', () => {
    const response = http.get(
        `${config.baseUrl}/users/v1/userByEmail?email=${encodeURIComponent(userPayload.emailAddress)}`,
      {
        headers: adminHeaders(),
        tags: { flow: 'customer-verify' },
      },
    );
    const body = asJson(response);
    ensureResponse(response, {
      'customer verify returns 200': (res) => res.status === 200,
      'customer verify returns same id': () => Number(body?.id) === Number(createdUserId),
    }, { flow: 'customer-verify' });
  });
}

export default function customerOnboardingDefault() {
  runCustomerOnboarding();
}
