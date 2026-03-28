const DEFAULT_BASE_URL = __ENV.BASE_URL || 'https://api.quickdelivery.fr';
const DEFAULT_WS_URL = __ENV.WS_URL || `${DEFAULT_BASE_URL.replace(/^http/i, 'ws')}/ws`;

export const config = {
  baseUrl: DEFAULT_BASE_URL.replace(/\/$/, ''),
  wsUrl: DEFAULT_WS_URL,
  locale: __ENV.DEFAULT_LOCALE || 'fr',
  insecureSkipTLSVerify: String(__ENV.INSECURE_SKIP_TLS_VERIFY || 'false').toLowerCase() === 'true',
  enableCourierLifecycleSmoke: String(__ENV.ENABLE_COURIER_LIFECYCLE_SMOKE || 'false').toLowerCase() === 'true',
  adminBearerToken: __ENV.ADMIN_BEARER_TOKEN || '',
  courierBearerToken: __ENV.COURIER_BEARER_TOKEN || '',
  courierId: __ENV.COURIER_ID || '',
  courierPackageId: __ENV.COURIER_PACKAGE_ID || '',
  courierPickupOtp: __ENV.COURIER_PICKUP_OTP || '',
  courierDeliveryOtp: __ENV.COURIER_DELIVERY_OTP || '',
  trackingBearerToken: __ENV.TRACKING_BEARER_TOKEN || '',
  trackingDeliveryPersonId: __ENV.TRACKING_DELIVERY_PERSON_ID || '',
  trackingPackageReference: __ENV.TRACKING_PACKAGE_REFERENCE || '',
  trackingGuestAccessToken: __ENV.TRACKING_GUEST_ACCESS_TOKEN || '',
};

export function authHeaders(token) {
  if (!token) {
    return {};
  }
  return {
    Authorization: `Bearer ${token}`,
  };
}
