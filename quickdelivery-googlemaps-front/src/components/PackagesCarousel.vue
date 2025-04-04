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
import { Geolocation } from '@capacitor/geolocation';
import http from '@/config/httpInterceptor';

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
    const coordinates = await Geolocation.getCurrentPosition();
    const positionData = {
      actuallatitude: coordinates.coords.latitude,
      actuallongitude : coordinates.coords.longitude,
    };
    this.$parent.$refs.mapVue.$refs.map.contentWindow.postMessage(positionData, "*");
    var url = 'packages-around-me?latitude='+coordinates.coords.latitude+'&longitude='+coordinates.coords.longitude+'&rayonEnMetres=300000';
    const response = await http.get(this.$i18n.t('rootURL')+url);
    this.packagesList = response.data;
    this.$parent.$refs.mapVue.$refs.map.contentWindow.postMessage(response.data, "*");
    this.displayDirection(0);
    window.onmessage = (e) => {
        if (Array.isArray(e.data)) {
                console.log(e.data);
        }else if (typeof e.data === 'string' && e.data.includes('SelectedPackage:')) {
           const packageID = e.data.split(':')[1];
           const markerDetailComponent = this.$refs.markerDetail;
           //const markerDetailToSelect = markerDetailComponent.filter(markerDetail => markerDetail.package_.id+'' === packageID);
           markerDetailComponent.forEach(markerDetail => {
            if(markerDetail.package_.id+'' === packageID)
             if (markerDetail && markerDetail.setFocusOnReserveButton) {
                markerDetail.setFocusOnReserveButton();
             }
           });
        }else if (typeof e.data === 'string' && e.data === 'StartLoading') {
            this.$store.commit('updateLoaderStatus', true);
        }else if (typeof e.data === 'string' && e.data === 'EndLoading') {
            this.$store.commit('updateLoaderStatus', false);
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
