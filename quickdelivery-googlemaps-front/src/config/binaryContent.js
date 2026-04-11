import { getAccessToken } from './auth';

export function blobToDataUrl(blob) {
  return new Promise((resolve, reject) => {
    if (!(blob instanceof Blob)) {
      reject(new Error('Expected a Blob instance.'));
      return;
    }

    const reader = new FileReader();
    reader.onloadend = () => {
      if (typeof reader.result === 'string') {
        resolve(reader.result);
        return;
      }
      reject(new Error('Unable to convert blob to data URL.'));
    };
    reader.onerror = () => {
      reject(reader.error || new Error('Unable to read blob.'));
    };
    reader.readAsDataURL(blob);
  });
}

export async function fetchProtectedBlob(url, options = {}) {
  const headers = new Headers(options.headers || {});
  const token = getAccessToken();

  if (token && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const response = await fetch(url, {
    method: 'GET',
    headers,
    cache: 'no-store',
  });

  if (!response.ok) {
    throw new Error(`Blob request failed with status ${response.status}`);
  }

  return response.blob();
}
