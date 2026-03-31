<template>
  <div class="tracking-summary-page">
    <template v-if="isLoadingPage">
      <div class="page-head">
        <button class="btn primary_btn back-btn" type="button" @click="goBack">
          {{ $t('actionBack') }}
        </button>
        <div>
          <h1>{{ $t('trackingSummaryTitle') }}</h1>
          <p>{{ packageReference }}</p>
        </div>
      </div>
      <div class="page-state">{{ $t('stateLoading') }}</div>
    </template>
    <template v-else-if="loadError">
      <div class="page-head">
        <button class="btn primary_btn back-btn" type="button" @click="goBack">
          {{ $t('actionBack') }}
        </button>
        <div>
          <h1>{{ $t('trackingSummaryTitle') }}</h1>
          <p>{{ packageReference }}</p>
        </div>
      </div>
      <div class="page-state error">{{ $t('stateLoadError') }}</div>
    </template>
    <template v-else>
      <div class="page-head">
        <button class="btn primary_btn back-btn" type="button" @click="goBack">
          {{ $t('actionBack') }}
        </button>
        <div>
          <h1>{{ $t('trackingSummaryTitle') }}</h1>
          <p>{{ packageReference }}</p>
        </div>
      </div>

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
  min-height: 100%;
  padding: 24px;
  background: #f6f7f9;
  box-sizing: border-box;
}

.page-head {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 18px;
}

.page-head h1 {
  margin: 0;
  color: #0f172a;
}

.page-head p {
  margin: 6px 0 0;
  color: #64748b;
}

.back-btn {
  min-width: 110px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 42px;
  padding: 0 18px;
  border: none;
  border-radius: 12px;
  background: #020617;
  color: #ffffff;
  font-weight: 600;
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.12);
}

.summary-shell {
  border: 1px solid #e5e7eb;
  border-radius: 18px;
  background: #ffffff;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.06);
  overflow: hidden;
}

@media screen and (max-width: 767px) {
  .tracking-summary-page {
    padding: 16px;
  }

  .page-head {
    flex-direction: column;
  }
}
</style>
