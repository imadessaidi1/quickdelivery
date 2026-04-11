import { config } from '../lib/config.js';
import { runEndToEndDelivery } from '../scenarios/e2e-delivery.js';
import { runGuestCheckout } from '../scenarios/guest-checkout.js';

function envNumber(key, fallback) {
  const value = Number(__ENV[key]);
  return Number.isFinite(value) && value > 0 ? value : fallback;
}

const guestStartRate = envNumber('PACKAGES_GUEST_START_RATE', 4);
const guestPeakRate = envNumber('PACKAGES_GUEST_PEAK_RATE', 70);
const deliveryPeakVus = envNumber('PACKAGES_DELIVERY_PEAK_VUS', 28);
const preAllocatedVus = envNumber('PACKAGES_PREALLOCATED_VUS', 80);
const maxVus = envNumber('PACKAGES_MAX_VUS', 320);

export const options = {
  scenarios: {
    package_create_hot_path: {
      executor: 'ramping-arrival-rate',
      exec: 'guestCheckout',
      startRate: guestStartRate,
      timeUnit: '1s',
      preAllocatedVUs: preAllocatedVus,
      maxVUs: maxVus,
      stages: [
        { duration: '2m', target: 10 },
        { duration: '2m', target: 20 },
        { duration: '2m', target: 30 },
        { duration: '2m', target: 40 },
        { duration: '2m', target: 55 },
        { duration: '2m', target: guestPeakRate },
        { duration: '1m', target: 0 },
      ],
    },
    package_lifecycle_hot_path: {
      executor: 'ramping-vus',
      exec: 'e2eDelivery',
      startTime: '30s',
      stages: [
        { duration: '2m', target: 6 },
        { duration: '2m', target: 12 },
        { duration: '2m', target: 18 },
        { duration: '2m', target: 22 },
        { duration: '2m', target: deliveryPeakVus },
        { duration: '1m', target: 0 },
      ],
      gracefulRampDown: '20s',
    },
  },
  thresholds: {},
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  insecureSkipTLSVerify: config.insecureSkipTLSVerify,
};

export function guestCheckout() {
  runGuestCheckout();
}

export function e2eDelivery() {
  runEndToEndDelivery();
}
