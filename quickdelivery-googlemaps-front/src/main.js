const { createApp } = require("vue");
import App from "./App.vue";
import i18n from './config/i18n';
import store from './config/store';
import router from './routers';

const app = createApp(App);
app.use(i18n).use(router).use(store);
app.mount('#app');