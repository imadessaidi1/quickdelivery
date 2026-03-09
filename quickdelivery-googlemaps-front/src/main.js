import { createApp } from 'vue';
import App from './App.vue';
import i18n from './config/i18n';
import store from './config/store';
import router from './routers';
import { handleAuthCallback } from './config/auth';

async function bootstrap() {
  try {
    await handleAuthCallback();
  } catch (e) {
    console.error('OIDC callback processing failed:', e);
  }

  const app = createApp(App);
  app.use(i18n).use(router).use(store);
  app.mount('#app');
}

bootstrap();
