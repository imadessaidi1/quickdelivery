import { config } from '../lib/config.js';
import { buildNominalThresholds } from '../lib/thresholds.js';
import { runAdminDashboards } from '../scenarios/admin-dashboards.js';
import { runCourierOnboarding } from '../scenarios/courier-onboarding.js';
import { runCourierLifecycle } from '../scenarios/courier-lifecycle.js';
import { runCustomerOnboarding } from '../scenarios/customer-onboarding.js';
import { runEndToEndDelivery } from '../scenarios/e2e-delivery.js';

export const options = {
  scenarios: {
    customer_onboarding_nominal: {
      executor: 'ramping-vus',
      exec: 'customerOnboarding',
      stages: [
        { duration: '2m', target: 2 },
        { duration: '6m', target: 4 },
        { duration: '2m', target: 0 },
      ],
      gracefulRampDown: '30s',
    },
    courier_onboarding_nominal: {
      executor: 'ramping-vus',
      exec: 'courierOnboarding',
      stages: [
        { duration: '2m', target: 1 },
        { duration: '6m', target: 2 },
        { duration: '2m', target: 0 },
      ],
      gracefulRampDown: '30s',
      startTime: '10s',
    },
    e2e_delivery_nominal: {
      executor: 'ramping-vus',
      exec: 'e2eDelivery',
      stages: [
        { duration: '2m', target: 2 },
        { duration: '6m', target: 4 },
        { duration: '2m', target: 0 },
      ],
      gracefulRampDown: '30s',
      startTime: '15s',
    },
    admin_dashboards_nominal: {
      executor: 'constant-vus',
      exec: 'adminDashboards',
      vus: 2,
      duration: '10m',
      startTime: '20s',
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

export function customerOnboarding() {
  runCustomerOnboarding();
}

export function adminDashboards() {
  runAdminDashboards();
}

export function courierOnboarding() {
  runCourierOnboarding();
}

export function courierLifecycle() {
  runCourierLifecycle();
}

export function e2eDelivery() {
  runEndToEndDelivery();
}
