<template>
    <Carrousel />
    <div class="main-content">
        <!--<MarkerList :style="{ width: '30%' }"/>-->
        <GoogleMap ref="mapVue" :style="{ width: '60%' }"/>
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
}
</style>