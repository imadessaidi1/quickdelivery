const { createApp } = require("vue");
import App from "./App.vue";
import i18n from './config/i18n';
import store from './config/store';
import router from './routers';
import validationCore from '@vuelidate/core';
import validations from '@vuelidate/validators';

const app = createApp(App);
app.use(i18n).use(router).use(validationCore).use(validations).use(store);
app.mount('#app');