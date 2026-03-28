import { config } from '../lib/config.js';
import { buildNominalThresholds } from '../lib/thresholds.js';
import { runAdminDashboards } from '../scenarios/admin-dashboards.js';
import { runCourierLifecycle } from '../scenarios/courier-lifecycle.js';
import { runGuestCheckout } from '../scenarios/guest-checkout.js';
import { runTrackingLive } from '../scenarios/tracking-live.js';

export const options = {
  scenarios: {
    guest_checkout_nominal: {
      executor: 'ramping-vus',
      exec: 'guestCheckout',
      stages: [
        { duration: '2m', target: 5 },
        { duration: '6m', target: 10 },
        { duration: '2m', target: 0 },
      ],
      gracefulRampDown: '30s',
    },
    admin_dashboards_nominal: {
      executor: 'constant-vus',
      exec: 'adminDashboards',
      vus: 2,
      duration: '10m',
      startTime: '20s',
    },
    tracking_live_nominal: {
      executor: 'constant-vus',
      exec: 'trackingLive',
      vus: 5,
      duration: '10m',
      startTime: '1m',
    },
    ...(config.enableCourierLifecycleSmoke ? {
      courier_lifecycle_nominal: {
        executor: 'constant-vus',
        exec: 'courierLifecycle',
        vus: 2,
        duration: '5m',
        startTime: '40s',
      },
    } : {}),
  },
  thresholds: buildNominalThresholds(),
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
