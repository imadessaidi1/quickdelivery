import http from 'k6/http';
import ws from 'k6/ws';
import { check, group, sleep } from 'k6';
import exec from 'k6/execution';
import { Counter, Trend } from 'k6/metrics';
import { authHeaders, config } from '../lib/config.js';
import { hasTrackingAuth, resolveTrackingToken, trackingHeaders } from '../lib/auth.js';
import { hasEnv } from '../lib/utils.js';

const trackingPropagationTrend = new Trend('tracking_propagation_ms');
const trackingMessagesReceived = new Counter('tracking_messages_received');

function positionSequence() {
  return [
    { latitude: 48.8673, longitude: 2.3691 },
    { latitude: 48.8667, longitude: 2.3704 },
    { latitude: 48.8658, longitude: 2.3716 },
  ];
}

export const options = {
  scenarios: {
    tracking_live: {
      executor: 'shared-iterations',
      vus: 1,
      iterations: 1,
      maxDuration: '1m',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.02'],
    checks: ['rate>0.98'],
    'http_req_duration{flow:tracking-http}': [`p(95)<${Number(__ENV.TRACKING_HTTP_P95_MS || 1200)}`],
    tracking_propagation_ms: [`p(95)<${Number(__ENV.TRACKING_PROPAGATION_P95_MS || 2000)}`],
  },
  insecureSkipTLSVerify: config.insecureSkipTLSVerify,
};

function runTrackingAttempt({ bearerToken, deliveryPersonId, packageReference, guestAccessToken }) {
  const resolvedToken = resolveTrackingToken(bearerToken);
  const subscriptionMessage = JSON.stringify({
    type: 'TRACK_PACKAGE_SUBSCRIBE',
    from: `k6-subscriber-${exec.vu.idInTest}`,
    to: 'PACKAGE_SERVICE',
    packageReference,
    guestAccessToken: guestAccessToken || null,
  });

  let sentAt = 0;
  let receivedCount = 0;
  let handshakeOk = false;
  let subscribed = false;
  let publishingStarted = false;

  function startPublishing(socket) {
    if (publishingStarted) {
      return;
    }
    publishingStarted = true;

      const trackingUpdateUrl = packageReference
        ? `${config.baseUrl}/packages/v1/tracking/package-position?packageReference=${encodeURIComponent(packageReference)}`
        : `${config.baseUrl}/packages/v1/tracking/position?deliveryPersonId=${encodeURIComponent(deliveryPersonId)}`;

      positionSequence().forEach((position, index) => {
        group(`tracking-http-update-${index + 1}`, () => {
          sentAt = Date.now();
          const publish = http.post(
            trackingUpdateUrl,
            JSON.stringify(position),
            {
            headers: {
              ...(bearerToken ? authHeaders(bearerToken) : trackingHeaders()),
              'Content-Type': 'application/json',
            },
            tags: { flow: 'tracking-http' },
          },
        );
        check(publish, {
          'tracking update returns 200': (res) => res.status === 200,
        }, { flow: 'tracking-http' });
        sleep(1);
      });
    });

    socket.setTimeout(() => {
        if (receivedCount === 0) {
          const retryPosition = positionSequence()[positionSequence().length - 1];
          sentAt = Date.now();
          const retryPublish = http.post(
            trackingUpdateUrl,
            JSON.stringify(retryPosition),
            {
            headers: {
              ...(bearerToken ? authHeaders(bearerToken) : trackingHeaders()),
              'Content-Type': 'application/json',
            },
            tags: { flow: 'tracking-http' },
          },
        );
        check(retryPublish, {
          'tracking update returns 200': (res) => res.status === 200,
        }, { flow: 'tracking-http' });
      }
      socket.setTimeout(() => socket.close(), receivedCount === 0 ? 3500 : 1500);
    }, 3000);
  }

  const response = ws.connect(config.wsUrl, { tags: { flow: 'tracking-ws' } }, (socket) => {
    socket.on('open', () => {
      handshakeOk = true;
      socket.send(subscriptionMessage);
      socket.setTimeout(() => {
        if (!publishingStarted) {
          startPublishing(socket);
        }
      }, 1500);
    });

    socket.on('message', (raw) => {
      try {
        const payload = JSON.parse(raw);
        if (payload.type === 'TRACK_PACKAGE_SUBSCRIBED' && payload.packageReference === packageReference) {
          subscribed = true;
          startPublishing(socket);
          return;
        }
        if (payload.type === 'PACKAGE_POSITION_UPDATE' && payload.packageReference === packageReference) {
          receivedCount += 1;
          trackingMessagesReceived.add(1);
          if (sentAt > 0) {
            trackingPropagationTrend.add(Date.now() - sentAt, { flow: 'tracking-ws' });
          }
        }
      } catch (_) {
        // Ignore non-JSON frames.
      }
    });
  });

  return {
    response,
    handshakeOk: handshakeOk || (response && response.status === 101),
    subscribed,
    receivedCount,
  };
}

export function runTrackingLiveForPackage({ bearerToken, deliveryPersonId, packageReference, guestAccessToken }) {
  let finalAttempt = null;
  const maxAttempts = 2;

  for (let attempt = 0; attempt < maxAttempts; attempt += 1) {
    finalAttempt = runTrackingAttempt({
      bearerToken,
      deliveryPersonId,
      packageReference,
      guestAccessToken,
    });
    if (finalAttempt.receivedCount > 0) {
      break;
    }
    sleep(0.8);
  }

  check(finalAttempt?.response, {
    'tracking websocket handshake ok': () => !!finalAttempt?.handshakeOk,
    'tracking websocket subscribed': () => !!finalAttempt?.subscribed,
    'tracking websocket received updates': () => Number(finalAttempt?.receivedCount || 0) > 0,
  }, { flow: 'tracking-ws' });
}

export function runTrackingLive() {
  if (!hasTrackingAuth() || !hasEnv(config.trackingDeliveryPersonId, config.trackingPackageReference)) {
    console.warn('Skipping tracking scenario: tracking env values are missing.');
    return;
  }

  runTrackingLiveForPackage({
    bearerToken: '',
    deliveryPersonId: config.trackingDeliveryPersonId,
    packageReference: config.trackingPackageReference,
    guestAccessToken: config.trackingGuestAccessToken,
  });
}

export default function trackingDefault() {
  runTrackingLive();
}
