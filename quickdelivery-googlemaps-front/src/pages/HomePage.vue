<template>
  <div
    class="home-page"
    :class="{
      'mobile-map-mode': isMobile && mobileViewMode === 'map',
      'mobile-list-mode': isMobile && mobileViewMode === 'list',
    }"
  >
    <div class="map-shell">
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
    <PackageDetailsModal ref="AppModal" classe="modal"/>
  </div>
</template>

<script>
import GoogleMap from '../components/GoogleMap.vue';
import Carrousel from '../components/PackagesCarousel.vue'
import PackageDetailsModal from "../components/PackageDetailsModal.vue";

export default {
  data() {
    return {
      mobileViewMode: 'map',
      isMobile: window.innerWidth < 768,
    };
  },
  mounted() {
    window.addEventListener('resize', this.handleResize);
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.handleResize);
  },
  methods: {
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

.packages-shell {
  min-height: 320px;
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
