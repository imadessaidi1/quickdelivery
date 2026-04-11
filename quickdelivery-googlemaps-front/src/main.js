import { createApp } from 'vue';
import App from './App.vue';
import i18n from './config/i18n';
import store from './config/store';
import router from './routers';
import './styles/design-system.css';
import {
  consumePendingPostAuthRoute,
  handleAuthCallback,
  hasValidAccessToken,
  initializeMobileAuthCallbackListener,
  resolveLandingPathForCurrentUser,
} from './config/auth';
import { ensureMobileBackendHostConfigured } from './config/network';
import { hydrateConnectedUser } from './config/session';
import { registerServiceWorker } from './config/pwa';

async function bootstrap() {
  ensureMobileBackendHostConfigured();
  await registerServiceWorker();
  await initializeMobileAuthCallbackListener();

  try {
    const callbackResult = await handleAuthCallback();
    if (callbackResult?.redirected) {
      return;
    }
  } catch (e) {
    console.error('OIDC callback processing failed:', e);
  }

  try {
    await hydrateConnectedUser();
  } catch (e) {
    console.error('Connected user hydration failed:', e);
  }

  const app = createApp(App);
  app.use(i18n).use(router).use(store);
  window.addEventListener('qd-auth-route-resolved', async (event) => {
    const targetPath = event?.detail?.path || resolveLandingPathForCurrentUser();
    try {
      await router.replace(targetPath);
    } catch (e) {
      console.error('Mobile auth route sync failed:', e);
    }
  });
  app.mount('#app');
  await router.isReady();
  const pendingPostAuthRoute = consumePendingPostAuthRoute();
  if (pendingPostAuthRoute) {
    try {
      await router.replace(pendingPostAuthRoute);
      return;
    } catch (e) {
      console.error('Pending post-auth route sync failed:', e);
    }
  }
  if (hasValidAccessToken()) {
    const currentRouteName = router.currentRoute.value?.name;
    if (currentRouteName === 'landingPage' || currentRouteName === 'loginPage' || currentRouteName === 'publicRegisterPage') {
      try {
        await router.replace(resolveLandingPathForCurrentUser());
      } catch (e) {
        console.error('Initial auth route sync failed:', e);
      }
    }
  }
}

bootstrap();
