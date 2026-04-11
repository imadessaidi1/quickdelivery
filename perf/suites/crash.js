import { config } from '../lib/config.js';
import { runAdminDashboards } from '../scenarios/admin-dashboards.js';
import { runCustomerOnboarding } from '../scenarios/customer-onboarding.js';
import { runEndToEndDelivery } from '../scenarios/e2e-delivery.js';
import { runGuestCheckout } from '../scenarios/guest-checkout.js';
import { runTrackingLive } from '../scenarios/tracking-live.js';

function envNumber(key, fallback) {
  const value = Number(__ENV[key]);
  return Number.isFinite(value) && value > 0 ? value : fallback;
}

const guestStartRate = envNumber('CRASH_GUEST_START_RATE', 4);
const guestPeakRate = envNumber('CRASH_GUEST_PEAK_RATE', 55);
const customerPeakVus = envNumber('CRASH_CUSTOMER_PEAK_VUS', 20);
const deliveryPeakVus = envNumber('CRASH_DELIVERY_PEAK_VUS', 16);
const adminVus = envNumber('CRASH_ADMIN_VUS', 10);
const trackingPeakVus = envNumber('CRASH_TRACKING_PEAK_VUS', 24);
const maxVus = envNumber('CRASH_MAX_VUS', 320);
const preAllocatedVus = envNumber('CRASH_PREALLOCATED_VUS', 80);

export const options = {
  scenarios: {
    guest_checkout_crash: {
      executor: 'ramping-arrival-rate',
      exec: 'guestCheckout',
      startRate: guestStartRate,
      timeUnit: '1s',
      preAllocatedVUs: preAllocatedVus,
      maxVUs: maxVus,
      stages: [
        { duration: '2m', target: guestStartRate * 2 },
        { duration: '3m', target: Math.round(guestPeakRate * 0.45) },
        { duration: '3m', target: Math.round(guestPeakRate * 0.7) },
        { duration: '3m', target: guestPeakRate },
        { duration: '2m', target: Math.round(guestPeakRate * 1.2) },
        { duration: '1m', target: 0 },
      ],
    },
    customer_onboarding_crash: {
      executor: 'ramping-vus',
      exec: 'customerOnboarding',
      startTime: '20s',
      stages: [
        { duration: '2m', target: Math.max(4, Math.round(customerPeakVus * 0.35)) },
        { duration: '3m', target: Math.max(8, Math.round(customerPeakVus * 0.65)) },
        { duration: '3m', target: customerPeakVus },
        { duration: '2m', target: Math.round(customerPeakVus * 1.25) },
        { duration: '1m', target: 0 },
      ],
      gracefulRampDown: '20s',
    },
    e2e_delivery_crash: {
      executor: 'ramping-vus',
      exec: 'e2eDelivery',
      startTime: '35s',
      stages: [
        { duration: '2m', target: Math.max(3, Math.round(deliveryPeakVus * 0.35)) },
        { duration: '3m', target: Math.max(6, Math.round(deliveryPeakVus * 0.65)) },
        { duration: '3m', target: deliveryPeakVus },
        { duration: '2m', target: Math.round(deliveryPeakVus * 1.25) },
        { duration: '1m', target: 0 },
      ],
      gracefulRampDown: '20s',
    },
    admin_dashboards_crash: {
      executor: 'constant-vus',
      exec: 'adminDashboards',
      startTime: '40s',
      vus: adminVus,
      duration: '13m',
    },
    tracking_live_crash: {
      executor: 'ramping-vus',
      exec: 'trackingLive',
      startTime: '55s',
      stages: [
        { duration: '2m', target: Math.max(4, Math.round(trackingPeakVus * 0.35)) },
        { duration: '3m', target: Math.max(8, Math.round(trackingPeakVus * 0.65)) },
        { duration: '3m', target: trackingPeakVus },
        { duration: '2m', target: Math.round(trackingPeakVus * 1.25) },
        { duration: '1m', target: 0 },
      ],
      gracefulRampDown: '20s',
    },
  },
  thresholds: {},
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  noConnectionReuse: String(__ENV.CRASH_NO_CONNECTION_REUSE || 'false').toLowerCase() === 'true',
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

export function trackingLive() {
  runTrackingLive();
}

