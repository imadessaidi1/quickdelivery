<template>
  <div
    class="home-page"
    :class="{
      'mobile-map-mode': isMobile && mobileViewMode === 'map',
      'mobile-list-mode': isMobile && mobileViewMode === 'list',
    }"
  >
    <div v-if="isAroundMeLoading" class="around-loader">
      <div class="around-loader-card">
        <div class="around-loader-spinner"></div>
        <strong>{{ $t('stateLoading') }}</strong>
        <span>{{ $t('stateLoadingPackagesAround') }}</span>
      </div>
    </div>
    <div class="map-shell">
      <div v-if="!isMobile" class="map-search-toolbar">
        <div class="search-block">
          <div class="search-input-shell">
            <AddressAutoComplete
              v-model="searchAddress"
              class="search-address-input"
              :placeholder="$t('mapSearchPlaceholder')"
              @place-selected="handleSearchPlaceSelected"
              @keyup.enter="submitAddressSearch"
            />
          </div>
        </div>
        <div class="search-mode-group">
          <button class="search-mode-btn" :class="{ active: searchMode === 'direct' }" type="button" @click="searchMode = 'direct'">{{ $t('mapSearchModeDirect') }}</button>
          <button class="search-mode-btn" :class="{ active: searchMode === 'tour' }" type="button" @click="searchMode = 'tour'">{{ $t('mapSearchModeTour') }}</button>
        </div>
        <div class="radius-block">
          <label class="radius-label">{{ $t('mapSearchRadiusLabel') }} <strong>{{ $t('mapSearchRadiusValue', { n: radiusKm }) }}</strong></label>
          <input class="radius-slider" type="range" min="1" max="100" step="1" v-model.number="radiusKm" @input="updateRadiusOnly" />
        </div>
        <div class="toolbar-actions">
          <button class="toolbar-btn primary" type="button" @click="submitAddressSearch">{{ $t('actionSearch') }}</button>
        </div>
      </div>
      <GoogleMap ref="mapVue" :style="{ width: '100%', height: '100%' }" @map-iframe-loaded="handleMapIframeLoaded"/>
      <div class="map-legend">
        <div class="legend-title">{{ $t('mapLegendTitle') }}</div>
        <div class="legend-row">
          <span class="dot pickup"></span>
          <span>{{ $t('mapLegendPickup') }}</span>
        </div>
        <div class="legend-row">
          <span class="dot dropoff"></span>
          <span>{{ $t('mapLegendDropoff') }}</span>
        </div>
      </div>
    </div>
    <div class="packages-shell">
      <Carrousel
        @mobile-view-change="handleMobileViewChange"
        @loading-state-change="handleAroundMeLoadingState"
      />
    </div>
    <div v-if="isMobile" class="mobile-search-shell">
      <div class="map-search-toolbar mobile-search-toolbar">
        <div class="search-block">
          <div class="search-input-shell">
            <AddressAutoComplete
              v-model="searchAddress"
              class="search-address-input"
              :placeholder="$t('mapSearchPlaceholder')"
              @place-selected="handleSearchPlaceSelected"
              @keyup.enter="submitAddressSearch"
            />
          </div>
        </div>
        <div class="search-mode-group">
          <button class="search-mode-btn" :class="{ active: searchMode === 'direct' }" type="button" @click="searchMode = 'direct'">{{ $t('mapSearchModeDirect') }}</button>
          <button class="search-mode-btn" :class="{ active: searchMode === 'tour' }" type="button" @click="searchMode = 'tour'">{{ $t('mapSearchModeTour') }}</button>
        </div>
        <div class="radius-block">
          <label class="radius-label">{{ $t('mapSearchRadiusLabel') }} <strong>{{ $t('mapSearchRadiusValue', { n: radiusKm }) }}</strong></label>
          <input class="radius-slider" type="range" min="1" max="100" step="1" v-model.number="radiusKm" @input="updateRadiusOnly" />
        </div>
        <div class="toolbar-actions">
          <button class="toolbar-btn primary" type="button" @click="submitAddressSearch">{{ $t('actionSearch') }}</button>
        </div>
      </div>
    </div>
    <PackageDetailsModal ref="AppModal" classe="modal"/>
  </div>
</template>

<script>
import GoogleMap from '../components/GoogleMap.vue';
import Carrousel from '../components/PackagesCarousel.vue'
import PackageDetailsModal from "../components/PackageDetailsModal.vue";
import AddressAutoComplete from '../components/AddressAutocomplete.vue';
const GOOGLE_MAPS_KEY = process.env.VUE_APP_GOOGLE_MAPS_KEY || '';

export default {
  data() {
    return {
      mobileViewMode: 'map',
      isMobile: window.innerWidth < 768,
      searchAddress: '',
      searchMode: 'tour',
      isAroundMeLoading: true,
      hasCompletedInitialLoad: false,
      selectedSearchPlace: null,
      radiusKm: Math.max(1, Math.round((Number(this.$store.state.mapSearchRadius || 10000) / 1000))),
    };
  },
  mounted() {
    window.addEventListener('resize', this.handleResize);
    window.addEventListener('message', this.handleMapMessage);
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.handleResize);
    window.removeEventListener('message', this.handleMapMessage);
  },
  methods: {
    handleMapIframeLoaded() {
      window.dispatchEvent(new CustomEvent('qd-map-iframe-loaded'));
    },
    handleSearchPlaceSelected(place) {
      this.selectedSearchPlace = place?.latitude != null && place?.longitude != null ? place : null;
    },
    currentRadiusMeters() {
      return Math.max(1000, Math.round(Number(this.radiusKm || 1) * 1000));
    },
    updateRadiusOnly() {
      return this.currentRadiusMeters();
    },
    commitSearchRadius() {
      this.$store.commit('updateMapSearchRadius', this.currentRadiusMeters());
    },
    async geocodeSearchAddress(rawAddress) {
      const normalized = String(rawAddress || '').trim();
      if (!normalized || !GOOGLE_MAPS_KEY) {
        return null;
      }
      try {
        const response = await fetch(`https://maps.googleapis.com/maps/api/geocode/json?address=${encodeURIComponent(normalized)}&key=${encodeURIComponent(GOOGLE_MAPS_KEY)}`);
        if (!response.ok) {
          return null;
        }
        const payload = await response.json();
        const result = Array.isArray(payload?.results) ? payload.results[0] : null;
        const location = result?.geometry?.location;
        if (!location || typeof location.lat !== 'number' || typeof location.lng !== 'number') {
          return null;
        }
        return {
          rawAddress: result.formatted_address || normalized,
          latitude: location.lat,
          longitude: location.lng,
        };
      } catch (_error) {
        return null;
      }
    },
    async submitAddressSearch() {
      this.commitSearchRadius();
      let criteria = this.selectedSearchPlace && this.selectedSearchPlace.formattedAddress === this.searchAddress
        ? {
            rawAddress: this.selectedSearchPlace.formattedAddress,
            latitude: this.selectedSearchPlace.latitude,
            longitude: this.selectedSearchPlace.longitude,
          }
        : null;
      if (!criteria) {
        criteria = await this.geocodeSearchAddress(this.searchAddress);
      }
      if (!criteria) {
        return;
      }
      this.searchAddress = criteria.rawAddress;
      this.selectedSearchPlace = {
        formattedAddress: criteria.rawAddress,
        latitude: criteria.latitude,
        longitude: criteria.longitude,
      };
      window.dispatchEvent(new CustomEvent('qd-search-address', {
        detail: {
          mode: this.searchMode,
          criteria,
        },
      }));
    },
    async refreshAroundMe() {
      this.commitSearchRadius();
      const typedAddress = String(this.searchAddress || '').trim();
      if (typedAddress) {
        let criteria = this.selectedSearchPlace && this.selectedSearchPlace.formattedAddress === this.searchAddress
          ? {
              rawAddress: this.selectedSearchPlace.formattedAddress,
              latitude: this.selectedSearchPlace.latitude,
              longitude: this.selectedSearchPlace.longitude,
            }
          : null;
        if (!criteria) {
          criteria = await this.geocodeSearchAddress(typedAddress);
        }
        if (criteria) {
          this.searchMode = 'direct';
          this.searchAddress = criteria.rawAddress;
          this.selectedSearchPlace = {
            formattedAddress: criteria.rawAddress,
            latitude: criteria.latitude,
            longitude: criteria.longitude,
          };
          window.dispatchEvent(new CustomEvent('qd-search-address', {
            detail: {
              mode: 'direct',
              criteria,
            },
          }));
          return;
        }
      }
      this.selectedSearchPlace = null;
      window.dispatchEvent(new CustomEvent('qd-refresh-package-search', { detail: { mode: 'aroundMe' } }));
    },
    handleAroundMeLoadingState(isLoading) {
      const loading = Boolean(isLoading);
      if (!loading) {
        this.hasCompletedInitialLoad = true;
      }
      this.isAroundMeLoading = loading;
    },
    handleMobileViewChange(mode) {
      this.mobileViewMode = mode || 'map';
    },
    handleResize() {
      this.isMobile = window.innerWidth < 768;
    },
    handleMapMessage(event) {
      if (typeof event.data === 'string' && event.data.startsWith('SelectedPackage:')) {
        const packageId = event.data.split(':')[1];
        // Trigger Soft Lock
        const userId = this.$store.state.connectedUser?.id;
        if (userId) {
          window.dispatchEvent(new CustomEvent('qd-package-soft-lock', { 
            detail: { packageId: parseInt(packageId), userId } 
          }));
        }
        // Open Modal
        const pkg = this.$store.state.packagesArround?.find(p => p.id === parseInt(packageId));
        if (pkg && this.$refs.AppModal) {
          this.$refs.AppModal.openModalForPackage(pkg);
        }
      }
    },
  },
  components: {
    GoogleMap,
    Carrousel,
    PackageDetailsModal,
    AddressAutoComplete,
  },
};
</script>

<style>
.home-page {
  position: relative;
  display: grid;
  grid-template-columns: 1fr;
  gap: 14px;
  padding: 14px;
  width: 100%;
  height: calc(100% - 6px);
  min-height: 540px;
  box-sizing: border-box;
  background: var(--qd-bg);
  overflow-x: clip;
}

.around-loader {
  position: absolute;
  inset: 0;
  z-index: 8;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 18px;
  background: rgba(243, 244, 246, 0.72);
  backdrop-filter: blur(3px);
}

.around-loader-card {
  min-width: 220px;
  display: grid;
  justify-items: center;
  gap: 8px;
  padding: 18px 20px;
  border: 1px solid rgba(221, 227, 236, 0.9);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 16px 32px rgba(15, 23, 42, 0.12);
  text-align: center;
}

.around-loader-card strong {
  color: #0f172a;
  font-size: 15px;
}

.around-loader-card span {
  color: #64748b;
  font-size: 13px;
}

.around-loader-spinner {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  border: 3px solid var(--qd-primary-soft);
  border-top-color: var(--qd-primary);
  animation: around-loader-spin 0.8s linear infinite;
}

@keyframes around-loader-spin {
  to {
    transform: rotate(360deg);
  }
}

.map-shell {
  position: relative;
  min-height: 420px;
  border: 1px solid #dde3ec;
  border-radius: 16px;
  overflow: visible;
  background: #dfe6f3;
  box-shadow: 0 10px 25px rgba(21, 27, 39, 0.06);
  min-width: 0;
}

.map-search-toolbar {
  position: absolute;
  top: 20px;
  left: 20px;
  right: auto;
  width: min(600px, calc(100% - 40px));
  z-index: 100;
  display: flex;
  align-items: center;
  flex-wrap: nowrap;
  gap: 12px;
  padding: 10px 14px;
  border: 1px solid rgba(255, 255, 255, 0.4) !important;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.4) !important;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.12);
  backdrop-filter: blur(12px) saturate(160%);
  -webkit-backdrop-filter: blur(12px) saturate(160%);
  transition: all 0.3s ease;
}

.map-search-toolbar:focus-within {
  background: rgba(255, 255, 255, 0.7) !important;
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.18);
}

.search-block {
  flex: 1 1 320px;
  min-width: 0;
}

.search-input-shell {
  position: relative;
}

.search-address-input {
  width: 100%;
}

.search-address-input :deep(.address-autocomplete-input) {
  width: 100%;
  min-height: 48px;
  padding: 0 18px;
  border-radius: 16px;
  border: 2px solid rgba(215, 222, 234, 0.6);
  background: rgba(255, 255, 255, 0.8);
  font-size: 15px;
  font-weight: 600;
  box-sizing: border-box;
  transition: all 0.2s;
}

.search-address-input :deep(.address-autocomplete-input:focus) {
  border-color: var(--qd-primary);
  background: #ffffff;
}

.search-address-input :deep(.qd-place-autocomplete) {
  min-height: 44px;
  border-radius: 14px;
  border-color: #d7deea;
}

.search-mode-group {
  display: flex;
  gap: 6px;
  flex: 0 0 auto;
}

.radius-block {
  display: grid;
  gap: 4px;
  min-width: 0;
  width: 100%;
  flex: 1 1 180px;
}

.radius-label {
  font-size: 12px;
  color: #475569;
}

.radius-slider {
  -webkit-appearance: none;
  width: 100%;
  height: 8px;
  padding: 0;
  border: 0;
  border-radius: 10px;
  outline: none;
  background: #e2e8f0;
  cursor: pointer;
  accent-color: var(--qd-primary);
}

.radius-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 22px;
  height: 22px;
  background: var(--qd-primary);
  border: 3px solid #ffffff;
  border-radius: 50%;
  box-shadow: 0 4px 10px rgba(79, 70, 229, 0.3);
  transition: transform 0.2s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.radius-slider:active::-webkit-slider-thumb {
  transform: scale(1.2);
}

.radius-slider::-moz-range-track {
  height: 8px;
  border-radius: 10px;
  background: #e2e8f0;
}

.radius-slider::-moz-range-thumb {
  width: 22px;
  height: 22px;
  background: var(--qd-primary);
  border: 3px solid #ffffff;
  border-radius: 50%;
  box-shadow: 0 4px 10px rgba(79, 70, 229, 0.3);
}

.search-mode-btn {
  min-width: 90px;
  height: 46px;
  padding: 0 12px;
  border: 1.5px solid rgba(215, 222, 234, 0.6);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.7);
  color: #475569;
  font-size: 13px;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.02em;
  transition: all 0.2s;
  cursor: pointer;
}

.search-mode-btn.active {
  background: var(--qd-primary);
  border-color: var(--qd-primary);
  color: #ffffff;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.25);
}

.mission-btn.primary {
  background: #ffffff;
  color: #4f46e5;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.mission-btn.secondary {
  background: rgba(255, 255, 255, 0.15);
  color: #ffffff;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.mission-btn:hover {
  transform: translateY(-2px);
  filter: brightness(1.1);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.15);
}

.toolbar-actions {
  display: flex;
  gap: 8px;
  width: 100%;
  flex: 1 1 180px;
}

.toolbar-btn {
  height: 42px;
  padding: 0 14px;
  border-radius: 10px;
  font-weight: 700;
  border: 1px solid #d7deea;
}

.toolbar-btn.secondary {
  background: #ffffff;
  color: #1e293b;
}

.toolbar-btn.primary {
  background: var(--qd-primary);
  border-color: var(--qd-primary);
  color: #ffffff;
  height: 46px;
  padding: 0 20px;
  width: 100%;
  border-radius: 999px;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
  font-weight: 800;
  cursor: pointer;
  transition: all 0.2s;
}

.toolbar-btn.primary:hover {
  transform: translateY(-2px);
  filter: brightness(1.1);
  box-shadow: 0 6px 16px rgba(79, 70, 229, 0.4);
}

.packages-shell {
  min-height: 320px;
}

.mobile-search-shell {
  display: none;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  overflow-x: clip;
}

.map-legend {
  position: absolute;
  left: 16px;
  bottom: 16px;
  z-index: 3;
  min-width: 140px;
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid #e7e9ef;
  box-shadow: 0 10px 24px rgba(27, 38, 59, 0.08);
}

.legend-title {
  margin-bottom: 6px;
  font-size: 13px;
  font-weight: 700;
  color: #1f2937;
}

.legend-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
  font-size: 12px;
  color: #4b5563;
}

.legend-row:last-child {
  margin-bottom: 0;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.dot.pickup {
  background: #84cc16;
}

.dot.dropoff {
  background: #60a5fa;
}

.modal {
  z-index: 4;
}

/* Fix for Google Maps Autocomplete z-index and visibility */
.pac-container {
  z-index: 10000 !important;
  background-color: #ffffff !important;
  border-radius: 16px !important;
  margin-top: 8px !important;
  border: 1px solid #e2e8f0 !important;
  box-shadow: 0 15px 35px rgba(15, 23, 42, 0.15) !important;
  font-family: inherit !important;
}

.pac-item {
  padding: 12px 16px !important;
  cursor: pointer !important;
  border-top: 1px solid #f1f5f9 !important;
  display: flex !important;
  align-items: center !important;
  gap: 10px !important;
}

.pac-item:first-child {
  border-top: none !important;
}

.pac-item:hover {
  background-color: #f8fafc !important;
}

.pac-item-query {
  font-size: 14px !important;
  color: #0f172a !important;
  font-weight: 600 !important;
}

.pac-matched {
  color: #4f46e5 !important;
}

.pac-icon {
  margin-top: 0 !important;
}

@media screen and (min-width: 1024px) {
  .home-page {
    grid-template-columns: minmax(0, 1fr) minmax(480px, 36vw);
    min-height: 620px;
  }

  .packages-shell {
    height: 100%;
    min-width: 420px;
  }
}

@media screen and (min-width: 1440px) {
  .home-page {
    grid-template-columns: minmax(0, 1fr) minmax(460px, 31vw);
  }

  .packages-shell {
    min-width: 460px;
  }
}

@media screen and (max-width: 767px) {
  .home-page {
    min-height: calc(100dvh - 56px);
    padding: 8px;
    gap: 8px;
    width: 100%;
    max-width: 100%;
    overflow-x: hidden;
  }

  .around-loader {
    padding: 12px;
  }

  .around-loader-card {
    width: min(100%, 280px);
    padding: 16px;
    border-radius: 16px;
  }

  .map-shell {
    height: 50dvh;
    min-height: 300px;
    border-radius: 14px;
    width: 100%;
    max-width: 100%;
  }

  .map-search-toolbar {
    top: 10px;
    left: 10px;
    right: 10px;
    flex-wrap: wrap;
    padding: 8px;
  }

  .mobile-search-shell {
    display: block;
  }

  .mobile-search-toolbar {
    position: static;
    inset: auto;
    padding: 10px;
    border-radius: 16px;
    background: #ffffff;
    box-shadow: 0 8px 18px rgba(15, 23, 42, 0.06);
    width: 100%;
    max-width: 100%;
    min-width: 0;
    overflow-x: clip;
  }

  .search-block,
  .toolbar-actions,
  .search-mode-group,
  .radius-block {
    width: 100%;
  }

  .toolbar-actions {
    display: grid;
    grid-template-columns: 1fr;
  }

  .search-mode-group {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .search-mode-btn,
  .toolbar-btn {
    width: 100%;
  }

  .radius-block {
    gap: 6px;
    min-width: 0;
    flex: 1 1 auto;
  }

  .radius-slider {
    width: 100%;
    min-height: 0 !important;
    height: 8px !important;
    padding: 0 !important;
    border: 0 !important;
    border-radius: 10px !important;
    background: #e2e8f0 !important;
    display: block;
  }

  .radius-slider::-webkit-slider-runnable-track {
    height: 8px;
    border-radius: 10px;
    background: #e2e8f0;
  }

  .radius-slider::-webkit-slider-thumb {
    width: 22px;
    height: 22px;
    margin-top: -7px;
  }

  .radius-slider::-moz-range-track {
    height: 8px;
    border-radius: 10px;
    background: #e2e8f0;
  }

  .radius-slider::-moz-range-thumb {
    width: 22px;
    height: 22px;
    border: 3px solid #ffffff;
  }

  .packages-shell {
    width: 100%;
    max-width: 100%;
    min-width: 0;
    overflow-x: clip;
    margin-top: -2px;
  }

  .map-legend {
    left: 10px;
    bottom: 10px;
    padding: 8px 10px;
    min-width: 126px;
  }

  .legend-title {
    margin-bottom: 4px;
    font-size: 12px;
  }

  .legend-row {
    gap: 6px;
    font-size: 11px;
  }

  .home-page.mobile-map-mode .packages-shell {
    min-height: 0;
  }

  .home-page.mobile-map-mode .map-shell {
    height: 50dvh;
    min-height: 50dvh;
  }

  .home-page.mobile-list-mode .map-shell {
    display: none;
  }

  .home-page.mobile-list-mode .packages-shell {
    min-height: calc(100vh - 140px);
  }
}

@media screen and (max-width: 420px) {
  .map-shell {
    min-height: 340px;
  }

  .mobile-search-toolbar {
    padding: 8px;
  }

  .search-address-input :deep(.address-autocomplete-input),
  .search-address-input :deep(.qd-place-autocomplete) {
    min-height: 42px;
    font-size: 13px;
  }

  .search-mode-btn,
  .toolbar-btn {
    height: 40px;
    font-size: 12px;
  }
}

</style>
