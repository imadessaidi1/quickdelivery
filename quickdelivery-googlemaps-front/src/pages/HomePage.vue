<template>
  <div
    class="home-page"
    :class="{
      'mobile-map-mode': isMobile && mobileViewMode === 'map',
      'mobile-list-mode': isMobile && mobileViewMode === 'list',
    }"
  >
    <div class="map-shell">
      <div v-if="!isMobile" class="map-search-toolbar">
        <div class="search-block">
          <div class="search-input-shell">
            <span class="material-symbols-outlined search-icon">search</span>
            <AddressAutoComplete
              v-model="searchAddress"
              class="search-address-input"
              :placeholder="$t('mapSearchPlaceholder')"
              @keyup.enter="submitAddressSearch"
            />
          </div>
        </div>
        <div class="radius-group">
          <button class="radius-btn" :class="{ active: searchRadius === 10000 }" type="button" @click="updateRadius(10000)">10km</button>
          <button class="radius-btn" :class="{ active: searchRadius === 20000 }" type="button" @click="updateRadius(20000)">20km</button>
          <button class="radius-btn" :class="{ active: searchRadius === 30000 }" type="button" @click="updateRadius(30000)">30km</button>
        </div>
        <div class="toolbar-actions">
          <button class="toolbar-btn secondary" type="button" @click="submitAddressSearch">{{ $t('actionSearch') }}</button>
          <button class="toolbar-btn primary" type="button" @click="refreshAroundMe">{{ $t('mapSearchAroundMeAction') }}</button>
        </div>
      </div>
      <GoogleMap ref="mapVue" :style="{ width: '100%', height: '100%' }"/>
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
      <Carrousel @mobile-view-change="handleMobileViewChange" />
    </div>
    <div v-if="isMobile" class="mobile-search-shell">
      <div class="map-search-toolbar mobile-search-toolbar">
        <div class="search-block">
          <div class="search-input-shell">
            <span class="material-symbols-outlined search-icon">search</span>
            <AddressAutoComplete
              v-model="searchAddress"
              class="search-address-input"
              :placeholder="$t('mapSearchPlaceholder')"
              @keyup.enter="submitAddressSearch"
            />
          </div>
        </div>
        <div class="radius-group">
          <button class="radius-btn" :class="{ active: searchRadius === 10000 }" type="button" @click="updateRadius(10000)">10km</button>
          <button class="radius-btn" :class="{ active: searchRadius === 20000 }" type="button" @click="updateRadius(20000)">20km</button>
          <button class="radius-btn" :class="{ active: searchRadius === 30000 }" type="button" @click="updateRadius(30000)">30km</button>
        </div>
        <div class="toolbar-actions">
          <button class="toolbar-btn secondary" type="button" @click="submitAddressSearch">{{ $t('actionSearch') }}</button>
          <button class="toolbar-btn primary" type="button" @click="refreshAroundMe">{{ $t('mapSearchAroundMeAction') }}</button>
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

export default {
  data() {
    return {
      mobileViewMode: 'map',
      isMobile: window.innerWidth < 768,
      searchAddress: '',
      searchRadius: this.$store.state.mapSearchRadius || 10000,
    };
  },
  mounted() {
    window.addEventListener('resize', this.handleResize);
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.handleResize);
  },
  methods: {
    parseAddressCriteria(rawAddress) {
      const normalized = (rawAddress || '').trim();
      if (!normalized) {
        return null;
      }

      const chunks = normalized.split(',').map((value) => value.trim()).filter(Boolean);
      const postalTown = chunks[1] || '';
      const firstSpaceIndex = postalTown.indexOf(' ');

      return {
        rawAddress: normalized,
        line1: chunks[0] || '',
        zipCode: firstSpaceIndex === -1 ? '' : postalTown.slice(0, firstSpaceIndex).trim(),
        town: firstSpaceIndex === -1 ? postalTown : postalTown.slice(firstSpaceIndex + 1).trim(),
        country: chunks[2] || '',
      };
    },
    submitAddressSearch() {
      const criteria = this.parseAddressCriteria(this.searchAddress);
      if (!criteria) {
        return;
      }
      window.dispatchEvent(new CustomEvent('qd-search-around-address', { detail: criteria }));
    },
    updateRadius(radius) {
      if (this.searchRadius === radius) {
        return;
      }
      this.searchRadius = radius;
      this.$store.commit('updateMapSearchRadius', radius);
      if (this.searchAddress?.trim()) {
        this.submitAddressSearch();
        return;
      }
      this.refreshAroundMe();
    },
    refreshAroundMe() {
      window.dispatchEvent(new CustomEvent('qd-refresh-package-search', { detail: { mode: 'aroundMe' } }));
    },
    handleMobileViewChange(mode) {
      this.mobileViewMode = mode || 'map';
    },
    handleResize() {
      this.isMobile = window.innerWidth < 768;
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
  display: grid;
  grid-template-columns: 1fr;
  gap: 14px;
  padding: 14px;
  width: 100%;
  height: calc(100% - 6px);
  min-height: 540px;
  box-sizing: border-box;
  background: #f3f4f6;
}

.map-shell {
  position: relative;
  min-height: 420px;
  border: 1px solid #dde3ec;
  border-radius: 16px;
  overflow: hidden;
  background: #dfe6f3;
  box-shadow: 0 10px 25px rgba(21, 27, 39, 0.06);
}

.map-search-toolbar {
  position: absolute;
  top: 14px;
  left: 14px;
  right: 14px;
  z-index: 3;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border: 1px solid rgba(226, 232, 240, 0.95);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(6px);
}

.search-block {
  flex: 1 1 320px;
  min-width: 0;
}

.search-input-shell {
  position: relative;
}

.search-icon {
  position: absolute;
  top: 50%;
  left: 12px;
  transform: translateY(-50%);
  color: #64748b;
  font-size: 18px;
  pointer-events: none;
  z-index: 1;
}

.search-address-input,
.search-address-input:deep(.address-autocomplete-input) {
  width: 100%;
  min-height: 44px;
  padding: 0 14px 0 48px;
  border-radius: 12px;
  border-color: #d7deea;
  font-size: 14px;
  box-sizing: border-box;
}

.radius-group {
  display: flex;
  gap: 6px;
  flex: 0 0 auto;
}

.radius-btn {
  min-width: 62px;
  height: 42px;
  padding: 0 10px;
  border: 1px solid #d7deea;
  border-radius: 10px;
  background: #f8fafc;
  color: #1f2937;
  font-size: 12px;
  font-weight: 700;
}

.radius-btn.active {
  background: #eef2ff;
  border-color: #b8c3dd;
}

.toolbar-actions {
  display: flex;
  gap: 8px;
  flex: 0 0 auto;
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
  background: #020617;
  border-color: #020617;
  color: #ffffff;
}

.packages-shell {
  min-height: 320px;
}

.mobile-search-shell {
  display: none;
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
@media screen and (min-width: 1024px) {
  .home-page {
    grid-template-columns: 1fr minmax(340px, 28vw);
    min-height: 620px;
  }

  .packages-shell {
    height: 100%;
  }
}

@media screen and (max-width: 767px) {
  .home-page {
    min-height: 480px;
    padding: 8px;
    gap: 10px;
    width: 100%;
    max-width: 100%;
    overflow-x: hidden;
  }

  .map-shell {
    min-height: 360px;
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
  }

  .search-block,
  .toolbar-actions,
  .radius-group {
    width: 100%;
  }

  .toolbar-actions {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .radius-btn,
  .toolbar-btn {
    width: 100%;
  }

  .packages-shell {
    width: 100%;
    max-width: 100%;
    min-width: 0;
    overflow-x: hidden;
  }

  .map-legend {
    left: 10px;
    bottom: 10px;
    padding: 8px 10px;
  }

  .home-page.mobile-map-mode .packages-shell {
    min-height: 0;
  }

  .home-page.mobile-list-mode .map-shell {
    display: none;
  }

  .home-page.mobile-list-mode .packages-shell {
    min-height: calc(100vh - 140px);
  }
}
</style>
