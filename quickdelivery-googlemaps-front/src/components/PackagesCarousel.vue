<template>
    <div class="carousel-wrapper">
    <carousel :items-to-show="1" @slide-start="handleSlideStart">
    <slide v-for="package_ in packagesList" :key="package_">
        <MarkerDetails
          :package_="package_"
          :mapVue="getMapVue()"
          :modal="getPackageModal()"
          @on-my-road-selected="onMyRoadSelected"
          @reserve-on-my-road="reserveOnMyRoad"
        />
    </slide>

    <template #addons>
        <navigation />
    </template>
    </carousel>
    <div v-if="canUseBulkReserve() && selectedPackageId" class="on-road-action-bar">
      <button class="btn confirm_btn" @click="reserveOnMyRoad(selectedPackageId)">
        {{ $t('packagesArroundMArkerDetailActionsReserveOnMyRoad') }}
      </button>
    </div>
    </div>
</template>

<script>
// If you are using PurgeCSS, make sure to whitelist the carousel CSS classes
import 'vue3-carousel/dist/carousel.css'
import { Carousel, Slide, Navigation } from 'vue3-carousel'
import MarkerDetails from './MarkerDetails.vue';
//import { Geolocation } from '@capacitor/geolocation';
import http from '@/config/httpInterceptor';
import { getCurrentUserRoles } from '@/config/auth';

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
      selectedPackageId: null,
      onMyRoadPackageIds: [],
    };
  },
  async mounted() {
    //this.coordinates = await Geolocation.getCurrentPosition();
    this.positionData = await this.getCurrentLocation();
    this.refreshPackagesList(this.positionData);
    window.onmessage = (e) => {
        if (typeof e.data === 'string' && e.data.includes('SelectedPackage:')) {
           const packageID = e.data.split(':')[1];
           this.selectedPackageId = parseInt(packageID, 10);
        } else if (typeof e.data === 'string' && e.data.startsWith('OnMyRoadPackages:')) {
            const raw = e.data.substring('OnMyRoadPackages:'.length);
            try {
              const ids = JSON.parse(raw);
              this.onMyRoadPackageIds = Array.isArray(ids) ? ids.map((id) => parseInt(id, 10)).filter((id) => !Number.isNaN(id)) : [];
            } catch (_ignored) {
              this.onMyRoadPackageIds = [];
            }
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
    onMyRoadSelected(packageId) {
      this.selectedPackageId = packageId;
    },
    canUseBulkReserve() {
      const roles = getCurrentUserRoles();
      return roles.includes('ROLE_LIVREUR') || roles.includes('ROLE_ADMIN');
    },
    async reserveOnMyRoad(packageId) {
      if (!this.canUseBulkReserve()) {
        return;
      }
      const selectedId = packageId || this.selectedPackageId;
      if (!selectedId) {
        return;
      }
      const combined = new Set([selectedId, ...this.onMyRoadPackageIds]);
      const packageIds = Array.from(combined);
      if (packageIds.length === 0) {
        return;
      }
      const userLanguage = navigator.languages && navigator.languages.length ? navigator.languages[0] : navigator.language || 'fr-FR';
      const url = `${this.$i18n.t('rootURL')}${this.$i18n.t('reserveBatchPackageUrl')}?deliveryPersonID=${this.$store.state.connectedUser.id}&locale=${userLanguage}`;
      try {
        const response = await http.put(url, packageIds);
        const reservedIds = response?.data?.reservedPackageIds || [];
        if (Array.isArray(reservedIds) && reservedIds.length > 0) {
          const reservedSet = new Set(reservedIds.map((id) => parseInt(id, 10)));
          this.packagesList = this.packagesList.filter((pkg) => !reservedSet.has(pkg.id));
          this.onMyRoadPackageIds = this.onMyRoadPackageIds.filter((id) => !reservedSet.has(id));
          this.$parent.$refs.mapVue.$refs.map.contentWindow.postMessage(JSON.stringify(this.packagesList), "*");
          if (this.packagesList.length > 0) {
            this.displayDirection(0);
          }
        }
      } catch (error) {
        console.error('Bulk reserve failed', error);
      }
    },
    displayDirection(index) {
      const selectedPackage = this.packagesList?.[index];
      if (!selectedPackage || !Array.isArray(selectedPackage.addresses) || selectedPackage.addresses.length === 0) {
        return;
      }
      this.selectedPackageId = selectedPackage.id;
      this.onMyRoadPackageIds = [];
      this.$parent.$refs.mapVue.$refs.map.contentWindow.postMessage(`SelectedPackage:${selectedPackage.id}`, "*");
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
          this.onMyRoadPackageIds = [];
          this.selectedPackageId = this.packagesList.length > 0 ? this.packagesList[0].id : null;
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
      if (this.selectedPackageId === parseInt(packageId, 10)) {
        this.selectedPackageId = this.packagesList.length > 0 ? this.packagesList[0].id : null;
      }
      this.$parent.$refs.mapVue.$refs.map.contentWindow.postMessage(JSON.stringify(simpleList), "*");
    },
  },
}
</script>

<style>
.carousel-wrapper {
  width: 100%;
}

.on-road-action-bar {
  display: flex;
  justify-content: center;
  margin-top: 8px;
}
</style>
