import { config } from '../lib/config.js';
import { runAdminDashboards } from '../scenarios/admin-dashboards.js';
import { runCustomerOnboarding } from '../scenarios/customer-onboarding.js';
import { runGuestCheckout } from '../scenarios/guest-checkout.js';

function envNumber(key, fallback) {
  const value = Number(__ENV[key]);
  return Number.isFinite(value) && value > 0 ? value : fallback;
}

const guestStartRate = envNumber('DB_GUEST_START_RATE', 3);
const guestPeakRate = envNumber('DB_GUEST_PEAK_RATE', 50);
const customerPeakVus = envNumber('DB_CUSTOMER_PEAK_VUS', 26);
const adminVus = envNumber('DB_ADMIN_VUS', 12);
const preAllocatedVus = envNumber('DB_PREALLOCATED_VUS', 70);
const maxVus = envNumber('DB_MAX_VUS', 260);

export const options = {
  scenarios: {
    guest_checkout_db_mix: {
      executor: 'ramping-arrival-rate',
      exec: 'guestCheckout',
      startRate: guestStartRate,
      timeUnit: '1s',
      preAllocatedVUs: preAllocatedVus,
      maxVUs: maxVus,
      stages: [
        { duration: '2m', target: 8 },
        { duration: '2m', target: 16 },
        { duration: '2m', target: 24 },
        { duration: '2m', target: 32 },
        { duration: '2m', target: 40 },
        { duration: '2m', target: guestPeakRate },
        { duration: '1m', target: 0 },
      ],
    },
    customer_onboarding_db_mix: {
      executor: 'ramping-vus',
      exec: 'customerOnboarding',
      startTime: '20s',
      stages: [
        { duration: '2m', target: 4 },
        { duration: '2m', target: 8 },
        { duration: '2m', target: 12 },
        { duration: '2m', target: 18 },
        { duration: '2m', target: customerPeakVus },
        { duration: '1m', target: 0 },
      ],
      gracefulRampDown: '20s',
    },
    admin_dashboard_db_reads: {
      executor: 'constant-vus',
      exec: 'adminDashboards',
      startTime: '30s',
      vus: adminVus,
      duration: '12m',
    },
  },
  thresholds: {},
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  insecureSkipTLSVerify: config.insecureSkipTLSVerify,
};

export function guestCheckout() {
  runGuestCheckout();
}

export function customerOnboarding() {
  runCustomerOnboarding();
}

export function adminDashboards() {
  runAdminDashboards();
}
