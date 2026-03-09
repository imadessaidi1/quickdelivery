<template>
    <div class="carousel-wrapper">
    <div v-if="!isMobile" class="carousel-toolbar">
      <div class="radius-group">
        <button class="btn radius-btn" :class="{ active: searchRadius === 30000 }" @click="updateRadius(30000)">30km</button>
        <button class="btn radius-btn" :class="{ active: searchRadius === 100000 }" @click="updateRadius(100000)">100km</button>
        <button class="btn radius-btn" :class="{ active: searchRadius === 300000 }" @click="updateRadius(300000)">300km</button>
      </div>
      <button class="btn primary_btn" @click="refreshWithCurrentRadius">{{ $t('actionRefresh') }}</button>
    </div>
    <div v-if="isLoadingPackages" class="carousel-state">{{ $t('stateLoadingPackagesAround') }}</div>
    <div v-else-if="loadError" class="carousel-state error">{{ $t('stateLoadError') }}</div>
    <div v-else-if="packagesList.length === 0" class="carousel-state">{{ $t('stateEmptyPackagesAround') }}</div>
    <carousel
      :items-to-show="1"
      @slide-start="handleSlideStart"
    >
      <slide v-for="package_ in packagesList" :key="package_">
          <MarkerDetails
            :package_="package_"
            :mapVue="getMapVue()"
            :modal="getPackageModal()"
          />
      </slide>

      <template #addons>
          <navigation />
      </template>
    </carousel>
    </div>
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
      selectedPackageId: null,
      onMyRoadPackageIds: [],
      searchRadius: this.$store.state.mapSearchRadius || 30000,
      isLoadingPackages: false,
      loadError: false,
      isMobile: window.innerWidth < 768,
      searchMode: 'aroundMe',
      addressCriteria: null,
      lastSearchedAddress: '',
    };
  },
  async mounted() {
    //this.coordinates = await Geolocation.getCurrentPosition();
    this.positionData = await this.getCurrentLocation();
    this.refreshPackagesList(this.positionData);
    window.addEventListener('resize', this.handleResize);
    window.addEventListener('qd-search-around-address', this.handleAddressSearch);
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
  beforeUnmount() {
    window.removeEventListener('resize', this.handleResize);
    window.removeEventListener('qd-search-around-address', this.handleAddressSearch);
  },
  methods: {
    handleResize() {
      this.isMobile = window.innerWidth < 768;
    },
    handleSlideStart(data) {
      this.displayDirection(data.slidingToIndex);
    },
    updateRadius(radius) {
      if (this.searchRadius === radius) {
        return;
      }
      this.searchRadius = radius;
      this.$store.commit('updateMapSearchRadius', radius);
      this.refreshWithCurrentRadius();
    },
    refreshWithCurrentRadius() {
      if (this.searchMode === 'aroundAddress' && this.addressCriteria) {
        this.refreshPackagesListAroundAddress(this.addressCriteria);
      } else if (this.positionData) {
        this.refreshPackagesList(this.positionData);
      }
    },
    handleAddressSearch(event) {
      const criteria = event?.detail;
      if (!criteria) {
        return;
      }
      this.searchMode = 'aroundAddress';
      this.addressCriteria = criteria;
      this.lastSearchedAddress = criteria.rawAddress || '';
      this.refreshPackagesListAroundAddress(criteria);
    },
    async reserveOnMyRoad(packageId) {
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
        this.isLoadingPackages = true;
        this.loadError = false;
        this.searchMode = 'aroundMe';
        this.addressCriteria = null;
        try {
          this.$parent.$refs.mapVue.$refs.map.contentWindow.postMessage(JSON.stringify(this.positionData), "*");
          const url = 'packages-around-me?latitude='+positionData.actuallatitude+'&longitude='+positionData.actuallongitude+'&rayonEnMetres='+this.searchRadius;
          const response = await http.get(this.$i18n.t('rootURL')+url);
          this.packagesList = Array.isArray(response.data) ? response.data : [];
          this.onMyRoadPackageIds = [];
          this.selectedPackageId = this.packagesList.length > 0 ? this.packagesList[0].id : null;
          this.$parent.$refs.mapVue.$refs.map.contentWindow.postMessage(JSON.stringify(this.packagesList), "*");
          if (this.packagesList.length > 0) {
            this.displayDirection(0);
          }
        } catch (error) {
          this.loadError = true;
          console.error('Unable to refresh packages list', error);
          this.packagesList = [];
        } finally {
          this.isLoadingPackages = false;
        }
    },
    async refreshPackagesListAroundAddress(criteria) {
      this.isLoadingPackages = true;
      this.loadError = false;
      try {
        const params = new URLSearchParams({
          line1: criteria.line1 || '',
          zipCode: criteria.zipCode || '',
          town: criteria.town || '',
          country: criteria.country || '',
          rayonEnMetres: String(this.searchRadius),
        });
        const url = this.$i18n.t('rootURL') + this.$i18n.t('getPackagesAroundAddress') + params.toString();
        const response = await http.get(url);
        if (Array.isArray(response.data)) {
          this.packagesList = response.data;
        } else if (response.data && typeof response.data === 'object') {
          this.packagesList = Object.values(response.data)
            .filter((entry) => Array.isArray(entry))
            .flat();
        } else {
          this.packagesList = [];
        }
        this.onMyRoadPackageIds = [];
        this.selectedPackageId = this.packagesList.length > 0 ? this.packagesList[0].id : null;
        this.$parent.$refs.mapVue.$refs.map.contentWindow.postMessage(JSON.stringify(this.packagesList), "*");
        if (this.lastSearchedAddress) {
          this.$parent.$refs.mapVue.$refs.map.contentWindow.postMessage(JSON.stringify({ centerAddress: this.lastSearchedAddress }), "*");
        }
        if (this.packagesList.length > 0) {
          this.displayDirection(0);
        }
      } catch (error) {
        this.loadError = true;
        console.error('Unable to refresh packages list around address', error);
        this.packagesList = [];
      } finally {
        this.isLoadingPackages = false;
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
  padding-top: 2px;
  box-sizing: border-box;
  height: 100%;
}
.carousel-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  padding: 6px 8px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(2px);
}
.radius-group {
  display: flex;
  gap: 6px;
}
.radius-btn {
  border: solid 1px #9db5d3;
  background: #eef3fa;
  color: #365073;
}
.radius-btn.active {
  background: #d9e9ff;
  border-color: #6698d4;
  font-weight: 700;
}
.carousel-state {
  margin: 0 8px 8px 8px;
  padding: 8px;
  border-radius: 8px;
  background: #eef3f9;
  color: #3a4b5f;
  text-align: center;
  font-size: 13px;
}
.carousel-state.error {
  background: #fcecee;
  color: #b1354b;
}
@media screen and (min-width: 768px) {
  .carousel-wrapper {
    display: flex;
    flex-direction: column;
    padding: 10px;
    border-radius: 14px;
    background: transparent;
    border: none;
    box-shadow: none;
  }
  .carousel-toolbar {
    background: transparent;
    padding: 0;
  }
  .carousel-state {
    margin: 0 0 8px 0;
  }
}
@media screen and (max-width: 580px) {
  .carousel-toolbar {
    flex-wrap: wrap;
  }
  .radius-group {
    width: 100%;
    justify-content: space-between;
  }
  .radius-btn {
    flex: 1;
  }
}
</style>
