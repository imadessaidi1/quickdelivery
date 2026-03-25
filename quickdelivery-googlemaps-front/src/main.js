import { createApp } from 'vue';
import App from './App.vue';
import i18n from './config/i18n';
import store from './config/store';
import router from './routers';
import './styles/design-system.css';
import { handleAuthCallback, initializeMobileAuthCallbackListener } from './config/auth';
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
  app.mount('#app');
}

bootstrap();
