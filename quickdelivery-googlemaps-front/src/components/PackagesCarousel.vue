<template>
    <carousel :items-to-show="1" @slide-start="handleSlideStart">
    <slide v-for="package_ in packagesList" :key="package_">
        <MarkerDetails ref="marker.addressString+index" :package_="package_" :mapVue="getMapVue()" :modal="getPackageModal()"/>
    </slide>

    <template #addons>
        <navigation />
    </template>
    </carousel>
</template>

<script>
// If you are using PurgeCSS, make sure to whitelist the carousel CSS classes
import 'vue3-carousel/dist/carousel.css'
import { Carousel, Slide, Navigation } from 'vue3-carousel'
import MarkerDetails from './MarkerDetails.vue';

export default {
  name: 'App',
  components: {
    Carousel,
    Slide,
    Navigation,
    MarkerDetails,
  },
  data() {
    return {
      packagesList:[] ,
      location: null,
    };
  },
  async mounted() {
    window.onmessage = (e) => {
        if (Array.isArray(e.data)) {
                const rawData = e.data;
                this.packagesList = JSON.parse(JSON.stringify(rawData));
                this.displayDirection(0);
        }
        if (typeof e.data === 'string' && e.data.includes('SelectedPackage:')) {
           const packageID = e.data.split(':')[1];
           const markerDetailComponent = this.$refs.markerDetail;
           //const markerDetailToSelect = markerDetailComponent.filter(markerDetail => markerDetail.package_.id+'' === packageID);
           markerDetailComponent.forEach(markerDetail => {
            if(markerDetail.package_.id+'' === packageID)
             if (markerDetail && markerDetail.setFocusOnReserveButton) {
                markerDetail.setFocusOnReserveButton();
             }
           });
        }
       };
  },
  methods: {
    handleSlideStart(data) {
      this.displayDirection(data.slidingToIndex);
    },
    displayDirection(index) {
      var stringDeparture = "";
      var stringArrival = "";
      this.packagesList[index].addresses.forEach(address => {
        if (address.type === "DEPARTURE") {
          stringDeparture = address.latitude + "," + address.longitude;
        } else {
          stringArrival = address.latitude + "," + address.longitude;
        }
      });
      this.$parent.$refs.mapVue.$refs.map.contentWindow.postMessage("SelectedDirection:" + stringDeparture + ";" + stringArrival, "*");
    },
    getMapVue(){
        return this.$parent.$refs.mapVue;
    },
    getPackageModal(){
        return this.$parent.$refs.AppModal;
    },
  },
}
</script>

<style>

</style>
