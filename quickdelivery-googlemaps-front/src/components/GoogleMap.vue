<template>
  <div class="google-map">
    <!-- Google Maps intégré ici -->
    <div id="map">
    <iframe
          width="600"
          height="450"
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

  },
};
</script>

<style scoped>
.google-map {
  flex: 1;
  height: 400px; /* Ajustez la hauteur selon vos besoins */
}
</style>
