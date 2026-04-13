<template>
  <div class="tracking-summary-page qd-page">
    <template v-if="isLoadingPage">
      <header class="qd-page-header">
        <div class="header-main">
          <button class="back-link qd-btn-secondary" type="button" @click="goBack" style="margin-bottom: 12px; height: 36px; padding: 0 16px; border-radius: 10px; font-size: 0.85rem;">
            <span class="icon">←</span> {{ $t('actionBack') }}
          </button>
          <h1>{{ $t('trackingSummaryTitle') }}</h1>
          <p>{{ packageReference }}</p>
        </div>
      </header>
      <div class="page-state">{{ $t('stateLoading') }}</div>
    </template>
    <template v-else-if="loadError">
      <header class="qd-page-header">
        <div class="header-main">
          <button class="back-link qd-btn-secondary" type="button" @click="goBack" style="margin-bottom: 12px; height: 36px; padding: 0 16px; border-radius: 10px; font-size: 0.85rem;">
            <span class="icon">←</span> {{ $t('actionBack') }}
          </button>
          <h1>{{ $t('trackingSummaryTitle') }}</h1>
          <p>{{ packageReference }}</p>
        </div>
      </header>
      <div class="page-state error">{{ $t('stateLoadError') }}</div>
    </template>
    <template v-else>
      <header class="qd-page-header">
        <div class="header-main">
          <button class="back-link qd-btn-secondary" type="button" @click="goBack" style="margin-bottom: 12px; height: 36px; padding: 0 16px; border-radius: 10px; font-size: 0.85rem;">
            <span class="icon">←</span> {{ $t('actionBack') }}
          </button>
          <h1>{{ $t('trackingSummaryTitle') }}</h1>
          <p>{{ packageReference }}</p>
        </div>
      </header>

      <div class="summary-shell">
        <VerticalPackageDetails />
      </div>
    </template>
  </div>
</template>

<script>
import VerticalPackageDetails from '../components/VerticalPackageDetails.vue';
import { fetchTrackingPackage } from '../config/tracking';

export default {
  components: {
    VerticalPackageDetails,
  },
  props: {
    packageReference: {
      type: String,
      default: '',
    },
    returnTo: {
      type: String,
      default: '',
    },
    guestAccessToken: {
      type: String,
      default: '',
    },
  },
  async mounted() {
    if (!this.packageReference) {
      return;
    }
    this.isLoadingPage = true;
    this.loadError = false;
    try {
      const response = await fetchTrackingPackage(this.packageReference, this.guestAccessToken, this.$i18n);
      this.$store.commit('updatePackage', response.data);
    } catch (error) {
      this.loadError = true;
      console.error('Unable to load tracking summary.', error);
    } finally {
      this.isLoadingPage = false;
    }
  },
  data() {
    return {
      isLoadingPage: false,
      loadError: false,
    };
  },
  methods: {
    goBack() {
      if (window.history.length > 1) {
        this.$router.back();
        return;
      }
      if (this.returnTo) {
        this.$router.push(this.returnTo);
        return;
      }
      this.$router.push('/');
    },
  },
};
</script>

<style scoped>
.tracking-summary-page {
  padding-bottom: 40px;
}

.page-head {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 24px;
}

.page-head h1 {
  margin: 0;
  font-size: 2.5rem;
  font-weight: 800;
  color: #0f172a;
}

.page-head p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 1.125rem;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
}

.summary-shell {
  border: 1px solid #e5e7eb;
  border-radius: 24px;
  background: #ffffff;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.06);
  overflow: hidden;
}

@media screen and (max-width: 767px) {
  .tracking-summary-page {
    padding: 12px;
  }

  .page-head {
    flex-direction: column;
  }
}
</style>
