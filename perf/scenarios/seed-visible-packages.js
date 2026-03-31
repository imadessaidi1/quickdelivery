import http from 'k6/http';
import { group, sleep } from 'k6';
import { config } from '../lib/config.js';
import { buildVisibleSeedPayload } from '../lib/package-payload.js';
import { asJson, ensureResponse, sleepRange } from '../lib/utils.js';

const TARGET_RADII = [10, 20, 30];
const PACKAGES_PER_RADIUS = Number(__ENV.VISIBLE_PACKAGES_PER_RADIUS || 3);

function createSeedPackage(radiusKm, index) {
  const payload = buildVisibleSeedPayload(radiusKm, index);
  let createResponse = null;
  let created = null;

  for (let attempt = 0; attempt < 2; attempt += 1) {
    createResponse = http.post(
      `${config.baseUrl}/packages/v1/create`,
      {
        packageDTO: JSON.stringify(payload),
        locale: config.locale,
      },
      {
        tags: { flow: 'visible-seed-create' },
      },
    );
    created = asJson(createResponse);
    if (createResponse.status === 200 && Number(created?.id) > 0) {
      break;
    }
    sleep(sleepRange(0.4, 0.8));
  }

  ensureResponse(createResponse, {
    'visible seed create returns 200': (res) => res.status === 200,
    'visible seed create returns package': () => Number(created?.id) > 0,
  }, { flow: 'visible-seed-create' });

  if (!created?.id || !created?.guestAccessToken) {
    return null;
  }

  sleep(sleepRange(0.2, 0.4));

  const paymentResponse = http.put(
    `${config.baseUrl}/packages/v1/confirm-guest-payment?packageID=${created.id}&guestAccessToken=${encodeURIComponent(created.guestAccessToken)}`,
    null,
    {
      tags: { flow: 'visible-seed-payment' },
      responseCallback: http.expectedStatuses(200, 409),
    },
  );

  ensureResponse(paymentResponse, {
    'visible seed payment returns success or conflict': (res) => res.status === 200 || res.status === 409,
  }, { flow: 'visible-seed-payment' });

  return {
    id: created.id,
    reference: created.reference,
    radiusKm,
  };
}

export function runVisibleSeedPackages() {
  const created = [];

  TARGET_RADII.forEach((radiusKm) => {
    group(`visible-seed-${radiusKm}km`, () => {
      for (let index = 0; index < PACKAGES_PER_RADIUS; index += 1) {
        const seeded = createSeedPackage(radiusKm, index);
        if (seeded) {
          created.push(seeded);
        }
      }
    });
  });

  console.log(`Visible seed packages created: ${JSON.stringify(created)}`);
}

export const options = {
  scenarios: {
    visible_seed_packages: {
      executor: 'shared-iterations',
      vus: 1,
      iterations: 1,
      maxDuration: '5m',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.05'],
    checks: ['rate>0.95'],
  },
  insecureSkipTLSVerify: config.insecureSkipTLSVerify,
};

export default function visibleSeedPackagesDefault() {
  runVisibleSeedPackages();
}
