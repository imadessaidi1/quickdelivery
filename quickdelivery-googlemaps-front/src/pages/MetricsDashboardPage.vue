<template>
  <div class="metrics-dashboard-page qd-page" :class="{ 'is-loading': isLoading && !currentSnapshot }">
    <template v-if="isLoading && !currentSnapshot">
      <header class="qd-page-header">
        <div class="header-main">
          <span class="page-chip pulse-chip">{{ $t('menuAdminMetrics') }}</span>
          <h1>{{ $t('metricsPageTitle') }}</h1>
          <p>{{ $t('metricsPageSubtitle') }}</p>
        </div>
      </header>
      <div class="page-state loading-state">
        <div class="spinner"></div>
        <span>{{ $t('stateLoading') }}</span>
      </div>
    </template>

    <template v-else-if="loadError && !currentSnapshot">
      <header class="qd-page-header">
        <div class="header-main">
          <span class="page-chip">{{ $t('menuAdminMetrics') }}</span>
          <h1>{{ $t('metricsPageTitle') }}</h1>
        </div>
      </header>
      <div class="page-state error-state">
        <span class="material-symbols-outlined">report_problem</span>
        <p>{{ $t('stateLoadError') }}</p>
      </div>
    </template>

    <template v-else>
      <header class="qd-page-header">
        <div class="header-main">
          <div class="hero-header-row" style="display: flex; align-items: center; gap: 16px; margin-bottom: 8px;">
            <span class="page-chip status-chip" :class="overallStatus.tone.replace('tone-', '')">
              <span class="status-pulse"></span>
              {{ overallStatus.label }}
            </span>
            <span class="refresh-time" style="font-size: 0.8rem; color: var(--qd-muted);">
              {{ $t('metricsPageLastRefresh') }}: {{ formatDateTime(lastRefreshAt) }}
            </span>
          </div>
          <h1>{{ $t('metricsPageTitle') }}</h1>
          <p>{{ $t('metricsPageSubtitle') }}</p>
        </div>
        
        <div class="qd-page-header-actions">
          <div class="hero-system-stats">
            <div class="system-stat-item">
              <span class="item-label">{{ $t('metricsOverallHealthy') }}</span>
              <strong class="item-value">{{ healthyModuleCount }}/{{ moduleCards.length }}</strong>
            </div>
            <div class="system-stat-item warn">
              <span class="item-label">{{ $t('metricsOverallIncidents') }}</span>
              <strong class="item-value">{{ currentAlerts.length }}</strong>
            </div>
            <div class="system-stat-item">
              <span class="item-label">{{ $t('metricsOverallTraffic') }}</span>
              <strong class="item-value">{{ formatInteger(lastHourTrafficTotal) }}</strong>
            </div>
          </div>
        </div>
      </header>

      <!-- Quick Operations Grid -->
      <section class="population-grid">
        <PremiumDashboardCard
          v-for="pop in populationStats"
          :key="pop.label"
          variant="glass"
          :tone="pop.tone"
          class="pop-card"
        >
          <div class="stat-container">
            <span class="stat-icon material-symbols-outlined">{{ pop.icon }}</span>
            <div class="stat-copy">
              <strong class="stat-value">{{ formatInteger(pop.value) }}</strong>
              <span class="stat-label">{{ pop.label }}</span>
            </div>
          </div>
        </PremiumDashboardCard>
      </section>

      <!-- Services Operations Grid -->
      <div class="metrics-main-grid">
        <section class="services-column">
          <div class="section-title-row">
            <h2>{{ $t('dashboardMetricsGatewayService') }} & Modules</h2>
            <div class="service-legend">
              <span class="legend-dot ok"></span> {{ $t('metricsHealthOk') }}
              <span class="legend-dot warn"></span> {{ $t('metricsHealthWarn') }}
              <span class="legend-dot critical"></span> {{ $t('metricsHealthCritical') }}
            </div>
          </div>

          <div class="module-cards-container">
            <PremiumDashboardCard
              v-for="module in moduleCards"
              :key="module.key"
              variant="glass"
              :tone="module.statusClass === 'status-ok' ? 'emerald' : module.statusClass === 'status-warn' ? 'amber' : 'rose'"
              class="module-premium-card"
              interactive
            >
              <template #header>
                <div class="module-card-header">
                  <div class="module-title-group">
                    <span class="status-indicator" :class="module.statusClass"></span>
                    <h3>{{ module.title }}</h3>
                  </div>
                  <span class="heartbeat">{{ formatTimeAgo(module.heartbeatAt) }}</span>
                </div>
              </template>

              <div v-if="module.metrics" class="module-body">
                <div class="main-metrics-row">
                  <div class="mini-gauge">
                    <div class="gauge-bar" :style="{ width: formatPercent(module.metrics.cpuUsagePercent), backgroundColor: getMetricTone(module.metrics.cpuUsagePercent) }"></div>
                    <div class="gauge-labels">
                      <span>CPU</span>
                      <strong>{{ formatPercent(module.metrics.cpuUsagePercent) }}</strong>
                    </div>
                  </div>
                  <div class="mini-gauge">
                    <div class="gauge-bar" :style="{ width: formatPercent(computeHeapRatio(module.metrics)), backgroundColor: getMetricTone(computeHeapRatio(module.metrics)) }"></div>
                    <div class="gauge-labels">
                      <span>Heap</span>
                      <strong>{{ formatPercent(computeHeapRatio(module.metrics)) }}</strong>
                    </div>
                  </div>
                </div>

                <div class="detailed-metrics-grid">
                  <div class="d-metric">
                    <span class="d-label">{{ $t('metricsModuleRequests') }}</span>
                    <strong class="d-value">{{ formatInteger(module.metrics.httpRequestCount) }}</strong>
                  </div>
                  <div class="d-metric">
                    <span class="d-label">{{ $t('metricsModuleQueue') }}</span>
                    <strong class="d-value" :class="{ 'text-warn': module.metrics.asyncQueueSize > 10 }">{{ formatInteger(module.metrics.asyncQueueSize) }}</strong>
                  </div>
                </div>

                <div v-if="hasGoogleMapsMetrics(module.metrics)" class="detailed-metrics-grid googlemaps-metrics">
                  <div class="d-metric" v-if="module.metrics.googleMapsGeocodingCalls != null">
                    <span class="d-label">{{ $t('metricsGoogleMapsGeocoding') }}</span>
                    <strong class="d-value">{{ formatInteger(module.metrics.googleMapsGeocodingCalls) }}</strong>
                  </div>
                  <div class="d-metric" v-if="module.metrics.googleMapsDistanceMatrixCalls != null">
                    <span class="d-label">{{ $t('metricsGoogleMapsDistanceMatrix') }}</span>
                    <strong class="d-value">{{ formatInteger(module.metrics.googleMapsDistanceMatrixCalls) }}</strong>
                  </div>
                  <div class="d-metric" v-if="module.metrics.googleMapsDistanceCacheHits != null">
                    <span class="d-label">{{ $t('metricsGoogleMapsDistanceCacheHits') }}</span>
                    <strong class="d-value text-success">{{ formatInteger(module.metrics.googleMapsDistanceCacheHits) }}</strong>
                  </div>
                  <div class="d-metric" v-if="module.metrics.googleMapsClientMapLoads != null">
                    <span class="d-label">{{ $t('metricsGoogleMapsClientMapLoads') }}</span>
                    <strong class="d-value">{{ formatInteger(module.metrics.googleMapsClientMapLoads) }}</strong>
                  </div>
                  <div class="d-metric" v-if="module.metrics.googleMapsClientPlacesCalls != null">
                    <span class="d-label">{{ $t('metricsGoogleMapsClientPlacesCalls') }}</span>
                    <strong class="d-value">{{ formatInteger(module.metrics.googleMapsClientPlacesCalls) }}</strong>
                  </div>
                  <div class="d-metric" v-if="module.metrics.googleMapsClientRouteCalls != null">
                    <span class="d-label">{{ $t('metricsGoogleMapsClientRouteCalls') }}</span>
                    <strong class="d-value">{{ formatInteger(module.metrics.googleMapsClientRouteCalls) }}</strong>
                  </div>
                  <div class="d-metric" v-if="module.metrics.googleMapsClientGeocodingCalls != null">
                    <span class="d-label">{{ $t('metricsGoogleMapsClientGeocodingCalls') }}</span>
                    <strong class="d-value">{{ formatInteger(module.metrics.googleMapsClientGeocodingCalls) }}</strong>
                  </div>
                </div>

                <div v-if="module.logInsights" class="log-insights-mini">
                  <span class="log-badge warn" v-if="module.logInsights.warnCountLastHour">{{ module.logInsights.warnCountLastHour }} W</span>
                  <span class="log-badge error" v-if="module.logInsights.errorCountLastHour">{{ module.logInsights.errorCountLastHour }} E</span>
                  <div class="category-badges">
                    <span v-for="cat in module.topCategories" :key="cat.label" class="cat-pill">
                      {{ cat.label }}
                    </span>
                  </div>
                </div>
              </div>
              <div v-else class="module-empty-state">
                <span class="material-symbols-outlined">cloud_off</span>
                <p>{{ $t('metricsModuleUnavailable') }}</p>
              </div>
            </PremiumDashboardCard>
          </div>
        </section>

        <section class="intelligence-column">
          <PremiumDashboardCard
            :title="$t('metricsAlertsTitle')"
            variant="flat"
            tone="rose"
            class="alerts-card"
          >
            <div v-if="currentAlerts.length" class="alerts-stack">
              <div v-for="alert in currentAlerts" :key="alert" class="alert-item">
                <span class="material-symbols-outlined">warning</span>
                <span>{{ alert }}</span>
              </div>
            </div>
            <div v-else class="empty-intelligence">
              <span class="material-symbols-outlined">verified</span>
              <p>{{ $t('metricsAlertNone') }}</p>
            </div>
          </PremiumDashboardCard>

          <PremiumDashboardCard
            :title="$t('metricsRecommendationsTitle')"
            variant="glass"
            tone="amber"
            class="recommendations-card"
          >
            <div v-if="recommendations.length" class="rec-stack">
              <div v-for="rec in recommendations" :key="rec" class="rec-item">
                <span class="material-symbols-outlined">lightbulb</span>
                <p>{{ rec }}</p>
              </div>
            </div>
            <div v-else class="empty-intelligence">
              <span class="material-symbols-outlined">thumb_up</span>
              <p>{{ $t('metricsRecommendationHealthy') }}</p>
            </div>
          </PremiumDashboardCard>

          <PremiumDashboardCard
            :title="$t('metricsLogRecentEvents')"
            variant="glass"
            tone="slate"
            class="logs-technical-card"
            no-padding
          >
            <template #actions>
              <div class="log-filters-premium">
                <button
                  v-for="filter in logFilters"
                  :key="filter.value"
                  class="log-filter-btn"
                  :class="{ active: logLevelFilter === filter.value }"
                  @click="logLevelFilter = filter.value"
                >
                  {{ filter.label }}
                </button>
              </div>
            </template>
            
            <div class="technical-log-container">
              <div v-if="filteredRecentLogEvents.length" class="log-rows">
                <div v-for="event in filteredRecentLogEvents" :key="event.key" class="log-row-item">
                  <span class="log-time">{{ formatTimeOnly(event.timestamp) }}</span>
                  <span class="log-lvl" :class="event.levelClass">{{ event.level }}</span>
                  <span class="log-msg">{{ event.label }}</span>
                </div>
              </div>
              <div v-else class="empty-logs">
                <p>{{ $t('metricsLogNoEvents') }}</p>
              </div>
            </div>
          </PremiumDashboardCard>
        </section>
      </div>

      <!-- Analysis Charts Grid -->
      <section class="metrics-charts-section">
        <div class="section-title-row">
          <h2>{{ $t('metricsChartConnections') }} & Analysis</h2>
        </div>
        <div class="charts-premium-grid">
          <DashboardTrendChart
            v-for="chart in mainChartConfigs"
            :key="chart.key"
            :title="chart.title"
            :subtitle="chart.subtitle"
            :points="chart.points"
            :tone="chart.tone"
            :formatter="chart.formatter"
            :empty-label="$t('metricsChartEmpty')"
          />
        </div>
      </section>
    </template>
  </div>
</template>

<script>
import http from '@/config/httpInterceptor';
import DashboardTrendChart from '../components/DashboardTrendChart.vue';
import PremiumDashboardCard from '../components/PremiumDashboardCard.vue';
import { getAccessToken, hasValidAccessToken } from '../config/auth';

const HISTORY_STORAGE_KEY = 'qd-admin-metrics-history-v1';
const HISTORY_RETENTION_MS = 7 * 24 * 60 * 60 * 1000;
const POLL_INTERVAL_MS = 5 * 60 * 1000;
const PACKAGE_STATUSES = ['NEW', 'PAYMENTPENDING', 'RESERVED', 'PICKEDUP', 'INDELIVERY', 'DELIVERED'];
const MODULE_CONFIG = [
  { key: 'gateway', titleKey: 'dashboardMetricsGatewayService', color: '#6366f1' },
  { key: 'users', titleKey: 'dashboardMetricsUsersService', color: '#f59e0b' },
  { key: 'packages', titleKey: 'dashboardMetricsPackagesService', color: '#10b981' },
  { key: 'tracking', titleKey: 'dashboardMetricsTrackingService', color: '#0ea5e9' },
  { key: 'oauth', titleKey: 'dashboardMetricsOauthService', color: '#8b5cf6' },
  { key: 'redis', titleKey: 'dashboardMetricsRedisService', color: '#ef4444' },
];

export default {
  name: 'MetricsDashboardPage',
  components: {
    DashboardTrendChart,
    PremiumDashboardCard,
  },
  data() {
    return {
      isLoading: false,
      loadError: false,
      lastRefreshAt: null,
      currentSnapshot: null,
      history: [],
      refreshTimer: null,
      logInsights: {
        gateway: null,
        users: null,
        packages: null,
      },
      httpBreakdown: {
        gateway: null,
        users: null,
        packages: null,
      },
      logLevelFilter: 'ALL',
      currentYear: new Date().getFullYear(),
    };
  },
  computed: {
    currentLocale() {
      const locale = this.$i18n?.locale;
      return typeof locale === 'string' ? locale : locale?.value || 'fr';
    },
    populationStats() {
      return [
        { label: this.$t('metricsRegisteredCustomers'), value: this.currentPopulation.registeredCustomers, icon: 'group', tone: 'indigo' },
        { label: this.$t('metricsConnectedCustomers'), value: this.currentPopulation.connectedCustomers, icon: 'person_add', tone: 'emerald' },
        { label: this.$t('metricsRegisteredDeliveryPersons'), value: this.currentPopulation.registeredDeliveryPersons, icon: 'delivery_dining', tone: 'amber' },
        { label: this.$t('metricsConnectedDeliveryPersons'), value: this.currentPopulation.connectedDeliveryPersons, icon: 'directions_bike', tone: 'rose' },
      ];
    },
    currentOperations() {
      return this.currentSnapshot?.operations || {
        pendingUsersTotal: 0,
        pendingDocumentsTotal: 0,
        packageCounts: this.emptyPackageCounts(),
      };
    },
    currentPopulation() {
      return this.currentSnapshot?.population || {
        registeredCustomers: 0,
        registeredDeliveryPersons: 0,
        connectedCustomers: 0,
        connectedDeliveryPersons: 0,
      };
    },
    moduleCards() {
      return MODULE_CONFIG.map((moduleConfig) => {
        const moduleSnapshot = this.currentSnapshot?.modules?.[moduleConfig.key] || null;
        const availability = this.computeAvailability(moduleConfig.key);
        return {
          key: moduleConfig.key,
          title: this.$t(moduleConfig.titleKey),
          metrics: moduleSnapshot?.metrics || null,
          logInsights: this.logInsights[moduleConfig.key] || null,
          heartbeatAt: moduleSnapshot?.timestamp || null,
          availability,
          statusLabel: this.resolveModuleStatusLabel(moduleSnapshot, availability),
          statusClass: this.resolveModuleStatusClass(moduleSnapshot, availability),
          topCategories: this.topCategoriesForModule(moduleConfig.key),
        };
      });
    },
    healthyModuleCount() {
      return this.moduleCards.filter((module) => module.metrics).length;
    },
    overallStatus() {
      if (this.moduleCards.some((module) => !module.metrics)) {
        return { label: this.$t('metricsHealthCritical'), tone: 'tone-rose' };
      }
      if (this.currentAlerts.length > 0) {
        return { label: this.$t('metricsHealthWarn'), tone: 'tone-amber' };
      }
      return { label: this.$t('metricsHealthOk'), tone: 'tone-emerald' };
    },
    lastHourTrafficTotal() {
      const series = this.trafficSeries;
      return series.reduce((sum, s) => sum + Number(s.values?.[s.values.length - 1] || 0), 0);
    },
    mainChartConfigs() {
      return [
        { key: 'conn', title: this.$t('metricsChartConnections'), subtitle: this.$t('metricsConnectionsSubtitle'), points: this.buildPointsFromSeries(this.connectionSeries[0]), tone: '#6366f1', formatter: this.formatInteger },
        { key: 'cpu', title: this.$t('metricsChartCpu'), subtitle: this.$t('metricsHistorySubtitle'), points: this.buildPointsFromSeries(this.averageCpuSeries), tone: '#f59e0b', formatter: this.formatPercent },
        { key: 'traffic', title: this.$t('metricsChartTraffic'), subtitle: this.$t('metricsHistorySubtitle'), points: this.buildPointsFromSeries(this.totalTrafficSeries), tone: '#10b981', formatter: this.formatInteger },
        { key: 'errors', title: this.$t('metricsChartErrors'), subtitle: this.$t('metricsLogSignalsSubtitle'), points: this.buildPointsFromSeries(this.totalErrorSeries), tone: '#ef4444', formatter: this.formatInteger },
      ];
    },
    trafficSeries() {
      return this.buildSeries((snapshot, moduleKey) => snapshot.modules?.[moduleKey]?.httpDelta || 0, 'sum');
    },
    connectionSeries() {
      return [
        {
          key: 'connected-customers',
          label: this.$t('metricsConnectedCustomers'),
          color: '#6366f1',
          values: this.buildPopulationSeries('connectedCustomers'),
        },
      ];
    },
    averageCpuSeries() {
       const raw = this.buildSeries((snapshot, moduleKey) => snapshot.modules?.[moduleKey]?.metrics?.cpuUsagePercent || 0, 'avg');
       // Flatten to a single series of averages
       const values = this.hourLabels.map((_, i) => {
         const nonZero = raw.map(s => s.values[i]).filter(v => v > 0);
         return nonZero.length ? nonZero.reduce((a,b) => a+b, 0) / nonZero.length : 0;
       });
       return { label: 'CPU Avg', values };
    },
    totalTrafficSeries() {
      const raw = this.trafficSeries;
      const values = this.hourLabels.map((_, i) => raw.reduce((sum, s) => sum + (s.values[i] || 0), 0));
      return { label: 'Traffic Total', values };
    },
    totalErrorSeries() {
      const raw = this.buildLogSeries('errorCount');
      const values = this.hourLabels.map((_, i) => raw.reduce((sum, s) => sum + (s.values[i] || 0), 0));
      return { label: 'Errors Total', values };
    },
    currentAlerts() {
      const alerts = [];
      this.moduleCards.forEach((module) => {
        if (!module.metrics) {
          alerts.push(`${module.title}: ${this.$t('metricsModuleUnavailable')}`);
          return;
        }
        const heapRatio = this.computeHeapRatio(module.metrics);
        if (module.metrics.cpuUsagePercent >= 80) alerts.push(`${module.title}: CPU ${this.formatPercent(module.metrics.cpuUsagePercent)}`);
        if (heapRatio >= 85) alerts.push(`${module.title}: Heap ${this.formatPercent(heapRatio)}`);
        if (module.metrics.asyncQueueSize >= 15) alerts.push(`${module.title}: ${this.$t('metricsModuleQueue')} ${this.formatInteger(module.metrics.asyncQueueSize)}`);
        if ((module.logInsights?.errorCountLastHour || 0) >= 5) alerts.push(`${module.title}: ${this.$t('metricsLogErrorLastHour')} ${this.formatInteger(module.logInsights.errorCountLastHour)}`);
      });
      return alerts;
    },
    recommendations() {
      const actions = [];
      if (this.moduleCards.some((module) => !module.metrics)) actions.push('Check Discovery Service & Gateway availability immediately.');
      if (this.moduleCards.some((module) => (module.metrics?.cpuUsagePercent || 0) >= 80)) actions.push('High CPU detected on certain clusters. Consider horizontal scaling path.');
      if (this.currentOperations.pendingUsersTotal >= 20) actions.push('User validation queue is high. Assign more administrators.');
      return actions;
    },
    logFilters() {
      return [
        { value: 'ALL', label: this.$t('metricsLogFilterAll') },
        { value: 'WARN', label: this.$t('metricsLogFilterWarning') },
        { value: 'ERROR', label: this.$t('metricsLogFilterError') },
      ];
    },
    hourLabels() {
      const labels = [];
      const now = new Date();
      for (let index = 23; index >= 0; index -= 1) {
        const date = new Date(now.getTime() - (index * 60 * 60 * 1000));
        labels.push(`${String(date.getHours()).padStart(2, '0')}h`);
      }
      return labels;
    },
    hourlyBuckets() {
      const now = new Date();
      return Array.from({ length: 24 }, (_, index) => {
        const start = new Date(now.getFullYear(), now.getMonth(), now.getDate(), now.getHours() - (23 - index), 0, 0, 0);
        const end = new Date(start.getTime() + (60 * 60 * 1000));
        return { start: start.getTime(), end: end.getTime() };
      });
    },
    recentLogEvents() {
      return MODULE_CONFIG.flatMap((moduleConfig) =>
        (this.logInsights[moduleConfig.key]?.recentEvents || []).map((event, index) => ({
          key: `${moduleConfig.key}-${event.timestamp}-${index}`,
          label: `${this.$t(moduleConfig.titleKey)} · ${event.category} · ${event.message}`,
          level: event.level,
          levelClass: event.level === 'ERROR' ? 'is-error' : 'is-warn',
          timestamp: event.timestamp || 0,
        }))
      )
        .sort((left, right) => right.timestamp - left.timestamp)
        .slice(0, 15);
    },
    filteredRecentLogEvents() {
      if (this.logLevelFilter === 'ALL') return this.recentLogEvents;
      return this.recentLogEvents.filter((event) => event.level === this.logLevelFilter);
    },
  },
  mounted() {
    this.loadStoredHistory();
    if (hasValidAccessToken()) {
      this.refreshSnapshot();
      this.startRefreshTimer();
    }
  },
  beforeUnmount() {
    this.stopRefreshTimer();
    this.persistHistory();
  },
  methods: {
    startRefreshTimer() {
      this.stopRefreshTimer();
      this.refreshTimer = window.setInterval(() => this.refreshSnapshot(), POLL_INTERVAL_MS);
    },
    stopRefreshTimer() {
      if (this.refreshTimer) {
        window.clearInterval(this.refreshTimer);
        this.refreshTimer = null;
      }
    },
    async refreshSnapshot() {
      if (!hasValidAccessToken()) return;
      this.isLoading = !this.currentSnapshot;
      try {
        const token = getAccessToken();
        const headers = { Authorization: `Bearer ${token}` };
        const requests = await Promise.allSettled([
          http.get(`${this.$i18n.t('gatewayRootURL')}${this.$i18n.t('getAdminGatewayMetrics')}`, { headers }),
          http.get(`${this.$i18n.t('gatewayRootURL')}${this.$i18n.t('getAdminAggregatedUserMetrics')}`, { headers }),
          http.get(`${this.$i18n.t('gatewayRootURL')}${this.$i18n.t('getAdminAggregatedPackageMetrics')}`, { headers }),
          http.get(`${this.$i18n.t('gatewayRootURL')}${this.$i18n.t('getAdminAggregatedTrackingMetrics')}`, { headers }),
          http.get(`${this.$i18n.t('gatewayRootURL')}${this.$i18n.t('getAdminOauthMetrics')}`, { headers }),
          http.get(`${this.$i18n.t('gatewayRootURL')}${this.$i18n.t('getAdminRedisMetrics')}`, { headers }),
          http.get(`${this.$i18n.t('userRootURL')}${this.$i18n.t('getAdminUserOverview')}`, { headers }),
          http.get(`${this.$i18n.t('gatewayRootURL')}${this.$i18n.t('getAdminGatewayLogInsights')}`, { headers }),
          http.get(`${this.$i18n.t('gatewayRootURL')}${this.$i18n.t('getAdminAggregatedUserLogInsights')}`, { headers }),
          http.get(`${this.$i18n.t('gatewayRootURL')}${this.$i18n.t('getAdminAggregatedPackageLogInsights')}`, { headers }),
          http.get(`${this.$i18n.t('userRootURL')}${this.$i18n.t('getUsersForValidation')}?page=0&size=5`, { headers }),
        ]);

        const timestamp = Date.now();
        const modules = {
          gateway: this.toModuleSnapshot(requests[0], timestamp),
          users: this.toModuleSnapshot(requests[1], timestamp),
          packages: this.toModuleSnapshot(requests[2], timestamp),
          tracking: this.toModuleSnapshot(requests[3], timestamp),
          oauth: this.toModuleSnapshot(requests[4], timestamp),
          redis: this.toModuleSnapshot(requests[5], timestamp),
        };

        this.logInsights = {
          gateway: requests[7].status === 'fulfilled' ? requests[7].value.data : null,
          users: requests[8].status === 'fulfilled' ? requests[8].value.data?.summary : null,
          packages: requests[9].status === 'fulfilled' ? requests[9].value.data?.summary : null,
        };

        const population = requests[6].status === 'fulfilled' ? requests[6].value.data : {};
        const validation = requests[10].status === 'fulfilled' ? requests[10].value.data : {};

        const snapshot = {
          timestamp,
          modules: this.attachCounterDeltas(modules),
          operations: {
            pendingUsersTotal: Number(validation?.totalItems || 0),
          },
          population: {
            registeredCustomers: Number(population?.registeredCustomers || 0),
            registeredDeliveryPersons: Number(population?.registeredDeliveryPersons || 0),
            connectedCustomers: Number(population?.connectedCustomers || 0),
            connectedDeliveryPersons: Number(population?.connectedDeliveryPersons || 0),
          },
        };

        this.currentSnapshot = snapshot;
        this.lastRefreshAt = timestamp;
        this.appendHistory(snapshot);
      } catch (error) {
        this.loadError = true;
        console.error('Metrics Refresh Failure', error);
      } finally {
        this.isLoading = false;
      }
    },
    toModuleSnapshot(result, timestamp) {
      if (result.status !== 'fulfilled') return { timestamp, metrics: null, httpDelta: 0 };
      const data = result.value.data?.summary || result.value.data;
      return { timestamp, metrics: data || null, httpDelta: 0 };
    },
    attachCounterDeltas(modules) {
      const prev = this.history[this.history.length - 1] || this.currentSnapshot;
      return Object.entries(modules).reduce((acc, [k, v]) => {
        const prevM = prev?.modules?.[k]?.metrics;
        acc[k] = { ...v, httpDelta: Math.max(0, (v.metrics?.httpRequestCount || 0) - (prevM?.httpRequestCount || 0)) };
        return acc;
      }, {});
    },
    appendHistory(snapshot) {
      this.history = [...this.history, snapshot].filter(h => h.timestamp >= Date.now() - HISTORY_RETENTION_MS).slice(-2000);
      this.persistHistory();
    },
    loadStoredHistory() {
      try { this.history = JSON.parse(localStorage.getItem(HISTORY_STORAGE_KEY)) || []; } catch { this.history = []; }
    },
    persistHistory() {
      try { localStorage.setItem(HISTORY_STORAGE_KEY, JSON.stringify(this.history)); } catch (e) { /* ignore */ }
    },
    computeAvailability(key) {
      const relevant = this.history.filter(h => h.timestamp >= Date.now() - 86400000);
      if (!relevant.length) return null;
      return (relevant.filter(h => !!h.modules?.[key]?.metrics).length / relevant.length) * 100;
    },
    resolveModuleStatusLabel(snap, avail) {
      if (!snap?.metrics) return this.$t('metricsHealthCritical');
      if ((avail || 100) < 95 || this.computeHeapRatio(snap.metrics) >= 85) return this.$t('metricsHealthWarn');
      return this.$t('metricsHealthOk');
    },
    resolveModuleStatusClass(snap, avail) {
      if (!snap?.metrics) return 'status-critical';
      if ((avail || 100) < 95 || this.computeHeapRatio(snap.metrics) >= 85) return 'status-warn';
      return 'status-ok';
    },
    computeHeapRatio(m) {
      if (!m || !m.heapMaxMb) return 0;
      return (Number(m.heapUsedMb) / Number(m.heapMaxMb)) * 100;
    },
    hasGoogleMapsMetrics(metrics) {
      return [
        'googleMapsGeocodingCalls',
        'googleMapsDistanceMatrixCalls',
        'googleMapsDistanceCacheHits',
        'googleMapsClientMapLoads',
        'googleMapsClientPlacesCalls',
        'googleMapsClientRouteCalls',
        'googleMapsClientGeocodingCalls',
      ].some((key) => metrics?.[key] != null);
    },
    buildSeries(selector, aggregation) {
      return MODULE_CONFIG.map(cfg => ({
        key: cfg.key,
        label: this.$t(cfg.titleKey),
        values: this.hourlyBuckets.map(b => {
          const vals = this.history.filter(h => h.timestamp >= b.start && h.timestamp < b.end).map(h => Number(selector(h, cfg.key) || 0));
          if (!vals.length) return 0;
          return aggregation === 'sum' ? vals.reduce((a,b)=>a+b,0) : vals.reduce((a,b)=>a+b,0)/vals.length;
        })
      }));
    },
    buildLogSeries(field) {
      return MODULE_CONFIG.map(cfg => ({
        label: this.$t(cfg.titleKey),
        values: this.hourLabels.map(lbl => Number(this.logInsights[cfg.key]?.hourlyCounts?.find(h => h.hourLabel === lbl)?.[field] || 0))
      }));
    },
    buildPopulationSeries(field) {
      return this.hourlyBuckets.map(b => {
        const vals = this.history.filter(h => h.timestamp >= b.start && h.timestamp < b.end).map(h => Number(h.population?.[field] || 0));
        return vals.length ? vals[vals.length - 1] : 0;
      });
    },
    buildPointsFromSeries(series) {
      const max = Math.max(...series.values, 0.1);
      return series.values.map((v, i) => ({
        label: this.hourLabels[i],
        value: v,
        x: 32 + (i * 12), // Compact spacing for many points
        y: 180 - ((v/max) * 140) - 20
      }));
    },
    topCategoriesForModule(key) {
      return Object.entries(this.logInsights[key]?.categoryCounts || {}).sort((a,b)=>b[1]-a[1]).slice(0, 2).map(e => ({ label: e[0] }));
    },
    getMetricTone(val) {
      if (val >= 85) return '#ef4444';
      if (val >= 70) return '#f59e0b';
      return '#10b981';
    },
    formatInteger(v) { return new Intl.NumberFormat(this.currentLocale).format(v || 0); },
    formatPercent(v) { return `${Number(v || 0).toFixed(1)}%`; },
    formatTimeAgo(t) {
      if (!t) return 'Offline';
      const m = Math.round((Date.now() - t) / 60000);
      return m < 1 ? 'Live' : `${m}m ago`;
    },
    formatDateTime(t) {
      if (!t) return '-';
      return new Intl.DateTimeFormat(this.currentLocale, {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
      }).format(new Date(t));
    },
    formatTimeOnly(t) { return new Intl.DateTimeFormat(this.currentLocale, { hour: '2-digit', minute: '2-digit', second: '2-digit' }).format(new Date(t)); },
    emptyPackageCounts() { return PACKAGE_STATUSES.reduce((a, s) => ({ ...a, [s]: 0 }), {}); }
  }
};
</script>

<style scoped>
.hero-header-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
}

.status-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
}

.status-pulse {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: currentColor;
  animation: qd-glow 1.5s infinite;
}

@keyframes qd-glow {
  0% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.4; transform: scale(1.4); }
  100% { opacity: 1; transform: scale(1); }
}

.hero-system-stats {
  display: flex;
  gap: 32px;
  padding: 16px 24px;
  background: var(--qd-primary-dark);
  border-radius: 20px;
  color: #fff;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.15);
}

.system-stat-item {
  display: flex;
  flex-direction: column;
}

.item-label {
  font-size: 0.68rem;
  color: rgba(255, 255, 255, 0.5);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 2px;
}

.item-value {
  font-size: 1.4rem;
  font-weight: 800;
}

.system-stat-item.warn .item-value {
  color: #fbbf24;
}

/* Grids */
.population-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.stat-container {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  font-size: 32px;
  padding: 12px;
  background: rgba(15, 23, 42, 0.05);
  border-radius: 16px;
  color: #64748b;
}

.stat-value {
  display: block;
  font-size: 1.5rem;
  font-weight: 800;
  color: #0f172a;
}

.stat-label {
  font-size: 0.8125rem;
  color: #64748b;
}

.metrics-main-grid {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: 32px;
}

.section-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-title-row h2 {
  font-size: 1.25rem;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
}

.service-legend {
  font-size: 0.75rem;
  color: #64748b;
  display: flex;
  gap: 12px;
}

.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}

.legend-dot.ok { background: #10b981; }
.legend-dot.warn { background: #f59e0b; }
.legend-dot.critical { background: #ef4444; }

.module-cards-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.module-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.module-title-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.status-indicator {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.status-indicator.status-ok { background: #10b981; box-shadow: 0 0 8px rgba(16, 185, 129, 0.4); }
.status-indicator.status-warn { background: #f59e0b; box-shadow: 0 0 8px rgba(245, 158, 11, 0.4); }
.status-indicator.status-critical { background: #ef4444; box-shadow: 0 0 8px rgba(239, 68, 68, 0.4); }

.heartbeat {
  font-size: 0.75rem;
  color: #94a3b8;
}

.module-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.main-metrics-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.mini-gauge {
  background: rgba(15, 23, 42, 0.03);
  padding: 12px;
  border-radius: 12px;
  position: relative;
  overflow: hidden;
}

.gauge-bar {
  position: absolute;
  bottom: 0;
  left: 0;
  height: 4px;
  transition: width 0.5s ease;
}

.gauge-labels {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}

.gauge-labels span { font-size: 0.6875rem; color: #64748b; text-transform: uppercase; font-weight: 700; }
.gauge-labels strong { font-size: 1rem; color: #0f172a; }

.detailed-metrics-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.d-metric {
  background: #fff;
  border: 1px solid rgba(15, 23, 42, 0.05);
  padding: 10px;
  border-radius: 10px;
}

.d-label { font-size: 0.625rem; color: #94a3b8; text-transform: uppercase; display: block; margin-bottom: 2px; }
.d-value { font-size: 0.9375rem; color: #334155; font-weight: 700; }
.d-value.text-success { color: #10b981; }

.googlemaps-metrics {
  margin-top: 6px;
  padding-top: 6px;
  border-top: 1px dashed rgba(15, 23, 42, 0.08);
}

.log-insights-mini {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.log-badge {
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.6875rem;
  font-weight: 800;
}

.log-badge.warn { background: #fef3c7; color: #92400e; }
.log-badge.error { background: #fee2e2; color: #991b1b; }

.cat-pill {
  font-size: 0.625rem;
  padding: 2px 8px;
  background: #f1f5f9;
  border-radius: 999px;
  color: #64748b;
}

/* intelligence Column */
.intelligence-column {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.alerts-stack, .rec-stack {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.alert-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: rgba(239, 68, 68, 0.05);
  border-left: 4px solid #ef4444;
  border-radius: 8px;
  font-size: 0.875rem;
  color: #991b1b;
}

.alert-item span.material-symbols-outlined { color: #ef4444; }

.rec-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  background: rgba(245, 158, 11, 0.05);
  border-radius: 8px;
  font-size: 0.875rem;
}

.rec-item p { margin: 0; color: #92400e; }

.technical-log-container {
  background: #0f172a;
  height: 400px;
  overflow-y: auto;
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  font-size: 0.75rem;
  padding: 16px;
}

.log-row-item {
  display: flex;
  gap: 12px;
  padding: 4px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.log-time { color: #4ade80; opacity: 0.8; }
.log-lvl { font-weight: 800; min-width: 45px; }
.log-lvl.is-warn { color: #fbbf24; }
.log-lvl.is-error { color: #f87171; }
.log-msg { color: #cbd5e1; }

.log-filters-premium {
  display: flex;
  gap: 4px;
}

.log-filter-btn {
  background: none;
  border: none;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 0.6875rem;
  font-weight: 700;
  color: #64748b;
  cursor: pointer;
}

.log-filter-btn.active {
  background: #0f172a;
  color: #fff;
}

/* Charts Section */
.metrics-charts-section {
  margin-top: 24px;
}

.charts-premium-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
}

/* Loading/Error States */
.loading-state, .error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100px;
  color: #64748b;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid rgba(15, 23, 42, 0.1);
  border-top-color: #6366f1;
  border-radius: 50%;
  animation: qd-spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes qd-spin { to { transform: rotate(360deg); } }

@media (max-width: 1400px) {
  .metrics-main-grid { grid-template-columns: 1fr; }
  .intelligence-column { grid-template-columns: repeat(2, 1fr); }
  .logs-technical-card { grid-column: span 2; }
}

@media (max-width: 900px) {
  .population-grid { grid-template-columns: repeat(2, 1fr); }
  .intelligence-column { grid-template-columns: 1fr; }
  .logs-technical-card { grid-column: span 1; }
  .charts-premium-grid { grid-template-columns: 1fr; }
}

@media (max-width: 900px) {
  .hero-system-stats { gap: 12px; padding: 12px; }
  .item-value { font-size: 1.5rem; }
}

@media (max-width: 768px) {
  .metrics-dashboard-page { 
    padding: 12px; 
    gap: 14px; 
    width: 100%; 
    max-width: 100vw !important;
    box-sizing: border-box; 
    overflow-x: hidden !important; 
  }
  
  .premium-hero { flex-direction: column; align-items: stretch; gap: 12px; width: 100%; }
  .hero-content h1 { font-size: 1.75rem; overflow-wrap: break-word; line-height: 1.1; }
  
  .hero-system-stats { 
    flex-direction: column; 
    gap: 12px; 
    padding: 12px;
    align-items: stretch;
    text-align: center;
    width: 100%;
    box-sizing: border-box;
    border-radius: 14px;
    min-width: 0;
  }
  .system-stat-item { width: 100%; border-bottom: 1px solid rgba(255, 255, 255, 0.05); padding-bottom: 10px; }
  .system-stat-item:last-child { border-bottom: none; padding-bottom: 0; }
  
  .population-grid { 
    grid-template-columns: repeat(2, 1fr); 
    gap: 8px; 
    width: 100%; 
    display: grid !important; 
    min-width: 0;
  }
  .pop-card { min-width: 0; }
  .stat-container { gap: 8px; flex-direction: column; text-align: center; justify-content: center; min-width: 0; }
  .stat-copy { min-width: 0; width: 100%; }
  .stat-value { font-size: 1.1rem; }
  .stat-label { font-size: 0.65rem; word-break: break-word; white-space: normal; }
  .stat-icon { font-size: 20px; padding: 6px; margin-bottom: 4px; }
  
  .module-cards-container { 
    display: flex !important;
    flex-direction: column;
    gap: 12px;
    width: 100%; 
    grid-template-columns: 1fr; 
  }
  
  .metrics-main-grid { 
    display: flex !important;
    flex-direction: column;
    gap: 14px; 
    width: 100%; 
    grid-template-columns: 1fr;
  }
  
  .intelligence-column { 
    display: flex !important; 
    flex-direction: column; 
    gap: 12px; 
    width: 100%; 
  }
  
  .module-premium-card, .alerts-card, .recommendations-card, .logs-technical-card {
    width: 100% !important;
    min-width: 0 !important;
    max-width: 100% !important;
    box-sizing: border-box;
  }
  
  .services-column { width: 100%; min-width: 0; max-width: 100%; }
  
  .module-card-header h3 { font-size: 0.95rem; }
  .main-metrics-row { grid-template-columns: 1fr; gap: 12px; }
  .detailed-metrics-grid { grid-template-columns: 1fr; gap: 8px; }
  
  .technical-log-container {
    width: 100% !important;
    min-width: 0 !important;
    box-sizing: border-box;
  }
  .log-row-item {
    word-break: break-all !important;
    white-space: normal !important;
  }
  .log-msg { word-break: break-all !important; white-space: normal !important; flex: 1; min-width: 0; }
  
  .charts-premium-grid { 
    display: flex !important;
    flex-direction: column;
    gap: 12px;
  }

  .hero-content h1 {
    font-size: 1.5rem;
  }

  .hero-system-stats {
    flex-wrap: wrap;
    gap: 10px;
    padding: 12px;
  }

  .population-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  .module-cards-container {
    grid-template-columns: 1fr;
  }

  .charts-premium-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 480px) {
  .population-grid { grid-template-columns: repeat(2, 1fr); }
  .hero-content h1 { font-size: 1.5rem; }
  .hero-header-row { flex-direction: column; align-items: flex-start; gap: 8px; }
  .refresh-time { font-size: 0.7rem; }
}
</style>
