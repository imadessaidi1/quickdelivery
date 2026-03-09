<template>
  <div class="home-page">
    <GoogleMap ref="mapVue" :style="{ width: '100%', height: '100%' }"/>
    <div class="carousel-overlay">
      <Carrousel />
    </div>
    <PackageDetailsModal ref="AppModal" classe="modal"/>
  </div>
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
.home-page {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 540px;
}

.carousel-overlay {
  position: absolute;
  top: 8px;
  left: 0;
  width: 100%;
  height: 280px;
  z-index: 1;
  overflow: hidden;
  padding: 0 8px;
  box-sizing: border-box;
}

.modal {
  z-index: 3;
}

@media screen and (min-width: 768px) {
  .carousel-overlay {
    top: 12px;
    left: 12px;
    bottom: 12px;
    width: min(390px, 36vw);
    height: auto;
    padding: 0;
  }
}

@media screen and (max-width: 767px) {
  .home-page {
    min-height: 480px;
  }

  .carousel-overlay {
    top: 6px;
    height: 300px;
    padding: 0 6px;
  }
}
</style>
