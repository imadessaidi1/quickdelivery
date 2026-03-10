<template>
  <div class="package-tracking-page">
    <div class="page-header">
      <div>
        <h1>{{ $t('trackingPageTitle') }}</h1>
        <p>{{ $t('trackingPageSubtitle') }}</p>
      </div>
      <div class="reference-chip">{{ packageReference }}</div>
    </div>

    <PackageTrackingGoogleMap ref="mapTrackingVue" :routeInfo="routeInfo"/>
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
      }
    };
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
