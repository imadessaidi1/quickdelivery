function envNumber(key, fallback) {
  const value = Number(__ENV[key]);
  return Number.isFinite(value) && value > 0 ? value : fallback;
}

export function buildSmokeThresholds() {
  return {
    http_req_failed: ['rate<0.01'],
    checks: ['rate>0.99'],
    'http_req_duration{flow:courier-create}': [`p(95)<${envNumber('COURIER_CREATE_P95_MS', 2500)}`],
    'http_req_duration{flow:courier-fetch}': [`p(95)<${envNumber('COURIER_FETCH_P95_MS', 1200)}`],
    'http_req_duration{flow:courier-validate}': [`p(95)<${envNumber('COURIER_VALIDATE_P95_MS', 1500)}`],
    'http_req_duration{flow:customer-create}': [`p(95)<${envNumber('CUSTOMER_CREATE_P95_MS', 2500)}`],
    'http_req_duration{flow:customer-verify}': [`p(95)<${envNumber('CUSTOMER_VERIFY_P95_MS', 1200)}`],
    'http_req_duration{flow:guest-estimate}': [`p(95)<${envNumber('GUEST_ESTIMATE_P95_MS', 800)}`],
    'http_req_duration{flow:guest-create}': [`p(95)<${envNumber('GUEST_CREATE_P95_MS', 2500)}`],
    'http_req_duration{flow:guest-payment}': [`p(95)<${envNumber('GUEST_PAYMENT_P95_MS', 1500)}`],
    'http_req_duration{flow:guest-consult}': [`p(95)<${envNumber('GUEST_CONSULT_P95_MS', 800)}`],
    'http_req_duration{flow:admin-metrics}': [`p(95)<${envNumber('ADMIN_DASHBOARD_P95_MS', 2000)}`],
    'http_req_duration{flow:admin-finance}': [`p(95)<${envNumber('ADMIN_DASHBOARD_P95_MS', 2000)}`],
    'http_req_duration{flow:tracking-http}': [`p(95)<${envNumber('TRACKING_HTTP_P95_MS', 1200)}`],
    'ws_session_duration{flow:tracking-ws}': ['p(95)<10000'],
  };
}

export function buildNominalThresholds() {
  return {
    http_req_failed: ['rate<0.02'],
    checks: ['rate>0.98'],
    'http_req_duration{flow:courier-create}': [`p(95)<${envNumber('COURIER_CREATE_P95_MS', 2500)}`],
    'http_req_duration{flow:courier-fetch}': [`p(95)<${envNumber('COURIER_FETCH_P95_MS', 1200)}`],
    'http_req_duration{flow:courier-validate}': [`p(95)<${envNumber('COURIER_VALIDATE_P95_MS', 1500)}`],
    'http_req_duration{flow:customer-create}': [`p(95)<${envNumber('CUSTOMER_CREATE_P95_MS', 2500)}`],
    'http_req_duration{flow:customer-verify}': [`p(95)<${envNumber('CUSTOMER_VERIFY_P95_MS', 1200)}`],
    'http_req_duration{flow:guest-estimate}': [`p(95)<${envNumber('GUEST_ESTIMATE_P95_MS', 800)}`],
    'http_req_duration{flow:guest-create}': [`p(95)<${envNumber('GUEST_CREATE_P95_MS', 2500)}`],
    'http_req_duration{flow:guest-payment}': [`p(95)<${envNumber('GUEST_PAYMENT_P95_MS', 1500)}`],
    'http_req_duration{flow:guest-consult}': [`p(95)<${envNumber('GUEST_CONSULT_P95_MS', 800)}`],
    'http_req_duration{flow:admin-metrics}': [`p(95)<${envNumber('ADMIN_DASHBOARD_P95_MS', 2000)}`],
    'http_req_duration{flow:admin-finance}': [`p(95)<${envNumber('ADMIN_DASHBOARD_P95_MS', 2000)}`],
    'http_req_duration{flow:tracking-http}': [`p(95)<${envNumber('TRACKING_HTTP_P95_MS', 1200)}`],
    'tracking_propagation_ms{flow:tracking-ws}': [`p(95)<${envNumber('TRACKING_PROPAGATION_P95_MS', 2000)}`],
  };
}

export function buildStressThresholds() {
  return {
    http_req_failed: ['rate<0.05'],
    checks: ['rate>0.95'],
    'http_req_duration{flow:courier-create}': ['p(95)<3500'],
    'http_req_duration{flow:courier-fetch}': ['p(95)<1800'],
    'http_req_duration{flow:courier-validate}': ['p(95)<2500'],
    'http_req_duration{flow:customer-create}': ['p(95)<3500'],
    'http_req_duration{flow:customer-verify}': ['p(95)<1800'],
    'http_req_duration{flow:guest-estimate}': ['p(95)<1500'],
    'http_req_duration{flow:guest-create}': ['p(95)<4500'],
    'http_req_duration{flow:guest-payment}': ['p(95)<2500'],
    'http_req_duration{flow:guest-consult}': ['p(95)<1500'],
    'http_req_duration{flow:admin-metrics}': ['p(95)<3500'],
    'http_req_duration{flow:admin-finance}': ['p(95)<3500'],
    'http_req_duration{flow:tracking-http}': ['p(95)<2200'],
    'tracking_propagation_ms{flow:tracking-ws}': ['p(95)<3500'],
  };
}
