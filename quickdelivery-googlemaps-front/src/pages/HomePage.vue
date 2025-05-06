<template>
            <GoogleMap ref="mapVue" :style="{ width: '100%', height: '100%' }"/>
            <div class="carousel-overlay">
                <Carrousel />
            </div>
            <PackageDetailsModal ref="AppModal" classe="modal"/>
</template>

<script>
import GoogleMap from '../components/GoogleMap.vue';
import Carrousel from '../components/PackagesCarousel.vue'
import PackageDetailsModal from "../components/PackageDetailsModal.vue";

export default {
  components: {
    GoogleMap,
    Carrousel,
    PackageDetailsModal,
  },
};
</script>

<style>
.carousel-overlay {
  position: absolute;
  top: 7%;
  left: 0;
  width: 100%; /* Couvre toute la largeur de la carte */
  height: max-content; /* Ajustez la hauteur selon vos besoins */
  background: rgba(255, 255, 255, 0.3); /* Couleur de fond avec transparence */
  z-index: 1; /* Mettez un z-index plus élevé que la carte pour le superposer */
  overflow: hidden; /* Empêche le contenu de déborder */
}
.modal {
    z-index: 3;
}
</style>
