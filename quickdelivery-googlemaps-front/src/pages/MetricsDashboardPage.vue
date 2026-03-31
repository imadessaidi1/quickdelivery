<template>
  <div class="metrics-dashboard-page">
    <template v-if="isLoading && !currentSnapshot">
      <section class="hero-card">
        <div>
          <span class="hero-chip">{{ $t('menuAdminMetrics') }}</span>
          <h1>{{ $t('metricsPageTitle') }}</h1>
          <p>{{ $t('metricsPageSubtitle') }}</p>
        </div>
      </section>
      <div class="page-state">{{ $t('stateLoading') }}</div>
    </template>

    <template v-else-if="loadError && !currentSnapshot">
      <section class="hero-card">
        <div>
          <span class="hero-chip">{{ $t('menuAdminMetrics') }}</span>
          <h1>{{ $t('metricsPageTitle') }}</h1>
          <p>{{ $t('metricsPageSubtitle') }}</p>
        </div>
      </section>
      <div class="page-state error">{{ $t('stateLoadError') }}</div>
    </template>

    <template v-else>
      <section class="hero-card">
        <div>
          <span class="hero-chip">{{ $t('menuAdminMetrics') }}</span>
          <h1>{{ $t('metricsPageTitle') }}</h1>
          <p>{{ $t('metricsPageSubtitle') }}</p>
        </div>
        <div class="hero-side">
          <strong>{{ overallStatus.label }}</strong>
          <span>{{ $t('metricsPageLastRefresh') }}: {{ formatDateTime(lastRefreshAt) }}</span>
          <small>{{ $t('metricsPageAutoRefresh') }}</small>
        </div>
      </section>

      <section class="population-grid">
        <article class="stat-card tone-dark">
          <strong>{{ formatInteger(currentPopulation.registeredCustomers) }}</strong>
          <span>{{ $t('metricsRegisteredCustomers') }}</span>
        </article>
        <article class="stat-card tone-success">
          <strong>{{ formatInteger(currentPopulation.connectedCustomers) }}</strong>
          <span>{{ $t('metricsConnectedCustomers') }}</span>
        </article>
        <article class="stat-card tone-dark">
          <strong>{{ formatInteger(currentPopulation.registeredDeliveryPersons) }}</strong>
          <span>{{ $t('metricsRegisteredDeliveryPersons') }}</span>
        </article>
        <article class="stat-card tone-success">
          <strong>{{ formatInteger(currentPopulation.connectedDeliveryPersons) }}</strong>
          <span>{{ $t('metricsConnectedDeliveryPersons') }}</span>
        </article>
      </section>

      <section class="summary-grid">
        <article class="stat-card tone-dark">
          <strong>{{ healthyModuleCount }}/{{ moduleCards.length }}</strong>
          <span>{{ $t('metricsOverallHealthy') }}</span>
        </article>
        <article class="stat-card tone-warn">
          <strong>{{ currentAlerts.length }}</strong>
          <span>{{ $t('metricsOverallIncidents') }}</span>
        </article>
        <article class="stat-card tone-neutral">
          <strong>{{ currentOperations.pendingUsersTotal }}</strong>
          <span>{{ $t('metricsOverallBacklog') }}</span>
        </article>
        <article class="stat-card tone-success">
          <strong>{{ inFlightPackages }}</strong>
          <span>{{ $t('metricsOverallDeliveries') }}</span>
        </article>
        <article class="stat-card tone-dark">
          <strong>{{ formatInteger(lastHourTrafficTotal) }}</strong>
          <span>{{ $t('metricsOverallTraffic') }}</span>
        </article>
        <article class="stat-card" :class="overallStatus.tone">
          <strong>{{ overallStatus.label }}</strong>
          <span>{{ $t('metricsModuleHeartbeat') }}</span>
        </article>
      </section>

      <section class="module-grid">
        <article v-for="module in moduleCards" :key="module.key" class="panel-card module-card">
          <div class="panel-head module-head">
            <div>
              <h2>{{ module.title }}</h2>
              <p :class="module.statusClass">{{ module.statusLabel }}</p>
            </div>
            <strong>{{ formatDateTime(module.heartbeatAt) }}</strong>
          </div>

          <div v-if="module.metrics" class="metrics-list">
            <div class="metric-row">
              <strong>{{ $t('metricsModuleHeartbeat') }}</strong>
              <span>{{ formatTimeAgo(module.heartbeatAt) }}</span>
            </div>
            <div class="metric-row">
              <strong>{{ $t('metricsModuleAvailability') }}</strong>
              <span>{{ formatPercent(module.availability) }}</span>
            </div>
            <div class="metric-row">
              <strong>{{ $t('dashboardMetricUptime') }}</strong>
              <span>{{ formatDuration(module.metrics.uptimeSeconds) }}</span>
            </div>
            <div class="metric-row">
              <strong>{{ $t('dashboardMetricHeap') }}</strong>
              <span>{{ formatHeap(module.metrics.heapUsedMb, module.metrics.heapMaxMb) }}</span>
            </div>
            <div class="metric-row">
              <strong>{{ $t('dashboardMetricCpu') }}</strong>
              <span>{{ formatPercent(module.metrics.cpuUsagePercent) }}</span>
            </div>
            <div class="metric-row">
              <strong>{{ $t('metricsModuleRequests') }}</strong>
              <span>{{ formatInteger(module.metrics.httpRequestCount) }}</span>
            </div>
            <div class="metric-row">
              <strong>{{ $t('metricsModuleOperations') }}</strong>
              <span>{{ formatInteger(module.metrics.operationCallCount) }}</span>
            </div>
            <div class="metric-row">
              <strong>{{ $t('metricsModuleQueue') }}</strong>
              <span>{{ formatInteger(module.metrics.asyncQueueSize) }}</span>
            </div>
            <div class="metric-row">
              <strong>{{ $t('metricsModuleThreads') }}</strong>
              <span>{{ formatInteger(module.metrics.asyncActiveCount) }}</span>
            </div>
          </div>

          <div v-if="module.logInsights" class="log-signal-block">
            <div class="log-chip-row">
              <span class="log-chip warn">{{ $t('metricsLogWarnLastHour') }}: {{ formatInteger(module.logInsights.warnCountLastHour) }}</span>
              <span class="log-chip error">{{ $t('metricsLogErrorLastHour') }}: {{ formatInteger(module.logInsights.errorCountLastHour) }}</span>
            </div>
            <div v-if="module.topCategories.length" class="category-list">
              <span v-for="category in module.topCategories" :key="`${module.key}-${category.label}`" class="category-pill">
                {{ category.label }} · {{ formatInteger(category.count) }}
              </span>
            </div>
          </div>

          <div v-if="module.instances.length" class="instance-block">
            <div class="panel-head instance-head">
              <h3>{{ $t('metricsInstancesTitle') }}</h3>
              <strong>{{ module.healthyInstances }}/{{ module.totalInstances }}</strong>
            </div>
            <div class="instance-list">
              <article
                v-for="instance in module.instances"
                :key="`${module.key}-${instance.instanceId || instance.host || instance.port}`"
                class="instance-card"
                :class="{ unavailable: !instance.available }"
              >
                <div class="instance-topline">
                  <strong>{{ formatInstanceLabel(instance) }}</strong>
                  <span :class="instance.available ? 'status-ok' : 'status-critical'">
                    {{ instance.available ? $t('metricsHealthOk') : $t('metricsInstanceUnavailable') }}
                  </span>
                </div>
                <div v-if="instance.payload" class="instance-metrics">
                  <span>{{ $t('dashboardMetricCpu') }} {{ formatPercent(instance.payload.cpuUsagePercent) }}</span>
                  <span>{{ $t('dashboardMetricHeap') }} {{ formatHeap(instance.payload.heapUsedMb, instance.payload.heapMaxMb) }}</span>
                  <span>{{ $t('metricsModuleRequests') }} {{ formatInteger(instance.payload.httpRequestCount) }}</span>
                  <span>{{ $t('metricsModuleOperations') }} {{ formatInteger(instance.payload.operationCallCount) }}</span>
                  <span>{{ $t('metricsModuleQueue') }} {{ formatInteger(instance.payload.asyncQueueSize) }}</span>
                </div>
                <div v-else class="instance-error">
                  {{ instance.errorMessage || $t('metricsInstanceUnavailable') }}
                </div>
              </article>
            </div>
          </div>
        </article>
      </section>

      <section class="dashboard-grid">
        <article class="panel-card">
          <div class="panel-head">
            <h2>{{ $t('metricsAlertsTitle') }}</h2>
            <p>{{ $t('metricsAlertsSubtitle') }}</p>
          </div>
          <ul v-if="currentAlerts.length" class="bullet-list">
            <li v-for="alert in currentAlerts" :key="alert">{{ alert }}</li>
          </ul>
          <div v-else class="empty-state">{{ $t('metricsAlertNone') }}</div>
        </article>

        <article class="panel-card">
          <div class="panel-head">
            <h2>{{ $t('metricsRecommendationsTitle') }}</h2>
            <p>{{ $t('metricsRecommendationsSubtitle') }}</p>
          </div>
          <ul v-if="recommendations.length" class="bullet-list">
            <li v-for="recommendation in recommendations" :key="recommendation">{{ recommendation }}</li>
          </ul>
          <div v-else class="empty-state">{{ $t('metricsRecommendationHealthy') }}</div>
        </article>
      </section>

      <section class="chart-grid">
        <MetricsMultiSeriesChart
          :title="$t('metricsChartConnections')"
          :subtitle="$t('metricsConnectionsSubtitle')"
          :labels="hourLabels"
          :series="connectionSeries"
          :formatter="formatInteger"
          :empty-label="$t('metricsChartEmpty')"
        />
        <MetricsBarChart
          :title="$t('metricsRequestServiceTitle')"
          :subtitle="$t('metricsRequestServiceSubtitle')"
          :items="serviceRequestItems"
          :formatter="formatInteger"
          :empty-label="$t('metricsChartEmpty')"
        />
        <MetricsBarChart
          :title="$t('metricsRequestEndpointTitle')"
          :subtitle="$t('metricsRequestEndpointSubtitle')"
          :items="endpointRequestItems"
          :formatter="formatInteger"
          :empty-label="$t('metricsChartEmpty')"
        />
        <MetricsMultiSeriesChart
          :title="$t('metricsChartCpu')"
          :subtitle="$t('metricsHistorySubtitle')"
          :labels="hourLabels"
          :series="cpuSeries"
          :formatter="formatPercent"
          :empty-label="$t('metricsChartEmpty')"
        />
        <MetricsMultiSeriesChart
          :title="$t('metricsChartHeap')"
          :subtitle="$t('metricsHistorySubtitle')"
          :labels="hourLabels"
          :series="heapSeries"
          :formatter="formatPercent"
          :empty-label="$t('metricsChartEmpty')"
        />
        <MetricsMultiSeriesChart
          :title="$t('metricsChartTraffic')"
          :subtitle="$t('metricsHistorySubtitle')"
          :labels="hourLabels"
          :series="trafficSeries"
          :formatter="formatInteger"
          :empty-label="$t('metricsChartEmpty')"
        />
        <MetricsMultiSeriesChart
          :title="$t('metricsChartQueue')"
          :subtitle="$t('metricsHistorySubtitle')"
          :labels="hourLabels"
          :series="queueSeries"
          :formatter="formatInteger"
          :empty-label="$t('metricsChartEmpty')"
        />
        <MetricsMultiSeriesChart
          :title="$t('metricsChartWarnings')"
          :subtitle="$t('metricsLogSignalsSubtitle')"
          :labels="hourLabels"
          :series="warningSeries"
          :formatter="formatInteger"
          :empty-label="$t('metricsChartEmpty')"
        />
        <MetricsMultiSeriesChart
          :title="$t('metricsChartErrors')"
          :subtitle="$t('metricsLogSignalsSubtitle')"
          :labels="hourLabels"
          :series="errorSeries"
          :formatter="formatInteger"
          :empty-label="$t('metricsChartEmpty')"
        />
      </section>

      <section class="dashboard-grid logs-grid">
        <article class="panel-card">
          <div class="panel-head">
            <h2>{{ $t('metricsLogRecentEvents') }}</h2>
            <p>{{ $t('metricsLogSignalsSubtitle') }}</p>
          </div>
          <div class="filter-row">
            <button
              v-for="filter in logFilters"
              :key="filter.value"
              type="button"
              class="filter-chip"
              :class="{ active: logLevelFilter === filter.value }"
              @click="logLevelFilter = filter.value"
            >
              {{ filter.label }}
            </button>
          </div>
          <ul v-if="filteredRecentLogEvents.length" class="bullet-list">
            <li v-for="event in filteredRecentLogEvents" :key="event.key">
              <span class="event-level" :class="event.levelClass">{{ event.level }}</span>
              {{ event.label }}
            </li>
          </ul>
          <div v-else class="empty-state">{{ $t('metricsLogNoEvents') }}</div>
        </article>
      </section>
    </template>
  </div>
</template>

<script>
import http from '@/config/httpInterceptor';
import MetricsBarChart from '../components/MetricsBarChart.vue';
import MetricsMultiSeriesChart from '../components/MetricsMultiSeriesChart.vue';
import { getAccessToken } from '../config/auth';

const HISTORY_STORAGE_KEY = 'qd-admin-metrics-history-v1';
const HISTORY_RETENTION_MS = 7 * 24 * 60 * 60 * 1000;
const POLL_INTERVAL_MS = 5 * 60 * 1000;
const PACKAGE_STATUSES = ['NEW', 'PAYMENTPENDING', 'RESERVED', 'PICKEDUP', 'INDELIVERY', 'DELIVERED'];
const MODULE_CONFIG = [
  { key: 'gateway', titleKey: 'dashboardMetricsGatewayService', color: '#1f5fae' },
  { key: 'config', titleKey: 'dashboardMetricsConfigService', color: '#5b6b82' },
  { key: 'discovery', titleKey: 'dashboardMetricsDiscoveryService', color: '#0f766e' },
  { key: 'oauth', titleKey: 'dashboardMetricsOauthService', color: '#7c3aed' },
  { key: 'redis', titleKey: 'dashboardMetricsRedisService', color: '#b91c1c' },
  { key: 'mysql', titleKey: 'dashboardMetricsMysqlService', color: '#1d4ed8' },
  { key: 'users', titleKey: 'dashboardMetricsUsersService', color: '#ef7d32' },
  { key: 'packages', titleKey: 'dashboardMetricsPackagesService', color: '#159947' },
  { key: 'tracking', titleKey: 'dashboardMetricsTrackingService', color: '#0ea5e9' },
];

export default {
  components: {
    MetricsBarChart,
    MetricsMultiSeriesChart,
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
    };
  },
  computed: {
    currentLocale() {
      const locale = this.$i18n?.locale;
      return typeof locale === 'string' ? locale : locale?.value || 'fr';
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
          instances: moduleSnapshot?.instances || [],
          totalInstances: Number(moduleSnapshot?.totalInstances || 0),
          healthyInstances: Number(moduleSnapshot?.healthyInstances || 0),
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
        return { label: this.$t('metricsHealthCritical'), tone: 'tone-warn' };
      }
      if (this.currentAlerts.length > 0) {
        return { label: this.$t('metricsHealthWarn'), tone: 'tone-neutral' };
      }
      return { label: this.$t('metricsHealthOk'), tone: 'tone-success' };
    },
    inFlightPackages() {
      const counts = this.currentOperations.packageCounts;
      return Number(counts.RESERVED || 0) + Number(counts.PICKEDUP || 0) + Number(counts.INDELIVERY || 0);
    },
    currentAlerts() {
      const alerts = [];
      this.moduleCards.forEach((module) => {
        if (!module.metrics) {
          alerts.push(`${module.title}: ${this.$t('metricsModuleUnavailable')}`);
          return;
        }
        if (module.totalInstances > 0 && module.healthyInstances < module.totalInstances) {
          alerts.push(`${module.title}: ${module.healthyInstances}/${module.totalInstances} ${this.$t('metricsInstancesTitle').toLowerCase()}`);
        }
        const heapRatio = this.computeHeapRatio(module.metrics);
        if (module.metrics.cpuUsagePercent != null && module.metrics.cpuUsagePercent >= 80) {
          alerts.push(`${module.title}: CPU ${this.formatPercent(module.metrics.cpuUsagePercent)}`);
        }
        if (heapRatio != null && heapRatio >= 85) {
          alerts.push(`${module.title}: Heap ${this.formatPercent(heapRatio)}`);
        }
        if (module.metrics.asyncQueueSize != null && module.metrics.asyncQueueSize >= 15) {
          alerts.push(`${module.title}: ${this.$t('metricsModuleQueue')} ${this.formatInteger(module.metrics.asyncQueueSize)}`);
        }
        if ((module.logInsights?.errorCountLastHour || 0) >= 5) {
          alerts.push(`${module.title}: ${this.$t('metricsLogErrorLastHour')} ${this.formatInteger(module.logInsights.errorCountLastHour)}`);
        }
      });
      if (this.currentOperations.pendingUsersTotal >= 20) {
        alerts.push(`${this.$t('dashboardStatValidationQueue')}: ${this.formatInteger(this.currentOperations.pendingUsersTotal)}`);
      }
      if (this.currentOperations.packageCounts.PAYMENTPENDING >= 20) {
        alerts.push(`${this.$t('PAYMENTPENDING')}: ${this.formatInteger(this.currentOperations.packageCounts.PAYMENTPENDING)}`);
      }
      return alerts;
    },
    recommendations() {
      const actions = [];
      if (this.moduleCards.some((module) => !module.metrics)) {
        actions.push('Stabilize discovery, deployment and startup dependencies before analyzing business performance.');
      }
      if (this.moduleCards.some((module) => module.totalInstances > 0 && module.healthyInstances < module.totalInstances)) {
        actions.push('Recover the missing service instances because the metrics center reports a partial cluster state.');
      }
      if (this.moduleCards.some((module) => (module.metrics?.cpuUsagePercent || 0) >= 80)) {
        actions.push('Inspect the busiest endpoints and worker threads during peak CPU periods to isolate expensive code paths.');
      }
      if (this.moduleCards.some((module) => this.computeHeapRatio(module.metrics) >= 85)) {
        actions.push('Review heap pressure, payload sizes and object retention on the affected service.');
      }
      if (this.moduleCards.some((module) => (module.metrics?.asyncQueueSize || 0) >= 15)) {
        actions.push('Increase throughput or move more work to dedicated workers before async queues become visible latency.');
      }
      if (this.currentOperations.pendingUsersTotal >= 20) {
        actions.push('Reduce the validation backlog with faster review workflows or more admin capacity.');
      }
      if (this.currentOperations.packageCounts.PAYMENTPENDING >= 20) {
        actions.push('Audit the payment funnel because too many packages are waiting for payment confirmation.');
      }
      if (Object.values(this.logInsights).some((insights) => (insights?.categoryCounts?.Discovery || 0) > 0)) {
        actions.push('Investigate service discovery and infrastructure resolution issues reported by runtime logs.');
      }
      if (Object.values(this.logInsights).some((insights) => (insights?.categoryCounts?.Database || 0) > 0)) {
        actions.push('Review SQL errors and database warnings to remove recurring operational faults.');
      }
      return [...new Set(actions)];
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
    cpuSeries() {
      return this.buildSeries((snapshot, moduleKey) => snapshot.modules?.[moduleKey]?.metrics?.cpuUsagePercent || 0);
    },
    heapSeries() {
      return this.buildSeries((snapshot, moduleKey) => this.computeHeapRatio(snapshot.modules?.[moduleKey]?.metrics) || 0);
    },
    trafficSeries() {
      return this.buildSeries((snapshot, moduleKey) => snapshot.modules?.[moduleKey]?.httpDelta || 0, 'sum');
    },
    queueSeries() {
      return this.buildSeries((snapshot, moduleKey) => snapshot.modules?.[moduleKey]?.metrics?.asyncQueueSize || 0);
    },
    warningSeries() {
      return this.buildLogSeries('warnCount');
    },
    errorSeries() {
      return this.buildLogSeries('errorCount');
    },
    connectionSeries() {
      return [
        {
          key: 'connected-customers',
          label: this.$t('metricsConnectedCustomers'),
          color: '#1f5fae',
          values: this.buildPopulationSeries('connectedCustomers'),
        },
        {
          key: 'connected-delivery-persons',
          label: this.$t('metricsConnectedDeliveryPersons'),
          color: '#159947',
          values: this.buildPopulationSeries('connectedDeliveryPersons'),
        },
      ];
    },
    serviceRequestItems() {
      return MODULE_CONFIG.map((moduleConfig) => ({
        key: `service-${moduleConfig.key}`,
        label: this.$t(moduleConfig.titleKey),
        color: moduleConfig.color,
        value: Number(this.httpBreakdown[moduleConfig.key]?.totalRequestCount || 0),
        meta: this.$t('metricsRequestCount'),
      }));
    },
    endpointRequestItems() {
      return MODULE_CONFIG.flatMap((moduleConfig) =>
        (this.httpBreakdown[moduleConfig.key]?.endpoints || []).map((endpoint, index) => ({
          key: `${moduleConfig.key}-${index}`,
          label: `${endpoint.method} ${endpoint.endpoint}`,
          color: moduleConfig.color,
          value: Number(endpoint.requestCount || 0),
          meta: `${this.$t(moduleConfig.titleKey)} · ${this.$t('metricsRequestPeakLatency')} ${Number(endpoint.maxResponseTimeMs || 0).toFixed(0)} ms`,
        }))
      )
        .sort((left, right) => right.value - left.value)
        .slice(0, 10);
    },
    lastHourTrafficTotal() {
      return this.trafficSeries.reduce((sum, series) => sum + Number(series.values?.[series.values.length - 1] || 0), 0);
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
        .slice(0, 10);
    },
    filteredRecentLogEvents() {
      if (this.logLevelFilter === 'ALL') {
        return this.recentLogEvents;
      }
      return this.recentLogEvents.filter((event) => event.level === this.logLevelFilter);
    },
  },
  mounted() {
    this.loadStoredHistory();
    this.refreshSnapshot();
    this.refreshTimer = window.setInterval(() => this.refreshSnapshot(), POLL_INTERVAL_MS);
  },
  beforeUnmount() {
    if (this.refreshTimer) {
      window.clearInterval(this.refreshTimer);
      this.refreshTimer = null;
    }
    this.persistHistory();
  },
  methods: {
    async refreshSnapshot() {
      this.isLoading = !this.currentSnapshot;
      this.loadError = false;
      try {
        const token = getAccessToken();
        const headers = token ? { Authorization: `Bearer ${token}` } : {};
        const currentYear = new Date().getFullYear();
        const requests = await Promise.allSettled([
          http.get(`${this.$i18n.t('gatewayRootURL')}${this.$i18n.t('getAdminGatewayMetrics')}`, { headers }),
          http.get(`${this.$i18n.t('gatewayRootURL')}${this.$i18n.t('getAdminConfigMetrics')}`, { headers }),
          http.get(`${this.$i18n.t('gatewayRootURL')}${this.$i18n.t('getAdminDiscoveryMetrics')}`, { headers }),
          http.get(`${this.$i18n.t('gatewayRootURL')}${this.$i18n.t('getAdminOauthMetrics')}`, { headers }),
          http.get(`${this.$i18n.t('gatewayRootURL')}${this.$i18n.t('getAdminRedisMetrics')}`, { headers }),
          http.get(`${this.$i18n.t('gatewayRootURL')}${this.$i18n.t('getAdminMysqlMetrics')}`, { headers }),
          http.get(`${this.$i18n.t('gatewayRootURL')}${this.$i18n.t('getAdminAggregatedUserMetrics')}`, { headers }),
          http.get(`${this.$i18n.t('gatewayRootURL')}${this.$i18n.t('getAdminAggregatedPackageMetrics')}`, { headers }),
          http.get(`${this.$i18n.t('gatewayRootURL')}${this.$i18n.t('getAdminAggregatedTrackingMetrics')}`, { headers }),
          http.get(`${this.$i18n.t('userRootURL')}${this.$i18n.t('getAdminUserOverview')}`, { headers }),
          http.get(`${this.$i18n.t('rootURL')}${this.$i18n.t('getAdminPackageDashboardSummary')}?year=${currentYear}`, { headers }),
          http.get(`${this.$i18n.t('gatewayRootURL')}${this.$i18n.t('getAdminGatewayHttpBreakdown')}`, { headers }),
          http.get(`${this.$i18n.t('gatewayRootURL')}${this.$i18n.t('getAdminAggregatedUserHttpBreakdown')}`, { headers }),
          http.get(`${this.$i18n.t('gatewayRootURL')}${this.$i18n.t('getAdminAggregatedPackageHttpBreakdown')}`, { headers }),
          http.get(`${this.$i18n.t('gatewayRootURL')}${this.$i18n.t('getAdminGatewayLogInsights')}`, { headers }),
          http.get(`${this.$i18n.t('gatewayRootURL')}${this.$i18n.t('getAdminAggregatedUserLogInsights')}`, { headers }),
          http.get(`${this.$i18n.t('gatewayRootURL')}${this.$i18n.t('getAdminAggregatedPackageLogInsights')}`, { headers }),
          http.get(`${this.$i18n.t('userRootURL')}${this.$i18n.t('getUsersForValidation')}?page=0&size=5`, { headers }),
        ]);

        const timestamp = Date.now();
        const modules = {
          gateway: this.toModuleSnapshot(requests[0], timestamp),
          config: this.toModuleSnapshot(requests[1], timestamp),
          discovery: this.toModuleSnapshot(requests[2], timestamp),
          oauth: this.toModuleSnapshot(requests[3], timestamp),
          redis: this.toModuleSnapshot(requests[4], timestamp),
          mysql: this.toModuleSnapshot(requests[5], timestamp),
          users: this.toModuleSnapshot(requests[6], timestamp),
          packages: this.toModuleSnapshot(requests[7], timestamp),
          tracking: this.toModuleSnapshot(requests[8], timestamp),
        };
        this.logInsights = {
          gateway: requests[14].status === 'fulfilled' ? requests[14].value.data : null,
          config: null,
          discovery: null,
          oauth: null,
          redis: null,
          mysql: null,
          users: requests[15].status === 'fulfilled' ? requests[15].value.data?.summary || null : null,
          packages: requests[16].status === 'fulfilled' ? requests[16].value.data?.summary || null : null,
          tracking: null,
        };
        this.httpBreakdown = {
          gateway: requests[11].status === 'fulfilled' ? requests[11].value.data : null,
          config: null,
          discovery: null,
          oauth: null,
          redis: null,
          mysql: null,
          users: requests[12].status === 'fulfilled' ? requests[12].value.data?.summary || null : null,
          packages: requests[13].status === 'fulfilled' ? requests[13].value.data?.summary || null : null,
          tracking: this.buildDerivedTrackingHttpBreakdown(requests[8]),
        };

        const overviewResponse = requests[9].status === 'fulfilled' ? requests[9].value.data : null;
        const packageSummaryResponse = requests[10].status === 'fulfilled' ? requests[10].value.data : null;
        const usersResponse = requests[17].status === 'fulfilled' ? requests[17].value.data : null;
        const packageCounts = this.emptyPackageCounts();
        PACKAGE_STATUSES.forEach((status) => {
          packageCounts[status] = Number(packageSummaryResponse?.statusCounts?.[status] || 0);
        });

        const snapshot = {
          timestamp,
          modules: this.attachCounterDeltas(modules),
          operations: {
            pendingUsersTotal: Number(usersResponse?.totalItems || 0),
            pendingDocumentsTotal: Number(usersResponse?.totalDocuments || 0),
            packageCounts,
          },
          population: {
            registeredCustomers: Number(overviewResponse?.registeredCustomers || 0),
            registeredDeliveryPersons: Number(overviewResponse?.registeredDeliveryPersons || 0),
            connectedCustomers: Number(overviewResponse?.connectedCustomers || 0),
            connectedDeliveryPersons: Number(overviewResponse?.connectedDeliveryPersons || 0),
          },
        };

        this.currentSnapshot = snapshot;
        this.lastRefreshAt = timestamp;
        this.appendHistory(snapshot);
      } catch (error) {
        this.loadError = true;
        console.error('Unable to refresh admin metrics dashboard.', error);
      } finally {
        this.isLoading = false;
      }
    },
    toModuleSnapshot(result, timestamp) {
      if (result.status !== 'fulfilled') {
        return { timestamp, metrics: null, instances: [], totalInstances: 0, healthyInstances: 0, httpDelta: 0, operationDelta: 0 };
      }
      const payload = result.value.data || null;
      if (payload?.summary !== undefined) {
        const instances = Array.isArray(payload.instances) ? payload.instances : [];
        return {
          timestamp,
          metrics: payload.summary || null,
          instances,
          totalInstances: Number(payload.totalInstances || instances.length),
          healthyInstances: Number(payload.healthyInstances || instances.filter((instance) => instance?.available).length),
          httpDelta: 0,
          operationDelta: 0,
        };
      }
      return {
        timestamp,
        metrics: payload,
        instances: [],
        totalInstances: payload ? 1 : 0,
        healthyInstances: payload ? 1 : 0,
        httpDelta: 0,
        operationDelta: 0,
      };
    },
    attachCounterDeltas(modules) {
      const previousSnapshot = this.history[this.history.length - 1] || this.currentSnapshot;
      return Object.entries(modules).reduce((acc, [moduleKey, moduleValue]) => {
        const previousMetrics = previousSnapshot?.modules?.[moduleKey]?.metrics || null;
        const currentMetrics = moduleValue.metrics || null;
        acc[moduleKey] = {
          ...moduleValue,
          httpDelta: this.computeCounterDelta(previousMetrics?.httpRequestCount, currentMetrics?.httpRequestCount),
          operationDelta: this.computeCounterDelta(previousMetrics?.operationCallCount, currentMetrics?.operationCallCount),
        };
        return acc;
      }, {});
    },
    computeCounterDelta(previousValue, currentValue) {
      if (previousValue == null || currentValue == null) {
        return 0;
      }
      return Math.max(0, Number(currentValue) - Number(previousValue));
    },
    emptyPackageCounts() {
      return PACKAGE_STATUSES.reduce((acc, status) => {
        acc[status] = 0;
        return acc;
      }, {});
    },
    appendHistory(snapshot) {
      this.history = [...this.history, snapshot]
        .filter((entry) => entry.timestamp >= (Date.now() - HISTORY_RETENTION_MS))
        .slice(-2500);
      this.persistHistory();
    },
    loadStoredHistory() {
      try {
        const raw = localStorage.getItem(HISTORY_STORAGE_KEY);
        this.history = raw ? JSON.parse(raw) : [];
      } catch (_) {
        this.history = [];
      }
    },
    persistHistory() {
      try {
        localStorage.setItem(HISTORY_STORAGE_KEY, JSON.stringify(this.history));
      } catch (_) {
        // Ignore storage errors.
      }
    },
    computeAvailability(moduleKey) {
      const relevantSnapshots = this.history.filter((snapshot) => snapshot.timestamp >= (Date.now() - (24 * 60 * 60 * 1000)));
      if (!relevantSnapshots.length) {
        return null;
      }
      const upCount = relevantSnapshots.filter((snapshot) => !!snapshot.modules?.[moduleKey]?.metrics).length;
      return (upCount / relevantSnapshots.length) * 100;
    },
    resolveModuleStatusLabel(moduleSnapshot, availability) {
      if (!moduleSnapshot?.metrics) {
        return this.$t('metricsHealthCritical');
      }
      if ((availability || 100) < 95 || this.computeHeapRatio(moduleSnapshot.metrics) >= 85 || (moduleSnapshot.metrics.cpuUsagePercent || 0) >= 80) {
        return this.$t('metricsHealthWarn');
      }
      return this.$t('metricsHealthOk');
    },
    resolveModuleStatusClass(moduleSnapshot, availability) {
      if (!moduleSnapshot?.metrics) {
        return 'status-critical';
      }
      if ((availability || 100) < 95 || this.computeHeapRatio(moduleSnapshot.metrics) >= 85 || (moduleSnapshot.metrics.cpuUsagePercent || 0) >= 80) {
        return 'status-warn';
      }
      return 'status-ok';
    },
    computeHeapRatio(metrics) {
      if (!metrics || metrics.heapUsedMb == null || metrics.heapMaxMb == null || Number(metrics.heapMaxMb) <= 0) {
        return null;
      }
      return (Number(metrics.heapUsedMb) / Number(metrics.heapMaxMb)) * 100;
    },
    buildSeries(selector, aggregation = 'avg') {
      return MODULE_CONFIG.map((moduleConfig) => ({
        key: moduleConfig.key,
        label: this.$t(moduleConfig.titleKey),
        color: moduleConfig.color,
        values: this.hourlyBuckets.map((bucket) => {
          const values = this.history
            .filter((snapshot) => snapshot.timestamp >= bucket.start && snapshot.timestamp < bucket.end)
            .map((snapshot) => Number(selector(snapshot, moduleConfig.key) || 0))
            .filter((value) => !Number.isNaN(value));
          if (!values.length) {
            return 0;
          }
          if (aggregation === 'sum') {
            return values.reduce((sum, value) => sum + value, 0);
          }
          return values.reduce((sum, value) => sum + value, 0) / values.length;
        }),
      }));
    },
    buildLogSeries(field) {
      return MODULE_CONFIG.map((moduleConfig) => ({
        key: `${moduleConfig.key}-${field}`,
        label: this.$t(moduleConfig.titleKey),
        color: moduleConfig.color,
        values: this.hourLabels.map((label) => {
          const bucket = this.logInsights[moduleConfig.key]?.hourlyCounts?.find((hourlyCount) => hourlyCount.hourLabel === label);
          if (!bucket) {
            return 0;
          }
          return Number(field === 'warnCount' ? bucket.warnCount : bucket.errorCount) || 0;
        }),
      }));
    },
    buildPopulationSeries(field) {
      return this.hourlyBuckets.map((bucket) => {
        const values = this.history
          .filter((snapshot) => snapshot.timestamp >= bucket.start && snapshot.timestamp < bucket.end)
          .map((snapshot) => Number(snapshot.population?.[field] || 0))
          .filter((value) => !Number.isNaN(value));
        if (!values.length) {
          return 0;
        }
        return values[values.length - 1];
      });
    },
    buildDerivedTrackingHttpBreakdown(result) {
      if (result.status !== 'fulfilled') {
        return null;
      }
      const metrics = result.value.data?.summary || null;
      if (!metrics) {
        return null;
      }
      return {
        serviceName: 'tracking-service',
        totalRequestCount: Number(metrics.httpRequestCount || 0),
        endpoints: [],
      };
    },
    topCategoriesForModule(moduleKey) {
      const categoryCounts = this.logInsights[moduleKey]?.categoryCounts || {};
      return Object.entries(categoryCounts)
        .sort((left, right) => Number(right[1]) - Number(left[1]))
        .slice(0, 3)
        .map(([label, count]) => ({ label, count }));
    },
    formatInstanceLabel(instance) {
      const host = instance?.ipAddress || instance?.host || 'n/a';
      const port = instance?.port ? `:${instance.port}` : '';
      const instanceId = instance?.instanceId ? ` (${instance.instanceId})` : '';
      return `${host}${port}${instanceId}`;
    },
    formatInteger(value) {
      return new Intl.NumberFormat(this.currentLocale, { maximumFractionDigits: 0 }).format(Number(value || 0));
    },
    formatPercent(value) {
      if (value == null) {
        return '-';
      }
      return `${Number(value).toFixed(1)}%`;
    },
    formatHeap(used, max) {
      if (used == null && max == null) {
        return '-';
      }
      if (max == null) {
        return `${Number(used || 0).toFixed(1)} MB`;
      }
      return `${Number(used || 0).toFixed(1)} / ${Number(max).toFixed(1)} MB`;
    },
    formatDuration(seconds) {
      if (seconds == null) {
        return '-';
      }
      const totalSeconds = Math.max(0, Math.round(Number(seconds)));
      const hours = Math.floor(totalSeconds / 3600);
      const minutes = Math.floor((totalSeconds % 3600) / 60);
      return hours > 0 ? `${hours}h ${minutes}m` : `${minutes}m ${totalSeconds % 60}s`;
    },
    formatDateTime(timestamp) {
      if (!timestamp) {
        return '-';
      }
      return new Intl.DateTimeFormat(this.currentLocale, {
        day: '2-digit',
        month: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
      }).format(new Date(timestamp));
    },
    formatTimeAgo(timestamp) {
      if (!timestamp) {
        return '-';
      }
      const diffMinutes = Math.max(0, Math.round((Date.now() - timestamp) / 60000));
      if (diffMinutes < 1) {
        return '< 1 min';
      }
      if (diffMinutes < 60) {
        return `${diffMinutes} min`;
      }
      return `${Math.floor(diffMinutes / 60)} h`;
    },
  },
};
</script>

<style scoped>
.metrics-dashboard-page {
  min-height: 100%;
  padding: 28px;
  background: radial-gradient(circle at top left, rgba(21, 148, 71, 0.1), transparent 30%), linear-gradient(180deg, #f4f7fb 0%, #eef3f8 100%);
  box-sizing: border-box;
}
.hero-card,.panel-card,.stat-card {
  border: 1px solid #dfe7f2;
  border-radius: 24px;
  background: rgba(255,255,255,0.94);
  box-shadow: 0 16px 40px rgba(15,23,42,0.08);
}
.hero-card {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  padding: 28px;
  margin-bottom: 18px;
}
.hero-chip {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  background: #e4f4ea;
  color: #13613a;
  font-size: 0.78rem;
  font-weight: 700;
  text-transform: uppercase;
}
.hero-card h1 {
  margin: 12px 0 8px;
  font-size: 2.6rem;
  line-height: 1;
  color: #0f172a;
}
.hero-card p,.panel-head p {
  margin: 0;
  color: #64748b;
}
.hero-side {
  min-width: 240px;
  display: grid;
  gap: 8px;
  padding: 18px;
  border-radius: 18px;
  background: #0f172a;
  color: #fff;
}
.hero-side span,.hero-side small {
  color: rgba(255,255,255,0.72);
}
.summary-grid,.module-grid,.dashboard-grid,.chart-grid,.operations-grid {
  display: grid;
  gap: 18px;
}
.population-grid {
  display: grid;
  gap: 18px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin-bottom: 18px;
}
.summary-grid {
  grid-template-columns: repeat(6, minmax(0, 1fr));
  margin-bottom: 18px;
}
.module-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin-bottom: 18px;
}
.dashboard-grid,.chart-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-bottom: 18px;
}
.logs-grid {
  grid-template-columns: 1fr;
}
.operations-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}
.stat-card,.panel-card {
  padding: 22px;
}
.stat-card strong {
  display: block;
  margin-bottom: 8px;
  font-size: 1.9rem;
  color: #0f172a;
}
.module-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}
.status-ok { color: #15803d; }
.status-warn { color: #b45309; }
.status-critical { color: #b91c1c; }
.metrics-list,.bullet-list {
  display: grid;
  gap: 12px;
  padding: 0;
  margin: 0;
  list-style: none;
}
.log-signal-block {
  display: grid;
  gap: 10px;
  margin-top: 14px;
}
.instance-block {
  display: grid;
  gap: 12px;
  margin-top: 14px;
}
.instance-head h3 {
  margin: 0;
  font-size: 1rem;
  color: #0f172a;
}
.instance-list {
  display: grid;
  gap: 10px;
}
.instance-card {
  display: grid;
  gap: 8px;
  padding: 12px 14px;
  border-radius: 14px;
  background: #f8fafc;
  border: 1px solid #e7edf6;
}
.instance-card.unavailable {
  background: #fef2f2;
  border-color: #fecaca;
}
.instance-topline {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}
.instance-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  color: #334155;
  font-size: 0.9rem;
}
.instance-error {
  color: #b91c1c;
  font-size: 0.9rem;
}
.log-chip-row,
.category-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.log-chip,
.category-pill {
  display: inline-flex;
  align-items: center;
  padding: 8px 10px;
  border-radius: 999px;
  font-size: 0.78rem;
  font-weight: 600;
}
.log-chip.warn {
  background: #fff7ed;
  color: #b45309;
}
.log-chip.error {
  background: #fef2f2;
  color: #b91c1c;
}
.category-pill {
  background: #eff6ff;
  color: #24558f;
}
.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}
.filter-chip {
  border: 1px solid #d6dfeb;
  border-radius: 999px;
  background: #fff;
  color: #475569;
  padding: 6px 12px;
  font: inherit;
  cursor: pointer;
}
.filter-chip.active {
  border-color: #0f172a;
  background: #0f172a;
  color: #fff;
}
.event-level {
  display: inline-flex;
  min-width: 56px;
  margin-right: 8px;
  font-weight: 700;
}
.event-level.is-warn {
  color: #b45309;
}
.event-level.is-error {
  color: #b91c1c;
}
.metric-row,.operation-item,.bullet-list li {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 14px;
  border-radius: 14px;
  background: #f8fafc;
  border: 1px solid #e7edf6;
  color: #334155;
}
.operation-item {
  flex-direction: column;
}
.operation-item strong {
  color: #0f172a;
  font-size: 1.5rem;
}
.page-state,.empty-state {
  padding: 16px;
  border-radius: 14px;
  background: #edf2f7;
  color: #334155;
  text-align: center;
}
.page-state.error {
  background: #fef2f2;
  color: #b91c1c;
}
.tone-dark strong { color: #0f172a; }
.tone-warn strong { color: #c77b00; }
.tone-success strong { color: #15803d; }
.tone-neutral strong { color: #475569; }
@media screen and (max-width: 1280px) {
  .population-grid,
  .summary-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .module-grid,.dashboard-grid,.chart-grid,.operations-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media screen and (max-width: 767px) {
  .metrics-dashboard-page { padding: 16px; }
  .hero-card,.population-grid,.summary-grid,.module-grid,.dashboard-grid,.chart-grid,.operations-grid { grid-template-columns: 1fr; }
  .hero-card { flex-direction: column; }
  .hero-side { width: 100%; min-width: 0; }
  .hero-card h1 { font-size: 2rem; }
}
</style>
