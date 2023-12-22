const { createApp } = require("vue");
import App from "./App.vue";
import i18n from './config/i18n';

const app = createApp(App);
app.use(i18n);
app.mount('#app');