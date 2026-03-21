<template>
  <div class="package-tracking-page">
    <div class="page-header">
      <div>
        <h1>{{ $t('trackingPageTitle') }}</h1>
        <p>{{ $t('trackingPageSubtitle') }}</p>
      </div>
      <div class="reference-chip">{{ packageReference }}</div>
    </div>

    <PackageTrackingGoogleMap v-if="trackingState === 'ready'" ref="mapTrackingVue" :routeInfo="routeInfo"/>
    <div v-else class="tracking-state-card">
      <h2>{{ $t(trackingTitleKey) }}</h2>
      <p>{{ $t(trackingMessageKey, { reference: packageReference }) }}</p>
    </div>
  </div>
</template>

<script>
import PackageTrackingGoogleMap from '../components/PackageTrackingGoogleMap.vue';

export default {
  components: {
    PackageTrackingGoogleMap,
  },
  props: {
    packageReference: String,
  },
  data() {
    return {
      trackingState: 'ready',
      routeInfo: {
        distance: {
          text: '',
          value: '',
        },
        duration: {
          text: '',
          value: '',
        },
      },
    };
  },
  mounted() {
    window.onmessage = (e) => {
      if (typeof e.data === 'string' && e.data === 'EndLoading') {
        this.$store.commit('updateLoaderStatus', false);
      } else if (typeof e.data === 'string' && e.data.includes('RouteInfo;')) {
        this.routeInfo = JSON.parse(e.data.split(';')[1]);
      } else if (typeof e.data === 'string' && e.data === 'PackageNotFound') {
        this.trackingState = 'not_found';
        this.$store.commit('updateLoaderStatus', false);
      } else if (typeof e.data === 'string' && e.data === 'TrackingError') {
        this.trackingState = 'error';
        this.$store.commit('updateLoaderStatus', false);
      }
    };
  },
  computed: {
    trackingTitleKey() {
      return this.trackingState === 'not_found' ? 'trackingNotFoundTitle' : 'trackingErrorTitle';
    },
    trackingMessageKey() {
      return this.trackingState === 'not_found' ? 'trackingNotFoundMessage' : 'trackingErrorMessage';
    },
  },
};
</script>

<style>
.package-tracking-page {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: 16px 18px;
  background: #f6f7f9;
  box-sizing: border-box;
  overflow: hidden;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 14px;
}

.page-header h1 {
  margin: 0;
  font-size: 2rem;
  line-height: 1.05;
  color: #0f172a;
}

.page-header p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 0.9rem;
}

.reference-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 36px;
  padding: 0 14px;
  border-radius: 999px;
  background: #020617;
  color: #ffffff;
  font-size: 0.85rem;
  font-weight: 700;
  text-align: center;
  overflow-wrap: anywhere;
}

.package-tracking-page :deep(.tracking-layout) {
  flex: 1;
  min-height: 0;
}

.tracking-state-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 10px;
  border: 1px solid #e5e7eb;
  border-radius: 18px;
  background: #ffffff;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.06);
  padding: 32px 24px;
  text-align: center;
}

.tracking-state-card h2 {
  margin: 0;
  color: #0f172a;
  font-size: 1.25rem;
}

.tracking-state-card p {
  margin: 0;
  color: #64748b;
  max-width: 520px;
}

@media screen and (max-width: 767px) {
  .package-tracking-page {
    padding: 12px;
  }

  .page-header {
    flex-direction: column;
    align-items: stretch;
  }

  .page-header h1 {
    font-size: 1.55rem;
  }

  .page-header p {
    font-size: 0.82rem;
  }
}
</style>
