<template>
  <div class="google-map">
    <!-- Google Maps intégré ici -->
    <div id="map">
    <iframe
          width="600"
          height="450"
          :src="googleMapPath"
          style="border:0;"
          allowfullscreen=""
          loading="lazy"
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
  mounted() {
    // Initialiser la carte Google Maps ici
    // Vous devrez inclure la bibliothèque Google Maps JavaScript API
  },
};
</script>

<style scoped>
.google-map {
  flex: 1;
  height: 400px; /* Ajustez la hauteur selon vos besoins */
}
</style>
