import { config } from '../lib/config.js';
import { buildStressThresholds } from '../lib/thresholds.js';
import { runAdminDashboards } from '../scenarios/admin-dashboards.js';
import { runCourierOnboarding } from '../scenarios/courier-onboarding.js';
import { runCustomerOnboarding } from '../scenarios/customer-onboarding.js';
import { runEndToEndDelivery } from '../scenarios/e2e-delivery.js';

export const options = {
  scenarios: {
    customer_onboarding_stress: {
      executor: 'ramping-vus',
      exec: 'customerOnboarding',
      stages: [
        { duration: '3m', target: 4 },
        { duration: '5m', target: 8 },
        { duration: '5m', target: 12 },
        { duration: '5m', target: 0 },
      ],
      gracefulRampDown: '30s',
    },
    courier_onboarding_stress: {
      executor: 'ramping-vus',
      exec: 'courierOnboarding',
      startTime: '20s',
      stages: [
        { duration: '3m', target: 2 },
        { duration: '5m', target: 4 },
        { duration: '5m', target: 6 },
        { duration: '5m', target: 0 },
      ],
      gracefulRampDown: '30s',
    },
    e2e_delivery_stress: {
      executor: 'ramping-vus',
      exec: 'e2eDelivery',
      startTime: '30s',
      stages: [
        { duration: '3m', target: 4 },
        { duration: '5m', target: 8 },
        { duration: '5m', target: 12 },
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
  },
  thresholds: buildStressThresholds(),
  insecureSkipTLSVerify: config.insecureSkipTLSVerify,
};

export function customerOnboarding() {
  runCustomerOnboarding();
}

export function courierOnboarding() {
  runCourierOnboarding();
}

export function adminDashboards() {
  runAdminDashboards();
}

export function e2eDelivery() {
  runEndToEndDelivery();
}
