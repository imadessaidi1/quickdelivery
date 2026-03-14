<template>
    <div class="carousel-wrapper">
    <div v-if="isMobile" class="mobile-mode-switch">
      <button class="mode-btn" :class="{ active: mobileViewMode === 'map' }" @click="mobileViewMode = 'map'">{{ $t('mobileModeMap') }}</button>
      <button class="mode-btn" :class="{ active: mobileViewMode === 'list' }" @click="mobileViewMode = 'list'">{{ $t('mobileModeList') }}</button>
    </div>
    <div v-if="isLoadingPackages" class="carousel-state">{{ $t('stateLoadingPackagesAround') }}</div>
    <div v-else-if="loadError" class="carousel-state error">{{ $t('stateLoadError') }}</div>
    <div v-else-if="packagesList.length === 0" class="carousel-state">{{ $t('stateEmptyPackagesAround') }}</div>
    <template v-else>
      <div v-if="!isMobile" class="packages-panel">
        <div class="panel-header">
          <div>
            <h3>{{ $t('availablePackagesTitle') }}</h3>
            <p>{{ $t('availablePackagesSubtitle') }}</p>
          </div>
          <div class="count-chip">{{ packagesList.length }}</div>
        </div>
        <div class="vertical-carousel">
          <button class="v-nav up" :disabled="desktopSlideIndex === 0" @click="goDesktop(-2)" aria-label="Previous package">
            <span class="material-symbols-outlined">expand_less</span>
          </button>
          <div class="vertical-slide-window">
            <div
              v-for="(package_, visibleIndex) in visibleDesktopPackages"
              :key="package_.id || visibleIndex"
              class="package-item"
              :class="{ selected: selectedPackageId === package_.id }"
              @click="displayDirection(desktopSlideIndex + visibleIndex, { preserveDesktopWindow: true })"
            >
              <MarkerDetails
                :package_="package_"
                :mapVue="getMapVue()"
                :modal="getPackageModal()"
                :isSelected="selectedPackageId === package_.id"
                :dense="true"
              />
            </div>
          </div>
          <button class="v-nav down" :disabled="desktopSlideIndex >= packagesList.length - 2" @click="goDesktop(2)" aria-label="Next package">
            <span class="material-symbols-outlined">expand_more</span>
          </button>
          <div class="v-counter">{{ desktopSlideIndex + 1 }}-{{ Math.min(desktopSlideIndex + visibleDesktopPackages.length, packagesList.length) }} / {{ packagesList.length }}</div>
        </div>
      </div>
      <template v-else>
        <div v-if="mobileViewMode === 'map'" class="mobile-map-card">
          <carousel
            v-if="packagesList.length"
            v-model="mobileMapSlideIndex"
            :items-to-show="1"
            snap-align="start"
            :wrap-around="false"
            :mouse-drag="true"
            :touch-drag="true"
            @slide-start="handleMobileMapSlideStart"
            class="mobile-map-carousel"
          >
            <slide v-for="(package_, index) in packagesList" :key="package_.id || index">
              <div class="mobile-map-item" :class="{ selected: selectedPackageId === package_.id }" @click="displayDirection(index)">
                <MarkerDetails
                  :package_="package_"
                  :mapVue="getMapVue()"
                  :modal="getPackageModal()"
                  :isSelected="selectedPackageId === package_.id"
                  :compact="true"
                />
              </div>
            </slide>
            <template #addons>
              <navigation />
            </template>
          </carousel>
          <div v-else class="carousel-state">{{ $t('stateEmptyPackagesAround') }}</div>
        </div>
        <div v-else class="mobile-list">
          <div
            v-for="(package_, index) in packagesList"
            :key="package_.id || index"
            class="package-item"
            :class="{ selected: selectedPackageId === package_.id }"
            @click="displayDirection(index)"
          >
            <MarkerDetails
              :package_="package_"
              :mapVue="getMapVue()"
              :modal="getPackageModal()"
              :isSelected="selectedPackageId === package_.id"
            />
          </div>
        </div>
      </template>
    </template>
    </div>
</template>

<script>
// If you are using PurgeCSS, make sure to whitelist the carousel CSS classes
import 'vue3-carousel/dist/carousel.css'
import { Carousel, Slide, Navigation } from 'vue3-carousel';
import MarkerDetails from './MarkerDetails.vue';
//import { Geolocation } from '@capacitor/geolocation';
import http from '@/config/httpInterceptor';

export default {
  name: 'App',
  emits: ['mobile-view-change'],
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
      searchRadius: this.$store.state.mapSearchRadius || 10000,
      isLoadingPackages: false,
      loadError: false,
      isMobile: window.innerWidth < 768,
      searchMode: 'aroundMe',
      addressCriteria: null,
      lastSearchedAddress: '',
      desktopSlideIndex: 0,
      mobileMapSlideIndex: 0,
      mobileViewMode: 'map',
    };
  },
  computed: {
    visibleDesktopPackages() {
      return this.packagesList.slice(this.desktopSlideIndex, this.desktopSlideIndex + 2);
    },
    selectedPackageForMobileMap() {
      if (!this.packagesList.length) {
        return null;
      }
      const byId = this.packagesList.find((pkg) => pkg.id === this.selectedPackageId);
      return byId || this.packagesList[0];
    },
  },
  watch: {
    mobileViewMode(newValue) {
      this.$emit('mobile-view-change', newValue);
    },
  },
  async mounted() {
    //this.coordinates = await Geolocation.getCurrentPosition();
    this.positionData = await this.getCurrentLocation();
    this.refreshPackagesList(this.positionData);
    this.$emit('mobile-view-change', this.mobileViewMode);
    window.addEventListener('resize', this.handleResize);
    window.addEventListener('qd-search-around-address', this.handleAddressSearch);
    window.addEventListener('qd-refresh-package-search', this.handleSearchRefresh);
    window.onmessage = (e) => {
        if (typeof e.data === 'string' && e.data.includes('SelectedPackage:')) {
           const packageID = e.data.split(':')[1];
           this.syncSelectedPackageFromMap(parseInt(packageID, 10));
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
    window.removeEventListener('qd-refresh-package-search', this.handleSearchRefresh);
  },
  methods: {
    handleResize() {
      this.isMobile = window.innerWidth < 768;
    },
    handleSlideStart(data) {
      this.displayDirection(data.slidingToIndex);
    },
    handleMobileMapSlideStart(data) {
      this.displayDirection(data.slidingToIndex);
    },
    syncSelectedPackageFromMap(packageId) {
      if (Number.isNaN(packageId)) {
        return;
      }
      const index = this.packagesList.findIndex((pkg) => pkg.id === packageId);
      if (index === -1) {
        this.selectedPackageId = packageId;
        return;
      }
      this.mobileViewMode = 'map';
      this.displayDirection(index);
    },
    goDesktop(delta) {
      if (!this.packagesList.length) {
        return;
      }
      const nextIndex = Math.min(this.packagesList.length - 1, Math.max(0, this.desktopSlideIndex + delta));
      if (nextIndex === this.desktopSlideIndex) {
        return;
      }
      this.desktopSlideIndex = nextIndex;
      this.displayDirection(nextIndex);
    },
    updateRadius(radius) {
      if (this.searchRadius === radius) {
        return;
      }
      this.searchRadius = radius;
      this.$store.commit('updateMapSearchRadius', radius);
      this.refreshWithCurrentRadius();
    },
    handleSearchRefresh(event) {
      const mode = event?.detail?.mode;
      if (mode === 'aroundMe') {
        this.refreshPackagesList(this.positionData);
        return;
      }
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
    displayDirection(index, options = {}) {
      const selectedPackage = this.packagesList?.[index];
      if (!selectedPackage || !Array.isArray(selectedPackage.addresses) || selectedPackage.addresses.length === 0) {
        return;
      }
      this.selectedPackageId = selectedPackage.id;
      if (!options.preserveDesktopWindow) {
        this.desktopSlideIndex = index;
      }
      this.mobileMapSlideIndex = index;
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
          const url = this.$i18n.t('rootURL')
            + this.$i18n.t('getPackagesAroundMe')
            + positionData.actuallatitude
            + '&longitude=' + positionData.actuallongitude
            + '&rayonEnMetres=' + this.searchRadius;
          const response = await http.get(url);
          this.packagesList = Array.isArray(response.data) ? response.data : [];
          this.onMyRoadPackageIds = [];
          this.selectedPackageId = this.packagesList.length > 0 ? this.packagesList[0].id : null;
          this.desktopSlideIndex = 0;
          this.mobileMapSlideIndex = 0;
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
        if (!this.positionData?.actuallatitude || !this.positionData?.actuallongitude) {
          this.positionData = await this.getCurrentLocation();
        }
        const params = new URLSearchParams({
          latitude: String(this.positionData.actuallatitude),
          longitude: String(this.positionData.actuallongitude),
          line1: criteria.line1 || '',
          zipCode: criteria.zipCode || '',
          town: criteria.town || '',
          country: criteria.country || '',
          rayonEnMetres: String(this.searchRadius),
        });
        const url = this.$i18n.t('rootURL') + this.$i18n.t('getPackagesAroundMeByDestination') + params.toString();
        const response = await http.get(url);
        this.packagesList = Array.isArray(response.data) ? response.data : [];
        this.onMyRoadPackageIds = [];
        this.selectedPackageId = this.packagesList.length > 0 ? this.packagesList[0].id : null;
        this.desktopSlideIndex = 0;
        this.mobileMapSlideIndex = 0;
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
      if (this.desktopSlideIndex >= this.packagesList.length) {
        this.desktopSlideIndex = Math.max(0, this.packagesList.length - 1);
      }
      if (this.mobileMapSlideIndex >= this.packagesList.length) {
        this.mobileMapSlideIndex = Math.max(0, this.packagesList.length - 1);
      }
      this.$parent.$refs.mapVue.$refs.map.contentWindow.postMessage(JSON.stringify(simpleList), "*");
    },
  },
}
</script>

<style>
.carousel-wrapper {
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 0;
  overflow-x: hidden;
}
.mobile-mode-switch {
  display: none;
}
.mode-btn {
  flex: 1;
  height: 34px;
  border: 1px solid #d9dfeb;
  border-radius: 10px;
  background: #ffffff;
  color: #334155;
  font-size: 12px;
  font-weight: 600;
}
.mode-btn.active {
  background: #0f172a;
  color: #ffffff;
  border-color: #0f172a;
}
.carousel-state {
  padding: 10px;
  border-radius: 8px;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  color: #334155;
  text-align: center;
  font-size: 12px;
}
.carousel-state.error {
  background: #fef2f2;
  border-color: #fecaca;
  color: #b91c1c;
}
.packages-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  border-radius: 14px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  overflow: hidden;
}
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 14px 14px 12px;
  border-bottom: 1px solid #ebedf2;
  background: #ffffff;
}
.panel-header h3 {
  margin: 0;
  font-weight: 700;
  font-size: 20px;
  color: #111827;
}
.panel-header p {
  margin: 4px 0 0;
  font-size: 13px;
  color: #64748b;
}
.count-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  height: 24px;
  padding: 0 8px;
  border-radius: 999px;
  background: #eef2ff;
  color: #334155;
  font-size: 12px;
  font-weight: 700;
}
.packages-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px;
  overflow-y: auto;
  min-height: 0;
}
.vertical-carousel {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 10px;
  height: 100%;
  min-height: 0;
}
.vertical-slide-window {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-rows: repeat(2, minmax(190px, 1fr));
  gap: 8px;
  align-content: start;
}
.v-nav {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 36px;
  border: 1px solid #d7deea;
  border-radius: 10px;
  background: #ffffff;
  color: #1e293b;
  cursor: pointer;
}
.v-nav:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.v-counter {
  text-align: center;
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
}
.package-item {
  border-radius: 14px;
  transition: box-shadow 0.2s ease;
  min-height: 0;
}
.package-item:hover {
  box-shadow: 0 6px 16px rgba(15, 23, 42, 0.06);
}
.package-item.selected {
  box-shadow: none;
}
.mobile-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-right: 2px;
}
.mobile-map-carousel {
  padding: 2px 0 4px;
  width: 100%;
  max-width: 100%;
}
.mobile-map-item {
  width: 100%;
  border-radius: 14px;
  padding-right: 0;
  min-width: 0;
}
.mobile-map-item.selected {
  box-shadow: 0 0 0 2px #93a7cf inset;
}
@media screen and (min-width: 768px) {
  .carousel-wrapper {
    height: 100%;
  }
}
@media screen and (max-width: 767px) {
  .carousel-wrapper {
    background: rgba(255, 255, 255, 0.84);
    border-radius: 12px;
    padding: 6px;
    border: 1px solid rgba(226, 232, 240, 0.8);
    box-shadow: 0 8px 20px rgba(15, 23, 42, 0.08);
  }
  .mobile-mode-switch {
    display: flex;
    gap: 8px;
    margin-bottom: 2px;
  }
  .mode-btn {
    height: 38px;
    border-radius: 12px;
    font-size: 13px;
  }
  .mobile-map-card {
    width: 100%;
    max-width: 100%;
    min-width: 0;
    overflow: hidden;
    box-sizing: border-box;
  }
  .mobile-map-carousel {
    width: 100%;
    max-width: 100%;
    min-width: 0;
    box-sizing: border-box;
    overflow: hidden;
  }
  :deep(.mobile-map-carousel .carousel) {
    width: 100%;
    max-width: 100%;
    min-width: 0;
    box-sizing: border-box;
  }
  :deep(.mobile-map-carousel .carousel__viewport) {
    width: 100%;
    max-width: 100%;
    min-width: 0;
    overflow: hidden;
  }
  :deep(.mobile-map-carousel .carousel__track) {
    min-width: 0;
    max-width: 100%;
  }
  :deep(.mobile-map-carousel .carousel__slide) {
    width: 100% !important;
    max-width: 100% !important;
    min-width: 0 !important;
    box-sizing: border-box;
    flex: 0 0 100%;
  }
  :deep(.mobile-map-carousel .carousel__slide) {
    padding: 0;
  }
  :deep(.mobile-map-carousel .carousel__prev),
  :deep(.mobile-map-carousel .carousel__next) {
    width: 28px;
    height: 28px;
    border-radius: 999px;
    background: rgba(15, 23, 42, 0.82);
    color: #ffffff;
    border: none;
  }
  :deep(.mobile-map-carousel .carousel__prev) {
    left: -2px;
  }
  :deep(.mobile-map-carousel .carousel__next) {
    right: -2px;
  }
  .carousel-state {
    margin-bottom: 0;
  }
}

@media screen and (max-width: 420px) {
  .carousel-wrapper {
    padding: 5px;
    border-radius: 10px;
  }

  .mobile-mode-switch {
    gap: 6px;
  }

  .mode-btn {
    height: 36px;
    font-size: 12px;
  }

  :deep(.mobile-map-carousel .carousel__prev),
  :deep(.mobile-map-carousel .carousel__next) {
    width: 26px;
    height: 26px;
  }
}
</style>
