import http from 'k6/http';
import { group, sleep } from 'k6';
import { config } from '../lib/config.js';
import { adminHeaders, hasAdminAuth } from '../lib/auth.js';
import { buildMultipartFormData } from '../lib/multipart.js';
import { buildCourierUserPayload, buildCourierVehiclePayload } from '../lib/user-payload.js';
import { asJson, ensureResponse, hasEnv, sleepRange } from '../lib/utils.js';

function waitForCourierOnboarding(emailAddress) {
  const maxAttempts = 24;
  for (let attempt = 0; attempt < maxAttempts; attempt += 1) {
    const response = http.get(
      `${config.baseUrl}/users/v1/onboarding-status?email=${encodeURIComponent(emailAddress)}`,
      {
        headers: adminHeaders(),
        tags: { flow: 'courier-onboarding-status' },
      },
    );
    const body = asJson(response);
    if (response.status === 200) {
      if (body?.status === 'FAILED') {
        return { ready: false, failed: true, body };
      }
      if (body?.readyForValidation === true || body?.status === 'READY_FOR_VALIDATION') {
        return { ready: true, failed: false, body };
      }
    }
    sleep(1);
  }
  return { ready: false, failed: false, body: null };
}

function waitForCourierFetch(emailAddress, expectedUserId) {
  const maxAttempts = 12;
  let latestBody = null;
  let latestStatus = 0;

  for (let attempt = 0; attempt < maxAttempts; attempt += 1) {
    const response = http.get(
      `${config.baseUrl}/users/v1/userByEmail?email=${encodeURIComponent(emailAddress)}`,
      {
        headers: adminHeaders(),
        tags: { flow: 'courier-fetch' },
      },
    );
    latestStatus = response.status;
    latestBody = asJson(response);

    const documentCount = Object.keys(latestBody?.document || {}).length;
    if (response.status === 200 && Number(latestBody?.id) === Number(expectedUserId) && documentCount >= 7) {
      return { response, body: latestBody };
    }

    sleep(1);
  }

  return {
    response: { status: latestStatus },
    body: latestBody,
  };
}

function buildFakeDocument(documentType) {
  return {
    filename: `${documentType.toLowerCase()}.pdf`,
    contentType: 'application/pdf',
    content: `%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\n% quickdelivery perf ${documentType}\n`,
  };
}

function buildCourierDocuments() {
  return {
    ID: buildFakeDocument('ID'),
    PICTURE: {
      filename: 'picture.jpg',
      contentType: 'image/jpeg',
      content: 'fake-jpeg-content-quickdelivery-perf',
    },
    DRIVER_LICENCE: buildFakeDocument('DRIVER_LICENCE'),
    USER_COMPANY_EXTRACT: buildFakeDocument('USER_COMPANY_EXTRACT'),
    USER_COMPANY_INSURANCE: buildFakeDocument('USER_COMPANY_INSURANCE'),
    GRAY_CARD: buildFakeDocument('GRAY_CARD'),
    INSURANCE: buildFakeDocument('INSURANCE'),
  };
}

function buildValidationPayload(user) {
  const payload = JSON.parse(JSON.stringify(user || {}));
  payload.activeAccount = true;
  payload.emailAddressValidation = true;
  if (payload.document) {
    Object.keys(payload.document).forEach((documentType) => {
      payload.document[documentType].documentStatus = 'ACCEPTED';
      payload.document[documentType].reviewComment = '';
      payload.document[documentType].data = '';
    });
  }
  return payload;
}

function buildDraftUserPayload(userPayload, createdUserId, resumeToken) {
  return {
    ...userPayload,
    id: createdUserId,
    onboarding: { resumeToken },
    personalAddress: [
      {
        line1: '10 avenue de la Republique',
        line2: '',
        town: 'Paris',
        zipCode: '75011',
        country: 'France',
        type: 'RESIDENCE',
      },
    ],
  };
}

export function runCourierOnboarding() {
  if (!hasAdminAuth()) {
    console.warn('Skipping courier onboarding: admin token is missing.');
    return;
  }

  const userPayload = buildCourierUserPayload();
  const vehiclePayload = buildCourierVehiclePayload();
  let createdUserId = null;
  let resumeToken = null;
  let fetchedUser = null;

  group('courier-onboarding-create', () => {
    const response = http.post(
      `${config.baseUrl}/users/v1/create-account`,
      JSON.stringify({
        user: userPayload,
        locale: config.locale,
      }),
      {
        headers: {
          'Content-Type': 'application/json',
        },
        tags: { flow: 'courier-create' },
      },
    );

    const body = asJson(response);
    createdUserId = body?.id || null;
    resumeToken = body?.onboarding?.resumeToken || null;

    ensureResponse(response, {
      'courier create returns 200': (res) => res.status === 200,
      'courier create returns id': () => Number(createdUserId) > 0,
      'courier create echoes email': () => body?.emailAddress === userPayload.emailAddress,
      'courier create returns resume token': () => !!resumeToken,
    }, { flow: 'courier-create' });
  });

  if (!createdUserId) {
    return;
  }

  sleep(sleepRange(0.3, 0.8));

  group('courier-onboarding-resume-create', () => {
    const response = http.post(
      `${config.baseUrl}/users/v1/create-account`,
      JSON.stringify({
        user: userPayload,
        locale: config.locale,
      }),
      {
        headers: {
          'Content-Type': 'application/json',
        },
        tags: { flow: 'courier-resume-create' },
      },
    );
    const body = asJson(response);
    ensureResponse(response, {
      'courier resume create returns 200': (res) => res.status === 200,
      'courier resume create returns same id': () => Number(body?.id) === Number(createdUserId),
      'courier resume create keeps step 2': () => Number(body?.onboarding?.currentStep) === 2,
    }, { flow: 'courier-resume-create' });
  });

  sleep(sleepRange(0.2, 0.6));

  group('courier-onboarding-save-address', () => {
    const multipart = buildMultipartFormData({
      user: JSON.stringify(buildDraftUserPayload(userPayload, createdUserId, resumeToken)),
      vehicle: JSON.stringify({}),
      locale: config.locale,
      step: '2',
    });
    const response = http.post(
      `${config.baseUrl}/users/v1/save-onboarding-draft`,
      multipart.body,
      {
        headers: {
          'Content-Type': multipart.contentType,
        },
        tags: { flow: 'courier-save-step-2' },
      },
    );
    const body = asJson(response);
    resumeToken = body?.onboarding?.resumeToken || resumeToken;
    ensureResponse(response, {
      'courier save step 2 returns 200': (res) => res.status === 200,
      'courier save step 2 moves to step 3': () => Number(body?.onboarding?.currentStep) === 3,
    }, { flow: 'courier-save-step-2' });
  });

  sleep(sleepRange(0.2, 0.6));

  group('courier-onboarding-save-documents', () => {
    const multipart = buildMultipartFormData({
      user: JSON.stringify(buildDraftUserPayload(userPayload, createdUserId, resumeToken)),
      vehicle: JSON.stringify({}),
      step: '3',
      locale: config.locale,
      ID: buildFakeDocument('ID'),
      PICTURE: {
        filename: 'picture.jpg',
        contentType: 'image/jpeg',
        content: 'fake-jpeg-content-quickdelivery-perf',
      },
      DRIVER_LICENCE: buildFakeDocument('DRIVER_LICENCE'),
      USER_COMPANY_EXTRACT: buildFakeDocument('USER_COMPANY_EXTRACT'),
      USER_COMPANY_INSURANCE: buildFakeDocument('USER_COMPANY_INSURANCE'),
    });
    const response = http.post(
      `${config.baseUrl}/users/v1/save-onboarding-draft`,
      multipart.body,
      {
        headers: {
          'Content-Type': multipart.contentType,
        },
        tags: { flow: 'courier-save-step-3' },
      },
    );
    const body = asJson(response);
    resumeToken = body?.onboarding?.resumeToken || resumeToken;
    ensureResponse(response, {
      'courier save step 3 returns 200': (res) => res.status === 200,
      'courier save step 3 moves to step 4': () => Number(body?.onboarding?.currentStep) === 4,
    }, { flow: 'courier-save-step-3' });
  });

  sleep(sleepRange(0.3, 0.8));

  group('courier-onboarding-save-vehicle', () => {
    const multipart = buildMultipartFormData({
      user: JSON.stringify(buildDraftUserPayload(userPayload, createdUserId, resumeToken)),
      vehicle: JSON.stringify(vehiclePayload),
      step: '4',
      locale: config.locale,
      GRAY_CARD: buildFakeDocument('GRAY_CARD'),
      INSURANCE: buildFakeDocument('INSURANCE'),
    });
    const response = http.post(
      `${config.baseUrl}/users/v1/save-onboarding-draft`,
      multipart.body,
      {
        headers: {
          'Content-Type': multipart.contentType,
        },
        tags: { flow: 'courier-save-step-4' },
      },
    );
    const body = asJson(response);
    resumeToken = body?.onboarding?.resumeToken || resumeToken;
    ensureResponse(response, {
      'courier save step 4 returns 200': (res) => res.status === 200,
      'courier save step 4 moves to step 5': () => Number(body?.onboarding?.currentStep) === 5,
    }, { flow: 'courier-save-step-4' });
  });

  sleep(sleepRange(0.3, 0.8));

  group('courier-onboarding-complete', () => {
    const multipart = buildMultipartFormData({
      user: JSON.stringify(buildDraftUserPayload(userPayload, createdUserId, resumeToken)),
      vehicle: JSON.stringify(vehiclePayload),
      locale: config.locale,
    });
    const response = http.post(
      `${config.baseUrl}/users/v1/complete-onboarding`,
      multipart.body,
      {
        headers: {
          'Content-Type': multipart.contentType,
        },
        tags: { flow: 'courier-complete' },
      },
    );
    ensureResponse(response, {
      'courier complete onboarding returns 200': (res) => res.status === 200,
    }, { flow: 'courier-complete' });
  });

  sleep(sleepRange(0.3, 0.8));

  const onboardingState = waitForCourierOnboarding(userPayload.emailAddress);
  if (onboardingState.failed) {
    ensureResponse({ status: 500 }, {
      'courier onboarding async processing succeeded': () => false,
    }, { flow: 'courier-complete' });
    return;
  }

  group('courier-onboarding-fetch', () => {
    const result = waitForCourierFetch(userPayload.emailAddress, createdUserId);
    fetchedUser = result.body;
    ensureResponse(result.response, {
      'courier fetch returns 200': (res) => res.status === 200,
      'courier fetch returns same id': () => Number(fetchedUser?.id) === Number(createdUserId),
      'courier fetch returns documents': () => Object.keys(fetchedUser?.document || {}).length >= 7,
    }, { flow: 'courier-fetch' });
  });

  ensureResponse({ status: onboardingState.ready ? 200 : 409 }, {
    'courier onboarding async processing succeeded': () => !!onboardingState.ready,
  }, { flow: 'courier-complete' });

  if (!fetchedUser?.id || !onboardingState.ready) {
    return;
  }

  sleep(sleepRange(0.3, 0.8));

  group('courier-onboarding-validate', () => {
    const validationMultipart = buildMultipartFormData({
      user: JSON.stringify(buildValidationPayload(fetchedUser)),
      locale: config.locale,
    });
    const response = http.put(
      `${config.baseUrl}/users/v1/validateUser`,
      validationMultipart.body,
      {
        headers: {
          'Content-Type': validationMultipart.contentType,
          ...adminHeaders(),
        },
        tags: { flow: 'courier-validate' },
      },
    );
    const body = asJson(response);
    ensureResponse(response, {
      'courier validate returns 200': (res) => res.status === 200,
      'courier validate activates account': () => body?.activeAccount === true,
    }, { flow: 'courier-validate' });
  });
}

export default function courierOnboardingDefault() {
  runCourierOnboarding();
}
