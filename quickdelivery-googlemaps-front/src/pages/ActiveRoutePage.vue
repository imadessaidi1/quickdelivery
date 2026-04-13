<template>
  <div class="active-route-page qd-page">
    <header class="qd-page-header">
      <div class="header-main">
        <span class="page-chip info uppercase">{{ $t('activeRouteChip') }}</span>
        <h1>{{ $t('activeRouteTitle') }}</h1>
        <p>{{ $t('activeRouteSubtitle') }}</p>
      </div>
      <div class="qd-page-header-actions">
        <router-link to="/app" class="qd-btn-secondary">
          {{ $t('activeRouteBackToMap') }}
        </router-link>
        <button
          v-if="activeRoute"
          class="qd-btn-primary"
          type="button"
          :disabled="!canStartRoute"
          :title="canStartRoute ? '' : $t('activeRouteStartDisabled')"
          @click="startRoute"
        >
          {{ $t('actionStartRoute') }}
        </button>
        <button
          class="qd-btn-primary"
          type="button"
          :disabled="!nextNavigationUrl"
          @click="openNextNavigationSegment"
        >
          <span class="material-symbols-outlined">explore</span>
          {{ $t('activeRouteOpenInGoogleMaps') }}
        </button>
        <button
          v-if="activeRoute"
          class="qd-btn-danger"
          type="button"
          :disabled="!canCancelRoute"
          :title="canCancelRoute ? '' : $t('activeRouteCancelDisabled')"
          @click="cancelRoute"
        >
          {{ $t('actionCancelRoute') }}
        </button>
      </div>
    </header>

    <template v-if="isLoadingPage">
      <div class="page-state">
        <div class="premium-spinner"></div>
        <span>{{ $t('stateLoading') }}</span>
      </div>
    </template>

    <template v-else-if="!activeRoute">
      <div class="page-state empty-mission">
        <span class="material-symbols-outlined large-icon">route</span>
        <p>{{ $t('activeRouteEmpty') }}</p>
        <router-link to="/app" class="qd-btn-primary">{{ $t('activeRouteBackToMap') }}</router-link>
      </div>
    </template>

    <template v-else>
      <section class="summary-grid">
        <PremiumDashboardCard tone="indigo" variant="glass">
          <div class="stat-content">
            <small>{{ $t('activeRouteStatusLabel') }}</small>
            <strong>{{ routeStatusLabel }}</strong>
          </div>
        </PremiumDashboardCard>
        <PremiumDashboardCard tone="slate" variant="glass">
          <div class="stat-content">
            <small>{{ $t('activeRoutePackagesLabel') }}</small>
            <strong>{{ packageCount }}</strong>
          </div>
        </PremiumDashboardCard>
        <PremiumDashboardCard tone="cyan" variant="glass">
          <div class="stat-content">
            <small>{{ $t('activeRouteDistanceLabel') }}</small>
            <strong>{{ totalDistanceLabel }}</strong>
          </div>
        </PremiumDashboardCard>
        <PremiumDashboardCard tone="amber" variant="glass">
          <div class="stat-content">
            <small>{{ $t('activeRouteDurationLabel') }}</small>
            <strong>{{ durationLabel }}</strong>
          </div>
        </PremiumDashboardCard>
        <PremiumDashboardCard tone="emerald" variant="glass">
          <div class="stat-content">
            <small>{{ displayedPriceLabel }}</small>
            <strong>{{ totalAmountLabel }}</strong>
          </div>
        </PremiumDashboardCard>
        <PremiumDashboardCard tone="indigo" variant="glass">
          <div class="stat-content">
            <small>{{ $t('activeRouteProgressLabel') }}</small>
            <strong>{{ completedStops }}/{{ totalStops }}</strong>
          </div>
        </PremiumDashboardCard>
      </section>

      <div class="mission-layout">
        <section class="mission-navigation">
          <article class="panel-card glass-pane">
            <div class="panel-head">
              <h2>{{ $t('activeRouteCurrentSegmentTitle') }}</h2>
              <span class="badge info">{{ currentSegmentLabel }}</span>
            </div>
            <p class="panel-desc">{{ $t('activeRouteCurrentSegmentSubtitle') }}</p>
            
            <div class="progress-container">
              <div class="progress-bar">
                <div class="progress-value" :style="{ width: `${progressPercent}%` }"></div>
              </div>
              <div class="progress-labels">
                <span>{{ progressPercent }}% {{ $t('activeRouteCompleted') }}</span>
                <strong>{{ nextStopLabel }}</strong>
              </div>
            </div>

            <div v-if="segmentSummaries.length" class="segment-stack">
              <button
                v-for="segment in segmentSummaries"
                :key="segment.index"
                type="button"
                class="segment-item tone-indigo"
                :class="{ active: segment.index === currentSegmentIndex }"
                @click="openNavigationSegment(segment.index)"
              >
                <div class="seg-info">
                  <strong>{{ segment.label }}</strong>
                  <small>{{ segment.stopRange }}</small>
                </div>
                <span class="material-symbols-outlined">chevron_right</span>
              </button>
            </div>
          </article>
        </section>

        <section class="mission-timeline">
          <article class="panel-card">
            <div class="panel-head">
              <h2>{{ $t('activeRouteStopsTitle') }}</h2>
              <span class="badge info outline">{{ totalStops }} {{ $t('activeRouteStopsLabel') }}</span>
            </div>
            
            <div class="timeline-stack">
              <div
                v-for="stop in decoratedStops"
                :key="`${stop.packageId}-${stop.kind}-${stop.order}`"
                class="timeline-node"
                :class="{ done: stop.isDone, current: stop.order === nextStopOrder }"
              >
                <div class="node-indicator">
                  <div class="node-dot">
                    <span v-if="stop.isDone" class="material-symbols-outlined">check</span>
                    <span v-else>{{ stop.order }}</span>
                  </div>
                  <div class="node-line"></div>
                </div>
                <div class="node-card" :class="stop.kind === 'pickup' ? 'tone-emerald' : 'tone-indigo'">
                  <div class="node-header">
                    <span class="badge" :class="stop.kind === 'pickup' ? 'success' : 'info'">
                      {{ stop.kindLabel }}
                    </span>
                    <span class="ref-id">{{ stop.packageReference }}</span>
                  </div>
                  <div class="node-address">{{ stop.addressLabel || '-' }}</div>
                  <div class="node-footer">
                    <span class="status-label">{{ stop.statusLabel }}</span>
                  </div>
                </div>
              </div>
            </div>
          </article>
        </section>
      </div>
    </template>
  </div>
</template>

<script>
import http from '@/config/httpInterceptor';
import { resolveDisplayedPackagePriceLabel } from '@/config/packagePricing';
import PremiumDashboardCard from '@/components/PremiumDashboardCard.vue';

const MAX_INTERMEDIATE_WAYPOINTS = 8;

export default {
  name: 'ActiveRoutePage',
  components: {
    PremiumDashboardCard,
  },
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
  padding-bottom: 40px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-content small {
  display: block;
  font-size: 0.75rem;
  color: var(--qd-muted);
  margin-bottom: 4px;
  font-weight: 600;
  text-transform: uppercase;
}

.stat-content strong {
  font-size: 1.25rem;
  color: var(--qd-text);
  font-weight: 800;
}

.mission-layout {
  display: grid;
  grid-template-columns: 380px 1fr;
  gap: 24px;
  align-items: start;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.panel-head h2 {
  font-size: 1.2rem;
  font-weight: 800;
  margin: 0;
}

.panel-desc {
  color: var(--qd-muted);
  font-size: 0.9rem;
  margin-bottom: 20px;
}

.progress-container {
  margin-bottom: 24px;
}

.progress-bar {
  height: 10px;
  background: var(--qd-bg);
  border-radius: 999px;
  overflow: hidden;
  margin-bottom: 10px;
}

.progress-value {
  height: 100%;
  background: linear-gradient(90deg, var(--qd-primary), var(--qd-accent));
  border-radius: inherit;
  transition: width 0.6s cubic-bezier(0.4, 0, 0.2, 1);
}

.progress-labels {
  display: flex;
  justify-content: space-between;
  font-size: 0.85rem;
}

.segment-stack {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.segment-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  background: var(--qd-surface-strong);
  border: 1px solid var(--qd-border);
  border-radius: var(--qd-radius);
  cursor: pointer;
  transition: var(--qd-transition);
  text-align: left;
}

.segment-item:hover {
  border-color: var(--qd-primary);
  transform: translateX(4px);
}

.segment-item.active {
  background: var(--qd-primary-soft);
  border-color: var(--qd-primary);
}

.seg-info strong {
  display: block;
  font-size: 0.95rem;
  margin-bottom: 2px;
}

.seg-info small {
  color: var(--qd-muted);
}

/* Timeline */
.timeline-stack {
  display: flex;
  flex-direction: column;
  padding-left: 12px;
}

.timeline-node {
  display: flex;
  gap: 20px;
}

.node-indicator {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.node-dot {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--qd-bg);
  border: 3px solid var(--qd-border);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  color: var(--qd-muted);
  flex-shrink: 0;
  transition: var(--qd-transition);
  z-index: 2;
}

.node-line {
  width: 3px;
  flex-grow: 1;
  background: var(--qd-border);
  margin: 4px 0;
}

.timeline-node:last-child .node-line {
  display: none;
}

.node-card {
  flex-grow: 1;
  background: var(--qd-surface-strong);
  border: 1px solid var(--qd-border);
  border-radius: var(--qd-radius);
  padding: 16px;
  margin-bottom: 20px;
  transition: var(--qd-transition);
}

/* Timeline States */
.timeline-node.done .node-dot {
  background: var(--qd-success);
  border-color: var(--qd-success);
  color: #fff;
}

.timeline-node.done .node-line {
  background: var(--qd-success);
}

.timeline-node.done .node-card {
  opacity: 0.7;
}

.timeline-node.current .node-dot {
  background: var(--qd-primary);
  border-color: var(--qd-primary);
  color: #fff;
  box-shadow: 0 0 0 5px var(--qd-primary-soft);
}

.timeline-node.current .node-card {
  border-color: var(--qd-primary);
  box-shadow: var(--qd-shadow);
}

.node-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.ref-id {
  font-family: monospace;
  font-weight: 700;
  color: var(--qd-muted);
}

.node-address {
  font-size: 0.95rem;
  color: var(--qd-text);
  line-height: 1.4;
  margin-bottom: 12px;
}

.node-footer .status-label {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--qd-muted);
}

.empty-mission {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px;
  color: var(--qd-muted);
}

.large-icon {
  font-size: 4rem;
  margin-bottom: 16px;
  opacity: 0.5;
}

.premium-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid var(--qd-primary-soft);
  border-top-color: var(--qd-primary);
  border-radius: 50%;
  animation: qd-spin 1s linear infinite;
  margin: 0 auto 16px;
}

@keyframes qd-spin { to { transform: rotate(360deg); } }

@media (max-width: 1280px) {
  .summary-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 1024px) {
  .mission-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .active-route-page { padding: 12px; gap: 14px; }
  .summary-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
  .stat-content small { font-size: 0.68rem; line-height: 1.2; }
  .stat-content strong { font-size: 1.15rem; line-height: 1.1; overflow-wrap: anywhere; }
  .empty-mission { padding: 24px 12px; }
  .large-icon { font-size: 2.25rem; margin-bottom: 8px; }
  .qd-page-header-actions { width: 100%; flex-direction: column; }
  .qd-page-header-actions > * { width: 100%; }
}
</style>
