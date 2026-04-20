import http from 'k6/http';
import { group, sleep } from 'k6';
import { authHeaders, config } from '../lib/config.js';
import { adminHeaders, requestUserAccessToken } from '../lib/auth.js';
import { buildMultipartFormData } from '../lib/multipart.js';
import { buildEstimatePayload } from '../lib/package-payload.js';
import { buildCourierUserPayload, buildCourierVehiclePayload } from '../lib/user-payload.js';
import { asJson, ensureResponse, pickFromEnvList, sleepRange } from '../lib/utils.js';
import {
  buildDraftUserPayload,
  buildFakeDocument,
  buildValidationPayload,
  waitForCourierFetch,
  waitForCourierOnboarding,
} from './courier-onboarding.js';

const ROUTE_START = { lat: 48.74845, lng: 2.48856 };
const ROUTE_END = { lat: 48.74683, lng: 2.40548 };

function createAndValidateCourier() {
  const userPayload = buildCourierUserPayload();
  const vehiclePayload = buildCourierVehiclePayload();
  let createdUserId = null;
  let resumeToken = null;
  let fetchedUser = null;

  group('route-courier-create', () => {
    const response = http.post(
      `${config.baseUrl}/users/v1/create-account`,
      JSON.stringify({ user: userPayload, locale: config.locale }),
      { headers: { 'Content-Type': 'application/json' }, tags: { flow: 'route-courier-create', name: '/users/v1/create-account' } },
    );
    const body = asJson(response);
    createdUserId = body?.id || null;
    resumeToken = body?.onboarding?.resumeToken || null;
    ensureResponse(response, {
      'route courier create returns 200': (res) => res.status === 200,
      'route courier create returns id': () => Number(createdUserId) > 0,
      'route courier create returns resume token': () => !!resumeToken,
    }, { flow: 'route-courier-create' });
  });

  if (!createdUserId || !resumeToken) {
    return null;
  }

  sleep(sleepRange(0.2, 0.5));

  group('route-courier-save-address', () => {
    const multipart = buildMultipartFormData({
      user: JSON.stringify(buildDraftUserPayload(userPayload, createdUserId, resumeToken)),
      vehicle: JSON.stringify({}),
      locale: config.locale,
      step: '2',
    });
    const response = http.post(`${config.baseUrl}/users/v1/save-onboarding-draft`, multipart.body, {
      headers: { 'Content-Type': multipart.contentType },
      tags: { flow: 'route-courier-save', name: '/users/v1/save-onboarding-draft' },
    });
    const body = asJson(response);
    resumeToken = body?.onboarding?.resumeToken || resumeToken;
    ensureResponse(response, {
      'route courier save address returns 200': (res) => res.status === 200,
    }, { flow: 'route-courier-save' });
  });

  group('route-courier-save-documents', () => {
    const multipart = buildMultipartFormData({
      user: JSON.stringify(buildDraftUserPayload(userPayload, createdUserId, resumeToken)),
      vehicle: JSON.stringify({}),
      step: '3',
      locale: config.locale,
      ID: buildFakeDocument('ID'),
      PICTURE: { filename: 'picture.jpg', contentType: 'image/jpeg', content: 'fake-jpeg-content-quickdelivery-perf' },
      DRIVER_LICENCE: buildFakeDocument('DRIVER_LICENCE'),
      USER_COMPANY_EXTRACT: buildFakeDocument('USER_COMPANY_EXTRACT'),
      USER_COMPANY_INSURANCE: buildFakeDocument('USER_COMPANY_INSURANCE'),
    });
    const response = http.post(`${config.baseUrl}/users/v1/save-onboarding-draft`, multipart.body, {
      headers: { 'Content-Type': multipart.contentType },
      tags: { flow: 'route-courier-save', name: '/users/v1/save-onboarding-draft' },
    });
    const body = asJson(response);
    resumeToken = body?.onboarding?.resumeToken || resumeToken;
    ensureResponse(response, {
      'route courier save documents returns 200': (res) => res.status === 200,
    }, { flow: 'route-courier-save' });
  });

  group('route-courier-save-vehicle', () => {
    const multipart = buildMultipartFormData({
      user: JSON.stringify(buildDraftUserPayload(userPayload, createdUserId, resumeToken)),
      vehicle: JSON.stringify(vehiclePayload),
      step: '4',
      locale: config.locale,
      GRAY_CARD: buildFakeDocument('GRAY_CARD'),
      INSURANCE: buildFakeDocument('INSURANCE'),
    });
    const response = http.post(`${config.baseUrl}/users/v1/save-onboarding-draft`, multipart.body, {
      headers: { 'Content-Type': multipart.contentType },
      tags: { flow: 'route-courier-save', name: '/users/v1/save-onboarding-draft' },
    });
    const body = asJson(response);
    resumeToken = body?.onboarding?.resumeToken || resumeToken;
    ensureResponse(response, {
      'route courier save vehicle returns 200': (res) => res.status === 200,
    }, { flow: 'route-courier-save' });
  });

  group('route-courier-complete', () => {
    const multipart = buildMultipartFormData({
      user: JSON.stringify(buildDraftUserPayload(userPayload, createdUserId, resumeToken)),
      vehicle: JSON.stringify(vehiclePayload),
      locale: config.locale,
    });
    const response = http.post(`${config.baseUrl}/users/v1/complete-onboarding`, multipart.body, {
      headers: { 'Content-Type': multipart.contentType },
      tags: { flow: 'route-courier-complete', name: '/users/v1/complete-onboarding' },
    });
    if (response.status !== 200) {
      console.warn(`route courier complete failed: HTTP ${response.status} ${response.body}`);
    }
    ensureResponse(response, {
      'route courier complete returns 200': (res) => res.status === 200,
    }, { flow: 'route-courier-complete' });
  });

  const onboardingState = waitForCourierOnboarding(userPayload.emailAddress);
  if (!onboardingState.ready) {
    ensureResponse({ status: 409 }, {
      'route courier onboarding ready': () => false,
    }, { flow: 'route-courier-complete' });
    return null;
  }

  group('route-courier-fetch-and-validate', () => {
    const result = waitForCourierFetch(userPayload.emailAddress, createdUserId);
    fetchedUser = result.body;
    ensureResponse(result.response, {
      'route courier fetch returns 200': (res) => res.status === 200,
      'route courier fetch returns documents': () => Object.keys(fetchedUser?.document || {}).length >= 7,
    }, { flow: 'route-courier-fetch' });

    if (fetchedUser?.id) {
      const validationMultipart = buildMultipartFormData({
        user: JSON.stringify(buildValidationPayload(fetchedUser)),
        locale: config.locale,
      });
      const response = http.put(`${config.baseUrl}/users/v1/validateUser`, validationMultipart.body, {
        headers: { 'Content-Type': validationMultipart.contentType, ...adminHeaders() },
        tags: { flow: 'route-courier-validate', name: '/users/v1/validateUser' },
      });
      ensureResponse(response, {
        'route courier validate returns 200': (res) => res.status === 200,
      }, { flow: 'route-courier-validate' });
    }
  });

  const token = requestUserAccessToken(userPayload.emailAddress, userPayload.password);
  ensureResponse({ status: token ? 200 : 401 }, {
    'route courier token resolved': () => !!token,
  }, { flow: 'route-courier-auth' });

  return token && fetchedUser?.id ? { id: fetchedUser.id, token } : null;
}

function createPaidPackage(seedLabel) {
  const packageDTO = {
    ...buildEstimatePayload({
      seed: `ROUTE-${seedLabel}-${Date.now()}`,
      departure: {
        line1: '3 Rue Pasteur',
        line2: '',
        town: 'Limeil-Brevannes',
        zipCode: '94450',
        country: 'France',
        latitude: ROUTE_START.lat,
        longitude: ROUTE_START.lng,
        addressAuto: '3 Rue Pasteur, 94450 Limeil-Brevannes, France',
      },
      arrival: {
        line1: '2 Rue Anatole France',
        line2: '',
        town: 'Orly',
        zipCode: '94310',
        country: 'France',
        latitude: ROUTE_END.lat,
        longitude: ROUTE_END.lng,
        addressAuto: '2 Rue Anatole France, 94310 Orly, France',
      },
      referencePrefix: 'PERF-ROUTE',
    }),
    status: 'PAYMENTPENDING',
  };
  let packageId = null;
  let packageReference = null;
  let guestAccessToken = null;

  group('route-package-create', () => {
    const response = http.post(
      `${config.baseUrl}/packages/v1/create`,
      { packageDTO: JSON.stringify(packageDTO), locale: config.locale },
      { tags: { flow: 'route-package-create', name: '/packages/v1/create' } },
    );
    const body = asJson(response);
    if (response.status !== 200) {
      console.warn(`route package create failed: HTTP ${response.status} ${response.body}`);
    }
    packageId = body?.id || null;
    packageReference = body?.reference || null;
    guestAccessToken = body?.guestAccessToken || null;
    ensureResponse(response, {
      'route package create returns 200': (res) => res.status === 200,
      'route package create returns id': () => Number(packageId) > 0,
      'route package create returns token': () => !!guestAccessToken,
    }, { flow: 'route-package-create' });
  });

  if (!packageId || !guestAccessToken) {
    return null;
  }

  group('route-package-payment', () => {
    const response = http.put(
      `${config.baseUrl}/packages/v1/confirm-guest-payment?packageID=${packageId}&guestAccessToken=${encodeURIComponent(guestAccessToken)}`,
      null,
      { tags: { flow: 'route-package-payment', name: '/packages/v1/confirm-guest-payment' }, responseCallback: http.expectedStatuses(200, 409) },
    );
    ensureResponse(response, {
      'route package payment returns success or conflict': (res) => res.status === 200 || res.status === 409,
    }, { flow: 'route-package-payment' });
  });

  return { id: packageId, reference: packageReference, guestAccessToken };
}

function reservePlannedRoute(courier, packageIds) {
  let result = null;
  const routePlan = {
    mode: 'PERSONAL_ROUTE',
    deliveryMode: 'STANDARD',
    vehicleType: 'CAR',
    start: ROUTE_START,
    end: ROUTE_END,
    packageIds,
  };

  group('route-plan-reserve-batch', () => {
    const response = http.put(
      `${config.baseUrl}/packages/v1/reserve-batch-planned?deliveryPersonID=${encodeURIComponent(courier.id)}&locale=${encodeURIComponent(config.locale)}`,
      JSON.stringify({ routePlan }),
      {
        headers: { 'Content-Type': 'application/json', ...authHeaders(courier.token) },
        tags: { flow: 'route-reserve-batch-planned', name: '/packages/v1/reserve-batch-planned' },
      },
    );
    result = asJson(response);
    if (response.status !== 200 || Number(result?.reservedCount || 0) === 0) {
      console.warn(`route reserve planned result: HTTP ${response.status} ${JSON.stringify(result)}`);
    }
    ensureResponse(response, {
      'route reserve planned returns 200': (res) => res.status === 200,
      'route reserve planned reserves packages': () => Number(result?.reservedCount || 0) > 0,
      'route reserve planned returns route': () => Number(result?.reservedRoutePlan?.routeId || 0) > 0,
    }, { flow: 'route-reserve-batch-planned' });
  });

  return result;
}

function getDeliveryContext(courier, packageId) {
  const response = http.get(
    `${config.baseUrl}/packages/v1/delivery-context?packageID=${encodeURIComponent(packageId)}&deliveryPersonID=${encodeURIComponent(courier.id)}`,
    { headers: authHeaders(courier.token), tags: { flow: 'route-delivery-context', name: '/packages/v1/delivery-context' } },
  );
  const body = asJson(response);
  ensureResponse(response, {
    'route delivery context returns 200': (res) => res.status === 200,
    'route delivery context has pickup otp': () => !!body?.pickUpOTP,
  }, { flow: 'route-delivery-context' });
  return body;
}

function runRouteCancellation(courier) {
  const packages = [createPaidPackage('CANCEL-A'), createPaidPackage('CANCEL-B')].filter(Boolean);
  if (packages.length === 0) return;

  const reserveResult = reservePlannedRoute(courier, packages.map((pkg) => pkg.id));
  if (!reserveResult?.reservedRoutePlan?.routeId) return;

  group('route-active-before-cancel', () => {
    const response = http.get(
      `${config.baseUrl}/packages/v1/active-route?deliveryPersonId=${encodeURIComponent(courier.id)}`,
      { headers: authHeaders(courier.token), tags: { flow: 'route-active', name: '/packages/v1/active-route' } },
    );
    ensureResponse(response, {
      'route active before cancel returns 200': (res) => res.status === 200,
    }, { flow: 'route-active' });
  });

  group('route-cancel-before-start', () => {
    const response = http.put(
      `${config.baseUrl}/packages/v1/cancel-active-route?deliveryPersonId=${encodeURIComponent(courier.id)}`,
      null,
      { headers: authHeaders(courier.token), tags: { flow: 'route-cancel-before-start', name: '/packages/v1/cancel-active-route' }, responseCallback: http.expectedStatuses(200, 204) },
    );
    ensureResponse(response, {
      'route cancel before start returns success': (res) => res.status === 200 || res.status === 204,
    }, { flow: 'route-cancel-before-start' });
  });
}

function runRouteExecution(courier) {
  const packages = [createPaidPackage('EXEC-A')].filter(Boolean);
  if (packages.length === 0) return;

  const reserveResult = reservePlannedRoute(courier, packages.map((pkg) => pkg.id));
  if (!reserveResult?.reservedRoutePlan?.routeId) return;

  group('route-start-active', () => {
    const response = http.put(
      `${config.baseUrl}/packages/v1/start-active-route?deliveryPersonId=${encodeURIComponent(courier.id)}`,
      null,
      { headers: authHeaders(courier.token), tags: { flow: 'route-start', name: '/packages/v1/start-active-route' } },
    );
    ensureResponse(response, {
      'route start returns 200': (res) => res.status === 200,
    }, { flow: 'route-start' });
  });

  const context = getDeliveryContext(courier, packages[0].id);
  if (!context?.pickUpOTP) return;

  let deliveryOtp = '';
  group('route-pickup', () => {
    const response = http.put(
      `${config.baseUrl}/packages/v1/pickup?packageID=${encodeURIComponent(packages[0].id)}&deliveryPersonID=${encodeURIComponent(courier.id)}&pickUpOTP=${encodeURIComponent(context.pickUpOTP)}&currentLatitude=${ROUTE_START.lat}&currentLongitude=${ROUTE_START.lng}&locale=${encodeURIComponent(config.locale)}`,
      null,
      { headers: authHeaders(courier.token), tags: { flow: 'route-pickup', name: '/packages/v1/pickup' } },
    );
    const body = asJson(response);
    deliveryOtp = body?.deliveryOTP || '';
    ensureResponse(response, {
      'route pickup returns 200': (res) => res.status === 200,
      'route pickup returns delivery otp': () => !!deliveryOtp,
    }, { flow: 'route-pickup' });
  });

  if (!deliveryOtp) return;

  group('route-deliver', () => {
    const response = http.put(
      `${config.baseUrl}/packages/v1/deliver?packageID=${encodeURIComponent(packages[0].id)}&deliveryPersonID=${encodeURIComponent(courier.id)}&deliveryOTP=${encodeURIComponent(deliveryOtp)}&currentLatitude=${ROUTE_END.lat}&currentLongitude=${ROUTE_END.lng}&locale=${encodeURIComponent(config.locale)}`,
      null,
      { headers: authHeaders(courier.token), tags: { flow: 'route-deliver', name: '/packages/v1/deliver' } },
    );
    if (response.status !== 200) {
      console.warn(`route deliver failed: HTTP ${response.status} ${response.body}`);
    }
    ensureResponse(response, {
      'route deliver returns 200': (res) => res.status === 200,
    }, { flow: 'route-deliver' });
  });
}

export function runRouteLifecycle() {
  let cancellationCourier = null;
  let executionCourier = null;
  const fallbackCourierId = pickFromEnvList(config.courierIdPool, config.courierId);
  const cancellationCourierId = pickFromEnvList(config.routeCancellationCourierIdPool, config.routeCancellationCourierId || fallbackCourierId);
  const executionCourierId = pickFromEnvList(config.routeExecutionCourierIdPool, config.routeExecutionCourierId || fallbackCourierId);
  if (fallbackCourierId && config.courierUsername && config.courierPassword) {
    const token = requestUserAccessToken(config.courierUsername, config.courierPassword);
    cancellationCourier = token ? { id: cancellationCourierId, token } : null;
    executionCourier = token ? { id: executionCourierId || fallbackCourierId, token } : null;
    ensureResponse({ status: cancellationCourier ? 200 : 401 }, {
      'route env courier token resolved': () => !!cancellationCourier,
    }, { flow: 'route-courier-auth' });
  }
  if (!cancellationCourier) {
    cancellationCourier = createAndValidateCourier();
    executionCourier = cancellationCourier;
  }
  if (!cancellationCourier || !executionCourier) return;
  sleep(sleepRange(0.5, 1.0));
  runRouteCancellation(cancellationCourier);
  sleep(sleepRange(0.5, 1.0));
  runRouteExecution(executionCourier);
}

export default function routeLifecycleDefault() {
  runRouteLifecycle();
}
