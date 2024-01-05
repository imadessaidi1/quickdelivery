<template>
  <div class="main_content">
    <GoogleMap ref="mapVue" :style="{ width: '100%', height: '100%' }"/>
    <div class="carousel-overlay">
        <Carrousel />
    </div>
  </div>
</template>

<script>
import GoogleMap from '../components/GoogleMap.vue';
import Carrousel from '../components/PackagesCarousel.vue'

export default {
  components: {
    GoogleMap,
    Carrousel,
  },
};
</script>

<style>
.main_content {
  height: 87%;
  box-sizing: border-box;
}

.carousel-overlay {
  position: absolute;
  top: 58px;
  left: 0;
  width: 100%; /* Couvre toute la largeur de la carte */
  height: 31%; /* Ajustez la hauteur selon vos besoins */
  background: rgba(255, 255, 255, 0.3); /* Couleur de fond avec transparence */
  z-index: 1; /* Mettez un z-index plus élevé que la carte pour le superposer */
  overflow: hidden; /* Empêche le contenu de déborder */
}

</style>
