<template>
  <div class="package-tracking-page qd-page">
    <header class="qd-page-header">
      <div class="header-main">
        <button v-if="showBackButton" class="back-link qd-btn-secondary" type="button" @click="goBack">
          <span class="icon">←</span> {{ $t('actionBack') }}
        </button>
        <h1>{{ $t('trackingPageTitle') }}</h1>
        <p>{{ $t('trackingPageSubtitle') }}</p>
      </div>
      <div class="qd-page-header-actions">
        <div class="reference-chip">{{ packageReference }}</div>
        <div v-if="packageData && packageData.courierName" class="courier-contact-chip">
          <span class="courier-name">{{ packageData.courierName }}</span>
          <a v-if="packageData.courierPhone" :href="'tel:' + packageData.courierPhone" class="courier-call-mini">
            <span class="material-symbols-outlined">call</span>
          </a>
        </div>
      </div>
    </header>

    <PackageTrackingGoogleMap
      v-if="trackingState === 'ready' && packageData"
      ref="mapTrackingVue"
      :routeInfo="routeInfo"
      :packageData="packageData"
      :packageReference="packageReference"
    />
    <div v-else class="tracking-state-card">
      <template v-if="trackingState === 'loading'">
        <h2>{{ $t('stateLoading') }}</h2>
        <p>{{ $t('trackingPageSubtitle') }}</p>
      </template>
      <template v-else>
      <h2>{{ $t(trackingTitleKey) }}</h2>
      <p>{{ $t(trackingMessageKey, { reference: packageReference }) }}</p>
      </template>
    </div>
  </div>
</template>

<script>
import PackageTrackingGoogleMap from '../components/PackageTrackingGoogleMap.vue';
import { fetchTrackingPackage } from '../config/tracking';

export default {
  components: {
    PackageTrackingGoogleMap,
  },
  props: {
    packageReference: String,
    guestAccessToken: {
      type: String,
      default: '',
    },
    returnTo: {
      type: String,
      default: '',
    },
  },
  data() {
    return {
      trackingState: 'loading',
      packageData: null,
      trackingMessageHandler: null,
      isTrackingLoaderActive: false,
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
  async mounted() {
    this.setTrackingLoader(true);
    if (!this.packageReference) {
      this.trackingState = 'not_found';
      this.setTrackingLoader(false);
      return;
    }

    this.trackingMessageHandler = (e) => {
      if (typeof e.data === 'string' && e.data === 'StartLoading') {
        this.setTrackingLoader(true);
      } else if (typeof e.data === 'string' && e.data === 'EndLoading') {
        this.setTrackingLoader(false);
      } else if (typeof e.data === 'string' && e.data.includes('RouteInfo;')) {
        this.routeInfo = JSON.parse(e.data.split(';')[1]);
      } else if (typeof e.data === 'string' && e.data === 'PackageNotFound') {
        this.trackingState = 'not_found';
        this.setTrackingLoader(false);
      } else if (typeof e.data === 'string' && e.data === 'TrackingError') {
        this.trackingState = 'error';
        this.setTrackingLoader(false);
      }
    };
    window.addEventListener('message', this.trackingMessageHandler);

    try {
      const response = await fetchTrackingPackage(this.packageReference, this.guestAccessToken, this.$i18n);
      this.packageData = response.data;
      this.$store.commit('updatePackage', response.data);
      this.trackingState = 'ready';
      this.setTrackingLoader(false);
    } catch (error) {
      const status = error?.response?.status;
      this.trackingState = status === 404 || status === 403 ? 'not_found' : 'error';
      this.setTrackingLoader(false);
    }
  },
  beforeUnmount() {
    if (this.trackingMessageHandler) {
      window.removeEventListener('message', this.trackingMessageHandler);
      this.trackingMessageHandler = null;
    }
    this.setTrackingLoader(false);
  },
  computed: {
    showBackButton() {
      return Boolean(this.returnTo) && !this.guestAccessToken;
    },
    trackingTitleKey() {
      return this.trackingState === 'not_found' ? 'trackingNotFoundTitle' : 'trackingErrorTitle';
    },
    trackingMessageKey() {
      return this.trackingState === 'not_found' ? 'trackingNotFoundMessage' : 'trackingErrorMessage';
    },
  },
  methods: {
    setTrackingLoader(isActive) {
      if (this.isTrackingLoaderActive === isActive) {
        return;
      }
      this.isTrackingLoaderActive = isActive;
      this.$store.commit('updateLoaderStatus', isActive);
    },
    goBack() {
      if (this.returnTo) {
        this.$router.push(this.returnTo);
        return;
      }
      this.$router.push('/myPackages');
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

.back-link {
  display: flex;
  align-items: center;
  gap: 8px;
  background: none;
  border: none;
  color: #64748b;
  font-weight: 700;
  font-size: 0.9375rem;
  cursor: pointer;
  padding: 0;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.back-link:hover {
  color: #0f172a;
  transform: translateX(-4px);
}

.back-link .icon {
  font-size: 1.1rem;
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
  background: var(--qd-primary-soft);
  color: var(--qd-primary-dark);
  padding: 8px 16px;
  border-radius: 12px;
  font-weight: 800;
  font-size: 0.9rem;
  letter-spacing: 0.02em;
}

.header-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.courier-contact-chip {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #ffffff;
  padding: 4px 4px 4px 12px;
  border-radius: 99px;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.08);
  border: 1px solid #e2e8f0;
}

.courier-name {
  font-size: 0.82rem;
  font-weight: 800;
  color: #0f172a;
  text-transform: capitalize;
}

.courier-call-mini {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #10b981;
  color: #ffffff;
  border-radius: 50%;
  text-decoration: none;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.courier-call-mini:hover {
  transform: scale(1.1);
  background: #059669;
  box-shadow: 0 4px 10px rgba(16, 185, 129, 0.3);
}

.courier-call-mini .material-symbols-outlined {
  font-size: 1.1rem;
}

@media screen and (max-width: 1180px) and (min-width: 768px) {
  .package-tracking-page {
    height: auto;
    min-height: 100%;
    overflow-y: auto;
    padding: 14px;
  }

  .page-header {
    align-items: center;
    gap: 14px;
  }

  .page-header h1 {
    font-size: 1.75rem;
  }

  .page-header p {
    font-size: 0.86rem;
  }

  .reference-chip {
    max-width: 280px;
  }
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

  .back-link {
    width: 100%;
    justify-content: center;
  }

  .page-header h1 {
    font-size: 1.55rem;
  }

  .page-header p {
    font-size: 0.82rem;
  }
}
</style>
