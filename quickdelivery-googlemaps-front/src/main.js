const { createApp } = require("vue");
import App from "./App.vue";
import i18n from './config/i18n';
import store from './config/store';
import router from './routers';

// Create the app instance
const app = createApp(App);

// Use Vue plugins
app.use(i18n).use(router).use(store);

// Mount the app
app.mount('#app');
