import { config } from '../lib/config.js';
import { buildNominalThresholds } from '../lib/thresholds.js';
import { runAdminDashboards } from '../scenarios/admin-dashboards.js';
import { runCustomerOnboarding } from '../scenarios/customer-onboarding.js';
import { runGuestCheckout } from '../scenarios/guest-checkout.js';
import { runEndToEndDelivery } from '../scenarios/e2e-delivery.js';

function envNumber(key, fallback) {
  const value = Number(__ENV[key]);
  return Number.isFinite(value) && value > 0 ? value : fallback;
}

const guestStartRate = envNumber('PUBLIC_REALISTIC_GUEST_START_RATE', 2);
const guestPeakRate = envNumber('PUBLIC_REALISTIC_GUEST_PEAK_RATE', 14);
const guestPreAllocatedVus = envNumber('PUBLIC_REALISTIC_GUEST_PREALLOCATED_VUS', 40);
const guestMaxVus = envNumber('PUBLIC_REALISTIC_GUEST_MAX_VUS', 120);
const customerPeakVus = envNumber('PUBLIC_REALISTIC_CUSTOMER_PEAK_VUS', 2);
const e2ePeakVus = envNumber('PUBLIC_REALISTIC_E2E_PEAK_VUS', 2);
const adminVus = envNumber('PUBLIC_REALISTIC_ADMIN_VUS', 2);

export const options = {
  scenarios: {
    guest_checkout_public: {
      executor: 'ramping-arrival-rate',
      exec: 'guestCheckout',
      startRate: guestStartRate,
      timeUnit: '1s',
      preAllocatedVUs: guestPreAllocatedVus,
      maxVUs: guestMaxVus,
      stages: [
        { duration: '2m', target: 4 },
        { duration: '4m', target: 8 },
        { duration: '4m', target: 12 },
        { duration: '4m', target: guestPeakRate },
        { duration: '2m', target: 0 },
      ],
    },
    customer_onboarding_public: {
      executor: 'ramping-vus',
      exec: 'customerOnboarding',
      startTime: '20s',
      stages: [
        { duration: '2m', target: 1 },
        { duration: '8m', target: customerPeakVus },
        { duration: '2m', target: 0 },
      ],
      gracefulRampDown: '30s',
    },
    e2e_delivery_public: {
      executor: 'ramping-vus',
      exec: 'e2eDelivery',
      startTime: '40s',
      stages: [
        { duration: '2m', target: 1 },
        { duration: '8m', target: e2ePeakVus },
        { duration: '2m', target: 0 },
      ],
      gracefulRampDown: '30s',
    },
    admin_dashboards_public: {
      executor: 'constant-vus',
      exec: 'adminDashboards',
      startTime: '45s',
      vus: adminVus,
      duration: '14m',
    },
  },
  thresholds: buildNominalThresholds(),
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  noConnectionReuse: String(__ENV.PUBLIC_REALISTIC_NO_CONNECTION_REUSE || 'false').toLowerCase() === 'true',
  insecureSkipTLSVerify: config.insecureSkipTLSVerify,
};

export function guestCheckout() {
  runGuestCheckout();
}

export function customerOnboarding() {
  runCustomerOnboarding();
}

export function e2eDelivery() {
  runEndToEndDelivery();
}

export function adminDashboards() {
  runAdminDashboards();
}
