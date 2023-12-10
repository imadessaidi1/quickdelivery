const { createApp } = require("vue");
import App from "./App.vue";
import VueGoogleMaps from "@fawmi/vue-google-maps";

const app = createApp(App)
  .use(VueGoogleMaps, {
    load: {
      key: "AIzaSyCBZNoGNPCcqSsTYTlOUhkXLhb47HqF9mM"
    }
  });
app.component('VueGoogleMaps', VueGoogleMaps);
app.mount('#app');