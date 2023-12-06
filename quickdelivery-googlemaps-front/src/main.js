const { createApp } = require("vue");
import App from "./App.vue";
import VueGoogleMaps from "@fawmi/vue-google-maps";

createApp(App)
  .use(VueGoogleMaps, {
    load: {
      key: "AIzaSyCBZNoGNPCcqSsTYTlOUhkXLhb47HqF9mM"
    }
  })
  .mount("#app");