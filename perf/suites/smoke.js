import { config } from '../lib/config.js';
import { buildSmokeThresholds } from '../lib/thresholds.js';
import { runAdminDashboards } from '../scenarios/admin-dashboards.js';
import { runCourierLifecycle } from '../scenarios/courier-lifecycle.js';
import { runGuestCheckout } from '../scenarios/guest-checkout.js';
import { runTrackingLive } from '../scenarios/tracking-live.js';

export const options = {
  scenarios: {
    guest_checkout_smoke: {
      executor: 'shared-iterations',
      exec: 'guestCheckout',
      vus: 1,
      iterations: 2,
      maxDuration: '2m',
    },
    admin_dashboards_smoke: {
      executor: 'shared-iterations',
      exec: 'adminDashboards',
      vus: 1,
      iterations: 2,
      maxDuration: '2m',
      startTime: '5s',
    },
    tracking_live_smoke: {
      executor: 'shared-iterations',
      exec: 'trackingLive',
      vus: 1,
      iterations: 1,
      maxDuration: '2m',
      startTime: '15s',
    },
    ...(config.enableCourierLifecycleSmoke ? {
      courier_lifecycle_smoke: {
        executor: 'shared-iterations',
        exec: 'courierLifecycle',
        vus: 1,
        iterations: 1,
        maxDuration: '2m',
        startTime: '10s',
      },
    } : {}),
  },
  thresholds: buildSmokeThresholds(),
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
