import http from 'k6/http';
import ws from 'k6/ws';
import { check, group, sleep } from 'k6';
import exec from 'k6/execution';
import { Counter, Trend } from 'k6/metrics';
import { authHeaders, config } from '../lib/config.js';
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

export function runTrackingLive() {
  if (!hasEnv(
    config.trackingBearerToken,
    config.trackingDeliveryPersonId,
    config.trackingPackageReference,
  )) {
    console.warn('Skipping tracking scenario: tracking env values are missing.');
    return;
  }

  const subscriptionMessage = JSON.stringify({
    type: 'TRACK_PACKAGE_SUBSCRIBE',
    from: `k6-subscriber-${exec.vu.idInTest}`,
    to: 'PACKAGE_SERVICE',
    packageReference: config.trackingPackageReference,
    guestAccessToken: config.trackingGuestAccessToken || null,
  });

  let sentAt = 0;
  let receivedCount = 0;

  const response = ws.connect(config.wsUrl, { tags: { flow: 'tracking-ws' } }, (socket) => {
    socket.on('open', () => {
      socket.send(subscriptionMessage);
    });

    socket.on('message', (raw) => {
      try {
        const payload = JSON.parse(raw);
        if (payload.type === 'PACKAGE_POSITION_UPDATE' && payload.packageReference === config.trackingPackageReference) {
          receivedCount += 1;
          trackingMessagesReceived.add(1);
          if (sentAt > 0) {
            trackingPropagationTrend.add(Date.now() - sentAt);
          }
        }
      } catch (_) {
        // Ignore non-JSON frames.
      }
    });

    socket.setTimeout(() => {
      positionSequence().forEach((position, index) => {
        group(`tracking-http-update-${index + 1}`, () => {
          sentAt = Date.now();
          const publish = http.post(
            `${config.baseUrl}/packages/v1/tracking/position?deliveryPersonId=${encodeURIComponent(config.trackingDeliveryPersonId)}`,
            JSON.stringify(position),
            {
              headers: {
                ...authHeaders(config.trackingBearerToken),
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
      socket.setTimeout(() => socket.close(), 3000);
    }, 800);
  });

  check(response, {
    'tracking websocket handshake ok': (res) => res && res.status === 101,
    'tracking websocket received updates': () => receivedCount > 0,
  }, { flow: 'tracking-ws' });
}

export default function trackingDefault() {
  runTrackingLive();
}
