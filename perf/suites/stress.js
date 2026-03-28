import { config } from '../lib/config.js';
import { buildStressThresholds } from '../lib/thresholds.js';
import { runAdminDashboards } from '../scenarios/admin-dashboards.js';
import { runCourierLifecycle } from '../scenarios/courier-lifecycle.js';
import { runGuestCheckout } from '../scenarios/guest-checkout.js';
import { runTrackingLive } from '../scenarios/tracking-live.js';

export const options = {
  scenarios: {
    guest_checkout_stress: {
      executor: 'ramping-vus',
      exec: 'guestCheckout',
      stages: [
        { duration: '3m', target: 10 },
        { duration: '5m', target: 20 },
        { duration: '5m', target: 40 },
        { duration: '5m', target: 0 },
      ],
      gracefulRampDown: '30s',
    },
    tracking_live_stress: {
      executor: 'ramping-vus',
      exec: 'trackingLive',
      startTime: '30s',
      stages: [
        { duration: '3m', target: 5 },
        { duration: '5m', target: 10 },
        { duration: '5m', target: 20 },
        { duration: '5m', target: 0 },
      ],
      gracefulRampDown: '30s',
    },
    admin_dashboards_stress: {
      executor: 'constant-vus',
      exec: 'adminDashboards',
      vus: 4,
      duration: '18m',
      startTime: '45s',
    },
    courier_lifecycle_stress: {
      executor: 'constant-vus',
      exec: 'courierLifecycle',
      vus: 4,
      duration: '15m',
      startTime: '1m',
    },
  },
  thresholds: buildStressThresholds(),
  insecureSkipTLSVerify: config.insecureSkipTLSVerify,
};

export function guestCheckout() {
  runGuestCheckout();
}

export function adminDashboards() {
  runAdminDashboards();
}

export function courierLifecycle() {
  runCourierLifecycle();
}

export function trackingLive() {
  runTrackingLive();
}
