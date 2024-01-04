<template>
            <GoogleMap ref="mapVue" :style="{ width: '100%', height: '90vh' }"/>
            <div class="carousel-overlay">
                <Carrousel />
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
.main-content {
  display: flex;
  position: relative;
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
