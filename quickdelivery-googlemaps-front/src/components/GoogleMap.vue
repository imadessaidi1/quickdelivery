<template>
  <div class="google-map">
    <!-- Google Maps intégré ici -->
    <div id="map">
    <iframe
            ref="map"
            width="100%"
          height="100%"
          :src="googleMapPath"
          style="border:0;"
          v-on:load="onLoadIframe"
          name="map"
        ></iframe>
    </div>
  </div>
</template>

<script>
export default {
  data() {
      return {
        googleMapPath: process.env.BASE_URL + 'google-maps.html',
        // Assurez-vous de remplacer 'nom-du-fichier-dans-public.html' par le vrai nom de votre fichier
      };
    },
  methods: {
  sendMessage(){
    this.$refs.map.contentWindow.postMessage("message", "*");
    },
  },
};
</script>

<style scoped>
#map{
  height: 100%;
}
.google-map {
  width: 100%;
  height: 75vh;
  flex: 1;
}
</style>
