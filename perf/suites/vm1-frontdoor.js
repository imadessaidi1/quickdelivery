import { config } from '../lib/config.js';
import { runAdminDashboards } from '../scenarios/admin-dashboards.js';
import { runGuestCheckout } from '../scenarios/guest-checkout.js';

function envNumber(key, fallback) {
  const value = Number(__ENV[key]);
  return Number.isFinite(value) && value > 0 ? value : fallback;
}

const guestStartRate = envNumber('VM1_GUEST_START_RATE', 5);
const guestPeakRate = envNumber('VM1_GUEST_PEAK_RATE', 120);
const adminVus = envNumber('VM1_ADMIN_VUS', 8);
const preAllocatedVus = envNumber('VM1_PREALLOCATED_VUS', 100);
const maxVus = envNumber('VM1_MAX_VUS', 450);

export const options = {
  scenarios: {
    guest_checkout_frontdoor: {
      executor: 'ramping-arrival-rate',
      exec: 'guestCheckout',
      startRate: guestStartRate,
      timeUnit: '1s',
      preAllocatedVUs: preAllocatedVus,
      maxVUs: maxVus,
      stages: [
        { duration: '2m', target: 20 },
        { duration: '2m', target: 40 },
        { duration: '2m', target: 60 },
        { duration: '2m', target: 80 },
        { duration: '2m', target: 100 },
        { duration: '2m', target: guestPeakRate },
        { duration: '1m', target: 0 },
      ],
    },
    admin_dashboards_background: {
      executor: 'constant-vus',
      exec: 'adminDashboards',
      startTime: '45s',
      vus: adminVus,
      duration: '12m',
    },
  },
  thresholds: {},
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  noConnectionReuse: String(__ENV.VM1_NO_CONNECTION_REUSE || 'true').toLowerCase() === 'true',
  insecureSkipTLSVerify: config.insecureSkipTLSVerify,
};

export function guestCheckout() {
  runGuestCheckout();
}

export function adminDashboards() {
  runAdminDashboards();
}
