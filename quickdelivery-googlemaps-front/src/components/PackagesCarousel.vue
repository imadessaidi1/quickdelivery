<template>
    <!--<h2>{{$t('packagesArround')}}</h2>-->
    <carousel :items-to-show="1" @slide-start="handleSlideStart">
    <slide v-for="package_ in packagesList" :key="package_">
        <MarkerDetails ref="marker.addressString+index" :package_="package_" />
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
import axios from 'axios'

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
    await this.getUserLocation();
    await this.fetchData();
  },
  methods: {
    async getUserLocation() {
      if (!("geolocation" in navigator)) {
        console.log('Geolocation is not available.');
        return;
      }
      try {
        const pos = await new Promise((resolve, reject) => {
          navigator.geolocation.getCurrentPosition(resolve, reject);
        });
        this.location = pos;
      } catch (err) {
        console.log(err.message);
      }
    },
    async fetchData() {
      try {
        const url = this.$i18n.t('rootURL')+this.$i18n.t('getPackagesAroundMe')+this.location.coords.latitude+"&longitude="+this.location.coords.longitude+"&rayonEnMetres=30000";
        const response = await axios.get(url);
        this.packagesList = response.data;
        this.displayDirection(0);
      } catch (error) {
        console.error('Erreur lors de la requête API', error);
      }
    },
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
  },
}
</script>

<style>

</style>
