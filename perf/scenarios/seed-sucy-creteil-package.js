import http from 'k6/http';
import { config } from '../lib/config.js';
import { buildCuratedVisibleSeedPlan, getCuratedVisibleSeedCities } from '../lib/package-payload.js';
import { asJson, ensureResponse, sleepRange } from '../lib/utils.js';
import { sleep } from 'k6';

function createAndPayPackage(payload) {
  const createResponse = http.post(
    `${config.baseUrl}/packages/v1/create`,
    {
      packageDTO: JSON.stringify(payload),
      locale: config.locale,
    },
    {
      tags: { flow: 'visible-seed-create' },
    },
  );

  const body = asJson(createResponse);
  ensureResponse(createResponse, {
    'targeted seed create returns 200': (res) => res.status === 200,
    'targeted seed create returns package': () => Number(body?.id) > 0,
  }, { flow: 'visible-seed-create' });

  if (!body?.id || !body?.guestAccessToken) {
    return null;
  }

  sleep(sleepRange(0.15, 0.35));

  const paymentResponse = http.put(
    `${config.baseUrl}/packages/v1/confirm-guest-payment?packageID=${body.id}&guestAccessToken=${encodeURIComponent(body.guestAccessToken)}`,
    null,
    {
      tags: { flow: 'visible-seed-payment' },
      responseCallback: http.expectedStatuses(200, 409),
    },
  );

  ensureResponse(paymentResponse, {
    'targeted seed payment returns success or conflict': (res) => res.status === 200 || res.status === 409,
  }, { flow: 'visible-seed-payment' });

  return body;
}

export const options = {
  scenarios: {
    targeted_visible_seed_package: {
      executor: 'shared-iterations',
      vus: 1,
      iterations: 1,
      maxDuration: '1m',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.05'],
    checks: ['rate>0.95'],
  },
  insecureSkipTLSVerify: config.insecureSkipTLSVerify,
};

export default function seedSucyCreteilPackage() {
  const cityIndex = getCuratedVisibleSeedCities().findIndex((city) => city.name === 'Sucy-en-Brie');
  if (cityIndex < 0) {
    throw new Error('Sucy-en-Brie seed plan not found.');
  }

  const payload = buildCuratedVisibleSeedPlan(cityIndex).anchorPayload;
  const created = createAndPayPackage(payload);
  console.log(`Targeted Sucy-en-Brie -> Creteil package created: ${JSON.stringify(created)}`);
}
