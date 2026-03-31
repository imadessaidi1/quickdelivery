import { config } from '../lib/config.js';
import { buildSmokeThresholds } from '../lib/thresholds.js';
import { runAdminDashboards } from '../scenarios/admin-dashboards.js';
import { runCourierOnboarding } from '../scenarios/courier-onboarding.js';
import { runCustomerOnboarding } from '../scenarios/customer-onboarding.js';
import { runEndToEndDelivery } from '../scenarios/e2e-delivery.js';

export const options = {
  scenarios: {
    customer_onboarding_smoke: {
      executor: 'shared-iterations',
      exec: 'customerOnboarding',
      vus: 1,
      iterations: 2,
      maxDuration: '2m',
    },
    courier_onboarding_smoke: {
      executor: 'shared-iterations',
      exec: 'courierOnboarding',
      vus: 1,
      iterations: 1,
      maxDuration: '2m',
      startTime: '3s',
    },
    e2e_delivery_smoke: {
      executor: 'shared-iterations',
      exec: 'e2eDelivery',
      vus: 1,
      iterations: 1,
      maxDuration: '3m',
      startTime: '6s',
    },
    admin_dashboards_smoke: {
      executor: 'shared-iterations',
      exec: 'adminDashboards',
      vus: 1,
      iterations: 2,
      maxDuration: '2m',
      startTime: '10s',
    },
  },
  thresholds: buildSmokeThresholds(),
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
