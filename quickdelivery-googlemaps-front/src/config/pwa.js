import { isMobileCapacitorRuntime } from './network';

async function clearBrowserServiceWorkers() {
  if (!('serviceWorker' in navigator)) {
    return;
  }

  try {
    const registrations = await navigator.serviceWorker.getRegistrations();
    await Promise.all(registrations.map((registration) => registration.unregister()));
  } catch (error) {
    console.warn('Service worker cleanup failed:', error);
  }

  if (!('caches' in window)) {
    return;
  }

  try {
    const cacheKeys = await window.caches.keys();
    await Promise.all(cacheKeys.map((cacheKey) => window.caches.delete(cacheKey)));
  } catch (error) {
    console.warn('Browser cache cleanup failed:', error);
  }
}

export async function registerServiceWorker() {
  if (!('serviceWorker' in navigator)) {
    return null;
  }

  if (!isMobileCapacitorRuntime()) {
    await clearBrowserServiceWorkers();
    return null;
  }

  try {
    const registration = await navigator.serviceWorker.register('/sw.js');
    return registration;
  } catch (error) {
    console.warn('Service worker registration failed:', error);
    return null;
  }
}
