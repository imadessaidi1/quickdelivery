<template>
  <div class="active-route-page">
    <div class="page-header">
      <div>
        <div class="page-chip">{{ $t('activeRouteChip') }}</div>
        <h1>{{ $t('activeRouteTitle') }}</h1>
        <p>{{ $t('activeRouteSubtitle') }}</p>
      </div>
      <div class="header-actions">
        <router-link to="/app" class="secondary-action">
          {{ $t('activeRouteBackToMap') }}
        </router-link>
        <button
          v-if="activeRoute"
          class="primary-action"
          type="button"
          :disabled="!canStartRoute"
          :title="canStartRoute ? '' : $t('activeRouteStartDisabled')"
          @click="startRoute"
        >
          {{ $t('actionStartRoute') }}
        </button>
        <button
          class="primary-action"
          type="button"
          :disabled="!nextNavigationUrl"
          @click="openNextNavigationSegment"
        >
          {{ $t('activeRouteOpenInGoogleMaps') }}
        </button>
        <button
          v-if="activeRoute"
          class="danger-action"
          type="button"
          :disabled="!canCancelRoute"
          :title="canCancelRoute ? '' : $t('activeRouteCancelDisabled')"
          @click="cancelRoute"
        >
          {{ $t('actionCancelRoute') }}
        </button>
      </div>
    </div>

    <template v-if="isLoadingPage">
      <div class="page-state">{{ $t('stateLoading') }}</div>
    </template>

    <template v-else-if="!activeRoute">
      <div class="page-state">{{ $t('activeRouteEmpty') }}</div>
    </template>

    <template v-else>
      <section class="summary-grid">
        <article class="summary-card accent-primary">
          <small>{{ $t('activeRouteStatusLabel') }}</small>
          <strong>{{ routeStatusLabel }}</strong>
        </article>
        <article class="summary-card">
          <small>{{ $t('activeRoutePackagesLabel') }}</small>
          <strong>{{ packageCount }}</strong>
        </article>
        <article class="summary-card">
          <small>{{ $t('activeRouteDistanceLabel') }}</small>
          <strong>{{ totalDistanceLabel }}</strong>
        </article>
        <article class="summary-card">
          <small>{{ $t('activeRouteDurationLabel') }}</small>
          <strong>{{ durationLabel }}</strong>
        </article>
        <article class="summary-card">
          <small>{{ displayedPriceLabel }}</small>
          <strong>{{ totalAmountLabel }}</strong>
        </article>
        <article class="summary-card">
          <small>{{ $t('activeRouteProgressLabel') }}</small>
          <strong>{{ completedStops }}/{{ totalStops }}</strong>
        </article>
      </section>

      <section class="route-layout">
        <article class="route-panel progress-panel">
          <div class="panel-header">
            <h2>{{ $t('activeRouteCurrentSegmentTitle') }}</h2>
            <span class="segment-pill">{{ currentSegmentLabel }}</span>
          </div>
          <p class="panel-subtitle">{{ $t('activeRouteCurrentSegmentSubtitle') }}</p>
          <div class="progress-track">
            <div class="progress-fill" :style="{ width: `${progressPercent}%` }"></div>
          </div>
          <div class="progress-meta">
            <span>{{ progressPercent }}%</span>
            <span>{{ nextStopLabel }}</span>
          </div>
          <div v-if="segmentSummaries.length" class="segment-list">
            <button
              v-for="segment in segmentSummaries"
              :key="segment.index"
              type="button"
              class="segment-card"
              :class="{ active: segment.index === currentSegmentIndex }"
              @click="openNavigationSegment(segment.index)"
            >
              <strong>{{ segment.label }}</strong>
              <span>{{ segment.stopRange }}</span>
            </button>
          </div>
        </article>

        <article class="route-panel steps-panel">
          <div class="panel-header">
            <h2>{{ $t('activeRouteStopsTitle') }}</h2>
            <span class="segment-pill neutral">{{ totalStops }}</span>
          </div>
          <div class="stops-list">
            <div
              v-for="stop in decoratedStops"
              :key="`${stop.packageId}-${stop.kind}-${stop.order}`"
              class="stop-row"
              :class="{ done: stop.isDone, active: stop.order === nextStopOrder }"
            >
              <div class="stop-order">{{ stop.order }}</div>
              <div class="stop-body">
                <div class="stop-topline">
                  <span class="stop-kind" :class="stop.kind">{{ stop.kindLabel }}</span>
                  <strong>{{ stop.packageReference }}</strong>
                </div>
                <div class="stop-address">{{ stop.addressLabel || '-' }}</div>
              </div>
              <div class="stop-status">{{ stop.statusLabel }}</div>
            </div>
          </div>
        </article>
      </section>
    </template>
  </div>
</template>

<script>
import http from '@/config/httpInterceptor';
import { resolveDisplayedPackagePriceLabel } from '@/config/packagePricing';

const MAX_INTERMEDIATE_WAYPOINTS = 8;

export default {
  data() {
    return {
      isLoadingPage: false,
    };
  },
  computed: {
    activeRoute() {
      return this.$store.state.activeDeliveryRoute || null;
    },
    connectedUserId() {
      return this.$store.state.connectedUser?.id ?? null;
    },
    routeStops() {
      return Array.isArray(this.activeRoute?.stops) ? this.activeRoute.stops : [];
    },
    totalStops() {
      return this.routeStops.length;
    },
    packageCount() {
      return Array.isArray(this.activeRoute?.packageIds) ? this.activeRoute.packageIds.length : 0;
    },
    displayedPriceLabel() {
      return resolveDisplayedPackagePriceLabel(this.$i18n);
    },
    routeStatusLabel() {
      const status = this.activeRoute?.status || 'PLANNED';
      return this.$t(status);
    },
    totalDistanceLabel() {
      const meters = Number(this.activeRoute?.metrics?.totalDistanceMeters || 0);
      if (!meters) {
        return '-';
      }
      return `${(meters / 1000).toFixed(1)} km`;
    },
    durationLabel() {
      const minutes = Number(this.activeRoute?.metrics?.estimatedDurationMinutes || 0);
      if (!minutes) {
        return '-';
      }
      return `${minutes.toFixed(0)} min`;
    },
    totalAmountLabel() {
      const amount = Number(this.activeRoute?.metrics?.totalDisplayedAmount || 0);
      if (!amount) {
        return '-';
      }
      return `${amount.toFixed(2)} ${this.$t('currency')}`;
    },
    decoratedStops() {
      return this.routeStops.map((stop) => {
        const isPickup = stop.kind === 'pickup';
        const isDone = stop.status === 'DONE';
        return {
          ...stop,
          isDone,
          kindLabel: this.$t(isPickup ? 'activeRoutePickupLabel' : 'activeRouteDropoffLabel'),
          statusLabel: isDone ? this.$t('activeRouteStopDone') : this.$t('activeRouteStopPending'),
        };
      });
    },
    completedStops() {
      return this.decoratedStops.filter((stop) => stop.isDone).length;
    },
    progressPercent() {
      if (!this.totalStops) {
        return 0;
      }
      return Math.round((this.completedStops / this.totalStops) * 100);
    },
    nextStop() {
      return this.decoratedStops.find((stop) => !stop.isDone) || null;
    },
    nextStopOrder() {
      return this.nextStop?.order || null;
    },
    nextStopLabel() {
      if (!this.nextStop) {
        return this.$t('activeRouteCompleted');
      }
      return `${this.nextStop.kindLabel} #${this.nextStop.order}`;
    },
    googleMapsNavigationUrls() {
      return Array.isArray(this.activeRoute?.googleMapsNavigationUrls)
        ? this.activeRoute.googleMapsNavigationUrls
        : [];
    },
    segmentSummaries() {
      return this.googleMapsNavigationUrls.map((url, index) => {
        const startStop = index * MAX_INTERMEDIATE_WAYPOINTS + 1;
        const endStop = Math.min(this.totalStops, startStop + MAX_INTERMEDIATE_WAYPOINTS - 1);
        return {
          index,
          url,
          label: `${this.$t('activeRouteSegmentLabel')} ${index + 1}`,
          stopRange: `${this.$t('activeRouteStopsRangeLabel')} ${startStop}-${endStop}`,
          startStop,
          endStop,
        };
      });
    },
    currentSegmentIndex() {
      if (!this.segmentSummaries.length || !this.nextStopOrder) {
        return 0;
      }
      const segmentIndex = this.segmentSummaries.findIndex((segment) =>
        this.nextStopOrder >= segment.startStop && this.nextStopOrder <= segment.endStop,
      );
      return segmentIndex >= 0 ? segmentIndex : 0;
    },
    currentSegmentLabel() {
      const segment = this.segmentSummaries[this.currentSegmentIndex] || this.segmentSummaries[0];
      return segment ? segment.label : '-';
    },
    nextNavigationUrl() {
      const segment = this.segmentSummaries[this.currentSegmentIndex] || this.segmentSummaries[0];
      return segment?.url || this.activeRoute?.googleMapsNavigationUrl || '';
    },
    canCancelRoute() {
      return this.activeRoute?.status === 'PLANNED';
    },
    canStartRoute() {
      return this.activeRoute?.status === 'PLANNED' && this.totalStops > this.completedStops;
    },
  },
  mounted() {
    this.initializePage();
  },
  methods: {
    async initializePage() {
      this.isLoadingPage = true;
      try {
        await this.fetchActiveRoute();
      } finally {
        this.isLoadingPage = false;
      }
    },
    async fetchActiveRoute() {
      if (!this.connectedUserId) {
        return;
      }
      try {
        const response = await http.get(
          `${this.$i18n.t('rootURL')}${this.$i18n.t('activeDeliveryRouteUrl')}${encodeURIComponent(this.connectedUserId)}`,
          { silent: true },
        );
        this.$store.commit('setActiveDeliveryRoute', response?.data || null);
      } catch (error) {
        console.warn('Unable to refresh active delivery route.', error);
      }
    },
    openNavigationSegment(segmentIndex) {
      const segment = this.segmentSummaries[segmentIndex];
      if (!segment?.url) {
        return;
      }
      window.open(segment.url, '_blank', 'noopener');
    },
    openNextNavigationSegment() {
      if (!this.nextNavigationUrl) {
        return;
      }
      window.open(this.nextNavigationUrl, '_blank', 'noopener');
    },
    async cancelRoute() {
      if (!this.canCancelRoute || !window.confirm(this.$t('activeRouteCancelConfirm'))) {
        return;
      }
      try {
        await http.put(`${this.$i18n.t('rootURL')}${this.$i18n.t('cancelActiveRouteUrl')}${encodeURIComponent(this.connectedUserId)}`);
        this.$store.commit('setOngoingDeliveryState', {
          isUserWithOngoingDelivery: false,
          activeDeliveryRoute: null,
        });
        window.dispatchEvent(new CustomEvent('qd-refresh-reservation-availability'));
        this.$router.push('/app');
      } catch (error) {
        console.error('Unable to cancel active route.', error);
      }
    },
    async startRoute() {
      if (!this.canStartRoute) {
        return;
      }
      try {
        const response = await http.put(`${this.$i18n.t('rootURL')}${this.$i18n.t('startActiveRouteUrl')}${encodeURIComponent(this.connectedUserId)}`);
        this.$store.commit('setActiveDeliveryRoute', response?.data || null);
        window.dispatchEvent(new CustomEvent('qd-refresh-reservation-availability'));
      } catch (error) {
        console.error('Unable to start active route.', error);
      }
    },
  },
};
</script>

<style scoped>
.active-route-page {
  min-height: 100%;
  padding: 28px;
  background: #f6f7f9;
  box-sizing: border-box;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 24px;
}

.page-chip {
  display: inline-flex;
  align-items: center;
  margin-bottom: 10px;
  padding: 6px 12px;
  border-radius: 999px;
  background: #fff2e8;
  color: #c9651a;
  font-size: 0.8rem;
  font-weight: 700;
}

.page-header h1 {
  margin: 0;
  font-size: 3rem;
  line-height: 1;
  color: #0f172a;
}

.page-header p {
  margin: 8px 0 0;
  color: #64748b;
  max-width: 720px;
}

.header-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.primary-action,
.secondary-action,
.danger-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 168px;
  height: 44px;
  padding: 0 18px;
  border-radius: 14px;
  text-decoration: none;
  font-weight: 700;
}

.secondary-action {
  border: 1px solid #d5d9e2;
  background: #fff;
  color: #334155;
}

.primary-action {
  border: 1px solid #cf6320;
  background: #ef7d32;
  color: #fff;
}

.danger-action {
  border: 1px solid #fecaca;
  background: #fee2e2;
  color: #991b1b;
}

.primary-action:disabled,
.danger-action:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 20px;
}

.summary-card {
  padding: 18px;
  border: 1px solid #e5e7eb;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.04);
}

.summary-card small {
  display: block;
  margin-bottom: 8px;
  color: #64748b;
}

.summary-card strong {
  color: #0f172a;
  font-size: 1.1rem;
}

.summary-card.accent-primary {
  background: linear-gradient(135deg, #fff7ef, #fff);
  border-color: #f6d7bb;
}

.route-layout {
  display: grid;
  grid-template-columns: minmax(320px, 420px) minmax(0, 1fr);
  gap: 18px;
}

.route-panel {
  border: 1px solid #e5e7eb;
  border-radius: 22px;
  background: #fff;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.05);
  padding: 22px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.panel-header h2 {
  margin: 0;
  color: #0f172a;
  font-size: 1.2rem;
}

.panel-subtitle {
  margin: 0 0 18px;
  color: #64748b;
}

.segment-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 52px;
  padding: 6px 10px;
  border-radius: 999px;
  background: #fff2e8;
  color: #c9651a;
  font-size: 0.8rem;
  font-weight: 700;
}

.segment-pill.neutral {
  background: #f1f5f9;
  color: #475569;
}

.progress-track {
  width: 100%;
  height: 12px;
  border-radius: 999px;
  background: #edf2f7;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #ef7d32, #cf6320);
}

.progress-meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-top: 10px;
  color: #475569;
  font-size: 0.92rem;
}

.segment-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 20px;
}

.segment-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 14px 16px;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  background: #fff;
  color: #334155;
}

.segment-card.active {
  border-color: #ef7d32;
  background: #fff7ef;
}

.segment-card strong,
.stop-topline strong,
.stop-address,
.stop-status,
.progress-meta span {
  overflow-wrap: anywhere;
}

.stops-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 72vh;
  overflow: auto;
}

.stop-row {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr) 120px;
  gap: 14px;
  align-items: center;
  padding: 14px;
  border: 1px solid #e5e7eb;
  border-radius: 18px;
  background: #fff;
}

.stop-row.done {
  background: #f8fafc;
  opacity: 0.74;
}

.stop-row.active {
  border-color: #ef7d32;
  box-shadow: 0 10px 22px rgba(239, 125, 50, 0.12);
}

.stop-order {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 999px;
  background: #0f172a;
  color: #fff;
  font-weight: 700;
}

.stop-topline {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.stop-kind {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 700;
}

.stop-kind.pickup {
  background: #ecfccb;
  color: #4d7c0f;
}

.stop-kind.dropoff {
  background: #dbeafe;
  color: #1d4ed8;
}

.stop-address {
  margin-top: 6px;
  color: #475569;
  line-height: 1.4;
}

.stop-status {
  justify-self: end;
  color: #64748b;
  font-size: 0.9rem;
  font-weight: 600;
}

.page-state {
  padding: 16px;
  border-radius: 14px;
  background: #eef3f9;
  color: #334155;
  text-align: center;
}

@media screen and (max-width: 1200px) {
  .summary-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .route-layout {
    grid-template-columns: 1fr;
  }
}

@media screen and (max-width: 767px) {
  .active-route-page {
    padding: 16px;
  }

  .page-header {
    flex-direction: column;
    align-items: stretch;
  }

  .page-header h1 {
    font-size: 2.25rem;
  }

  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .header-actions {
    flex-direction: column;
  }

  .primary-action,
  .secondary-action,
  .danger-action {
    width: 100%;
  }

  .stop-row {
    grid-template-columns: 42px minmax(0, 1fr);
  }

  .stop-order {
    width: 42px;
    height: 42px;
  }

  .stop-status {
    grid-column: 2;
    justify-self: start;
  }

  .segment-card {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media screen and (max-width: 460px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }

  .panel-header,
  .progress-meta {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
