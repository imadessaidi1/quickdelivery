<template>
  <div class="tracking-layout">
    <aside class="tracking-summary">
      <div class="summary-card">
        <div class="summary-head">
          <h2>{{ $t('trackingSummaryTitle') }}</h2>
          <button class="details-btn summary-mobile-btn" @click="details">{{ $t('packagesArroundMArkerDetailActionsDetails') }}</button>
        </div>
        <PackageSummary/>
      </div>
    </aside>

    <div class="tracking-map-shell">
      <div class="route-metrics" v-if="routeInfo && (routeInfo.distance.text || routeInfo.duration.text)">
        <div class="metric-card">
          <small>{{ $t('trackingDistanceLabel') }}</small>
          <strong>{{ routeInfo.distance.text || '-' }}</strong>
        </div>
        <div class="metric-card">
          <small>{{ $t('trackingDurationLabel') }}</small>
          <strong>{{ routeInfo.duration.text || '-' }}</strong>
        </div>
      </div>

      <div class="map-frame">
        <iframe
          ref="map"
          width="100%"
          height="100%"
          :src="googleMapPath"
          style="border: 0;"
          @load="onLoadIframe"
          name="map"
        ></iframe>
      </div>

      <div class="floating-action">
        <button @click="details" class="details-btn">{{ $t('packagesArroundMArkerDetailActionsDetails') }}</button>
      </div>
    </div>

    <div v-if="showModal" class="tracking-modal">
      <div class="tracking-modal-content">
        <button class="close-btn" @click="closeDetails">
          <span class="material-symbols-outlined">close</span>
        </button>
        <div class="modal-header">
          <h2>{{ $t('trackingSummaryTitle') }}</h2>
        </div>
        <PackageSummary/>
      </div>
    </div>
  </div>
</template>

<script>
import PackageSummary from '../components/VerticalPackageDetails.vue';
import http from '@/config/httpInterceptor';

export default {
  components: {
    PackageSummary,
  },
  data() {
    return {
      googleMapPath: process.env.BASE_URL + 'google-maps-package-tracking.html',
      showModal: false,
    };
  },
  props: {
    routeInfo: Object,
  },
  mounted() {
    http.get(this.$i18n.t('rootURL') + this.$i18n.t('getPackage') + this.$parent.packageReference)
      .then((response) => {
        this.$store.commit('updatePackage', response.data);
      }).catch(() => {
        console.error('Unable to process your request this time. Please try again later.');
      });
  },
  methods: {
    onLoadIframe() {
      this.$refs.map.contentWindow.postMessage('PackageReference:' + this.$parent.packageReference, '*');
    },
    details() {
      this.showModal = true;
    },
    closeDetails() {
      this.showModal = false;
    },
  },
  watch: {
    '$store.state.packagesLastPosition': {
      deep: true,
      handler(newVal) {
        const newPosition = newVal[this.$parent.packageReference];
        this.$refs.map.contentWindow.postMessage('PackageNewPosition;' + JSON.stringify(newPosition), '*');
      },
    },
  },
};
</script>

<style scoped>
.tracking-layout {
  display: grid;
  grid-template-columns: minmax(280px, 330px) minmax(0, 1fr);
  gap: 12px;
  height: 100%;
  min-height: 0;
}

.tracking-summary {
  min-width: 0;
  align-self: start;
}

.summary-card {
  height: auto;
  max-height: none;
  border: 1px solid #e5e7eb;
  border-radius: 18px;
  background: #ffffff;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.06);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.summary-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-bottom: 1px solid #eef2f7;
  flex-shrink: 0;
}

.summary-head h2 {
  margin: 0;
  color: #0f172a;
  font-size: 0.92rem;
  line-height: 1.05;
}

.summary-mobile-btn {
  display: none;
}

.tracking-map-shell {
  position: relative;
  min-height: 0;
  height: 100%;
  border: 1px solid #dde3ec;
  border-radius: 18px;
  overflow: hidden;
  background: #dfe6f3;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.06);
}

.route-metrics {
  position: absolute;
  top: 10px;
  left: 10px;
  right: 10px;
  z-index: 4;
  display: flex;
  gap: 8px;
  justify-content: center;
}

.metric-card {
  min-width: 132px;
  padding: 8px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.08);
  text-align: center;
}

.metric-card small {
  display: block;
  margin-bottom: 2px;
  color: #64748b;
  font-size: 0.72rem;
  line-height: 1.1;
}

.metric-card strong {
  color: #0f172a;
  font-size: 0.88rem;
  line-height: 1.1;
}

.map-frame {
  width: 100%;
  height: 100%;
  min-height: 0;
}

.floating-action {
  display: none;
  position: absolute;
  right: 50%;
  bottom: 12px;
  transform: translateX(50%);
  z-index: 4;
}

.details-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 118px;
  height: 34px;
  padding: 0 14px;
  border: none;
  border-radius: 10px;
  background: #020617;
  color: #ffffff;
  font-weight: 600;
  font-size: 0.82rem;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.12);
}

.tracking-modal {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 16px;
  background: rgba(15, 23, 42, 0.42);
}

.tracking-modal-content {
  position: relative;
  width: min(960px, 100%);
  max-height: 90vh;
  overflow-y: auto;
  padding: 16px;
  border-radius: 18px;
  background: #f8fafc;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.18);
}

.modal-header {
  margin-bottom: 16px;
}

.modal-header h2 {
  margin: 0;
  color: #0f172a;
}

.close-btn {
  position: absolute;
  top: 14px;
  right: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: 1px solid #dbe1ea;
  border-radius: 999px;
  background: #ffffff;
  color: #0f172a;
}

@media screen and (max-width: 1100px) {
  .tracking-layout {
    grid-template-columns: 1fr;
  }

  .tracking-summary {
    display: none;
  }

  .floating-action {
    display: block;
  }

  .tracking-map-shell,
  .map-frame {
    min-height: 0;
  }
}

@media screen and (max-width: 767px) {
  .tracking-layout {
    height: auto;
  }

  .route-metrics {
    flex-direction: column;
    left: 10px;
    right: 10px;
    top: 10px;
  }

  .metric-card {
    min-width: 0;
  }

  .tracking-map-shell,
  .map-frame {
    min-height: 320px;
  }
}
</style>
