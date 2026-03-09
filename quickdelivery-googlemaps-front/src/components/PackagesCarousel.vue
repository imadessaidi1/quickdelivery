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
//import { Geolocation } from '@capacitor/geolocation';
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
      coordinates: null,
      positionData: null,
    };
  },
  async mounted() {
    //this.coordinates = await Geolocation.getCurrentPosition();
    this.positionData = await this.getCurrentLocation();
    this.refreshPackagesList(this.positionData);
    window.onmessage = (e) => {
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
        }else if (typeof e.data === 'string' && e.data === 'StartLoading') {
            this.$store.commit('updateLoaderStatus', true);
        }else if (typeof e.data === 'string' && e.data === 'EndLoading') {
            this.$store.commit('updateLoaderStatus', false);
        }else if (typeof e.data === 'string' && e.data.includes('RefreshPackagesList')) {
            this.removePackageFromListe(e.data.split(' ')[1]);
        }
    };
  },
  methods: {
    handleSlideStart(data) {
      this.displayDirection(data.slidingToIndex);
    },
    displayDirection(index) {
      const selectedPackage = this.packagesList?.[index];
      if (!selectedPackage || !Array.isArray(selectedPackage.addresses) || selectedPackage.addresses.length === 0) {
        return;
      }
      let stringDeparture = "";
      let stringArrival = "";
      selectedPackage.addresses.forEach(address => {
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
    getCurrentLocation() {
        return new Promise((resolve, reject) => {
          if (navigator.geolocation) {
            navigator.geolocation.getCurrentPosition(
              (position) => {
                const positionData = {
                  actuallatitude: position.coords.latitude,
                  actuallongitude: position.coords.longitude,
                };
                resolve(positionData); // Résoudre la promesse avec les coordonnées
              },
              (error) => {
                reject('Unable to retrieve location');
                console.error('Unable to retrieve location', error);
              }
            );
          } else {
            reject('Geolocation not supported');
            console.error('Geolocation not supported');
          }
        });
      },
    async refreshPackagesList(positionData){
        try {
          this.$parent.$refs.mapVue.$refs.map.contentWindow.postMessage(JSON.stringify(this.positionData), "*");
          const url = 'packages-around-me?latitude='+positionData.actuallatitude+'&longitude='+positionData.actuallongitude+'&rayonEnMetres=300000';
          const response = await http.get(this.$i18n.t('rootURL')+url);
          this.packagesList = Array.isArray(response.data) ? response.data : [];
          this.$parent.$refs.mapVue.$refs.map.contentWindow.postMessage(JSON.stringify(this.packagesList), "*");
          if (this.packagesList.length > 0) {
            this.displayDirection(0);
          }
        } catch (error) {
          console.error('Unable to refresh packages list', error);
          this.packagesList = [];
        }
    },
    removePackageFromListe(packageId){
      const simpleList = JSON.parse(JSON.stringify(this.packagesList));
      const index = simpleList.findIndex(pkg => pkg.id === parseInt(packageId, 10));
      if (index !== -1) {
        simpleList.splice(index, 1); // Supprime 1 élément à l'index trouvé
      }
      this.packagesList = simpleList;
      this.$parent.$refs.mapVue.$refs.map.contentWindow.postMessage(JSON.stringify(simpleList), "*");
    },
  },
}
</script>

<style>

</style>
