import http from 'k6/http';
import { group } from 'k6';
import { config } from '../lib/config.js';
import { adminHeaders, hasAdminAuth } from '../lib/auth.js';
import { asJson, ensureResponse, hasEnv } from '../lib/utils.js';

export function runAdminDashboards() {
  if (!hasAdminAuth()) {
    console.warn('Skipping admin dashboard scenario: ADMIN_BEARER_TOKEN is missing.');
    return;
  }

  const headers = adminHeaders();

  group('admin-dashboard-metrics', () => {
    const response = http.get(`${config.baseUrl}/packages/v1/admin/metrics`, {
      headers,
      tags: { flow: 'admin-metrics' },
    });
    ensureResponse(response, {
      'admin metrics returns 200': (res) => res.status === 200,
      'admin metrics contains serviceName': (res) => !!asJson(res)?.serviceName,
    }, { flow: 'admin-metrics' });
  });

  group('admin-dashboard-finance', () => {
    const response = http.get(`${config.baseUrl}/packages/v1/admin/financial-dashboard`, {
      headers,
      tags: { flow: 'admin-finance' },
    });
    ensureResponse(response, {
      'admin finance returns 200': (res) => res.status === 200,
      'admin finance contains generatedAt': (res) => !!asJson(res)?.generatedAt,
    }, { flow: 'admin-finance' });
  });
}

export default function adminDashboardsDefault() {
  runAdminDashboards();
}
