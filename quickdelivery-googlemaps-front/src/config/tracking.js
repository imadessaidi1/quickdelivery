import http from './httpInterceptor';

export async function fetchTrackingPackage(reference, guestAccessToken = '', i18n) {
  if (!reference) {
    throw new Error('Missing package reference');
  }

  const encodedReference = encodeURIComponent(reference);
  if (guestAccessToken) {
    return http.get(
      i18n.t('rootURL') + i18n.t('getGuestPackage') + encodedReference + '&guestAccessToken=' + encodeURIComponent(guestAccessToken),
      { skipAuth: true }
    );
  }

  return http.get(i18n.t('rootURL') + i18n.t('getPackage') + encodedReference);
}
