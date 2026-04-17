<template>
  <div class="dashboard-page qd-page" :class="`type-${dashboardType}`">
    <template v-if="isLoadingPage">
      <header class="qd-page-header">
        <div class="header-main">
          <span class="page-chip">{{ $t('dashboardWelcomeBack') }}</span>
          <h1>{{ dashboardTitle }}</h1>
          <p>{{ dashboardSubtitle }}</p>
        </div>
      </header>
      <div class="page-state loading-state">
        <div class="spinner"></div>
        <span>{{ $t('stateLoading') }}</span>
      </div>
    </template>

    <template v-else-if="loadError">
      <header class="qd-page-header">
        <div class="header-main">
          <span class="page-chip">{{ $t('dashboardWelcomeBack') }}</span>
          <h1>{{ dashboardTitle }}</h1>
        </div>
      </header>
      <div class="page-state error-state">
        <span class="material-symbols-outlined">error</span>
        <p>{{ $t('stateLoadError') }}</p>
      </div>
    </template>

    <template v-else>
      <header class="qd-page-header">
        <div class="header-main">
          <span class="page-chip pulse-chip">{{ $t('dashboardWelcomeBack') }}</span>
          <h1>{{ dashboardTitle }}</h1>
          <p>{{ dashboardSubtitle }}</p>
        </div>
        <div class="qd-page-header-actions">
          <div class="hero-account-glass">
            <div class="account-avatar">
              {{ connectedUserName.charAt(0).toUpperCase() }}
            </div>
            <div class="account-info">
              <strong>{{ connectedUserName }}</strong>
              <span class="role-badge">{{ connectedUserRoleLabel }}</span>
            </div>
          </div>
        </div>
      </header>

      <section class="stats-grid">
        <PremiumDashboardCard
          v-for="stat in statsCards"
          :key="stat.label"
          variant="glass"
          :tone="stat.tone.replace('tone-', '') || 'indigo'"
          class="stat-premium-card"
        >
          <div class="stat-inner">
            <span class="stat-value">{{ stat.value }}</span>
            <span class="stat-label">{{ stat.label }}</span>
          </div>
        </PremiumDashboardCard>
      </section>

      <div class="dashboard-main-layout">
        <!-- Section: Priorités et Actions -->
        <section class="priority-actions-column">
          <PremiumDashboardCard 
            :title="$t('dashboardQuickActions')" 
            variant="flat" 
            class="action-panel"
            no-padding
          >
            <div class="action-grid-premium">
              <router-link
                v-for="action in quickActions"
                :key="action.to"
                class="premium-action-tile"
                :to="action.to"
              >
                <div class="tile-icon">
                  <span class="material-symbols-outlined">{{ action.icon }}</span>
                </div>
                <strong>{{ action.label }}</strong>
                <span class="material-symbols-outlined tile-arrow">chevron_right</span>
              </router-link>
            </div>
          </PremiumDashboardCard>

          <PremiumDashboardCard 
            :title="$t('dashboardTodo')" 
            variant="glass" 
            tone="amber"
            class="todo-panel"
          >
            <div class="todo-stack">
              <div v-for="(item, index) in todoItems" :key="index" class="todo-pill">
                <span class="material-symbols-outlined">check_circle</span>
                <span>{{ item }}</span>
              </div>
            </div>
          </PremiumDashboardCard>
        </section>

        <!-- Section: Activité et Charts -->
        <section class="activity-history-column">
          <div class="charts-container" v-if="chartCards.length">
            <DashboardTrendChart
              v-for="chart in chartCards"
              :key="chart.key"
              :title="chart.title"
              :subtitle="chart.subtitle"
              :points="chart.points"
              :tone="chart.tone"
              :formatter="chart.formatter"
              :empty-label="$t('dashboardChartEmpty')"
              class="chart-panel"
            />
          </div>

          <div class="dual-panel-row">
            <PremiumDashboardCard :title="$t('dashboardActivity')" class="timeline-panel">
              <div v-if="timelineItems.length" class="premium-timeline">
                <div v-for="item in timelineItems" :key="item.key" class="timeline-entry">
                  <div class="entry-dot"></div>
                  <div class="entry-content">
                    <strong>{{ item.title }}</strong>
                    <span>{{ item.subtitle }}</span>
                  </div>
                </div>
              </div>
              <div v-else class="empty-compact">
                <span class="material-symbols-outlined">history</span>
                <p>{{ emptyTimelineLabel }}</p>
              </div>
            </PremiumDashboardCard>

            <PremiumDashboardCard :title="$t('dashboardUpcoming')" class="upcoming-panel">
              <div v-if="upcomingItems.length" class="upcoming-stack">
                <div v-for="item in upcomingItems" :key="item.key" class="upcoming-card-inner">
                  <div class="inner-icon">
                    <span class="material-symbols-outlined">event</span>
                  </div>
                  <div class="inner-copy">
                    <strong>{{ item.title }}</strong>
                    <span>{{ item.subtitle }}</span>
                  </div>
                </div>
              </div>
              <div v-else class="empty-compact">
                <span class="material-symbols-outlined">schedule</span>
                <p>{{ $t('dashboardNoData') }}</p>
              </div>
            </PremiumDashboardCard>
          </div>
        </section>
      </div>
    </template>
  </div>
</template>

<script>
import http from '@/config/httpInterceptor';
import { getCurrentUserRoles } from '@/config/auth';
import { hydrateConnectedUser } from '@/config/session';
import DashboardTrendChart from '../components/DashboardTrendChart.vue';
import PremiumDashboardCard from '../components/PremiumDashboardCard.vue';

const PACKAGE_ACTIVITY_ORDER = ['NEW', 'PAYMENTPENDING', 'RESERVED', 'PICKEDUP', 'INDELIVERY', 'DELIVERED'];
const MONTH_COUNT = 12;
const MONTH_GRAPH_START_X = 32;
const MONTH_GRAPH_STEP_X = 25;
const MONTH_GRAPH_HEIGHT = 180;

export default {
  components: {
    DashboardTrendChart,
    PremiumDashboardCard,
  },
  props: {
    dashboardType: {
      type: String,
      required: true,
    },
  },
  data() {
    return {
      isLoadingPage: false,
      loadError: false,
      hasLoadedDashboard: false,
      packagesByStatus: {},
      adminPackageCounts: {},
      adminMonthlyShipmentCounts: Array.from({ length: MONTH_COUNT }, () => 0),
      adminMonthlyDeliveredRevenue: Array.from({ length: MONTH_COUNT }, () => 0),
      adminRecentPackages: [],
      pendingUsers: [],
      pendingUsersTotal: 0,
      pendingVehiclesTotal: 0,
      pendingDocumentsTotal: 0,
      pendingVehicleDocumentsTotal: 0,
      currentYear: new Date().getFullYear(),
    };
  },
  computed: {
    userRoles() {
      return getCurrentUserRoles();
    },
    connectedUser() {
      return this.$store.state.connectedUser || {};
    },
    connectedUserName() {
      return [this.connectedUser.firstName, this.connectedUser.lastName].filter(Boolean).join(' ') || this.connectedUser.email || 'QuickDelivery';
    },
    connectedUserRoleLabel() {
      if (this.dashboardType === 'admin') return this.$t('dashboardRoleAdmin');
      if (this.dashboardType === 'courier') return this.$t('DELIVERY_PERSON');
      return this.$t('CUSTOMER');
    },
    dashboardTitle() {
      if (this.dashboardType === 'admin') return this.$t('dashboardAdminTitle');
      if (this.dashboardType === 'courier') return this.$t('dashboardCourierTitle');
      return this.$t('dashboardClientTitle');
    },
    dashboardSubtitle() {
      if (this.dashboardType === 'admin') return this.$t('dashboardAdminSubtitle');
      if (this.dashboardType === 'courier') return this.$t('dashboardCourierSubtitle');
      return this.$t('dashboardClientSubtitle');
    },
    flattenedPackages() {
      if (this.dashboardType === 'admin') return this.adminRecentPackages;
      return Object.values(this.packagesByStatus || {}).flatMap((group) => group || []);
    },
    currentLocale() {
      const locale = this.$i18n?.locale;
      return typeof locale === 'string' ? locale : locale?.value || 'fr';
    },
    chartMonthLabels() {
      return Array.from({ length: MONTH_COUNT }, (_, monthIndex) => new Intl.DateTimeFormat(this.currentLocale, {
        month: 'short',
      }).format(new Date(this.currentYear, monthIndex, 1)));
    },
    packageCounts() {
      if (this.dashboardType === 'admin') return this.adminPackageCounts || {};
      return this.flattenedPackages.reduce((acc, pkg) => {
        const status = pkg.status || 'UNKNOWN';
        acc[status] = (acc[status] || 0) + 1;
        return acc;
      }, {});
    },
    chartCards() {
      if (this.dashboardType === 'admin') {
        return [
          this.createChartConfig({
            key: 'admin-shipments',
            title: this.$t('dashboardAdminShipmentsChartTitle'),
            subtitle: this.$t('dashboardAdminShipmentsChartSubtitle', { year: this.currentYear }),
            tone: '#1f5fae',
            formatter: this.formatInteger,
            values: this.adminMonthlyShipmentCounts,
          }),
          this.createChartConfig({
            key: 'admin-earnings',
            title: this.$t('dashboardAdminEarningsChartTitle'),
            subtitle: this.$t('dashboardAdminEarningsChartSubtitle', { year: this.currentYear }),
            tone: '#ef7d32',
            formatter: this.formatCurrency,
            values: this.adminMonthlyDeliveredRevenue,
          }),
        ];
      }
      if (this.dashboardType === 'courier') {
        return [
          this.createChartConfig({
            key: 'courier-earnings',
            title: this.$t('dashboardCourierChartTitle'),
            subtitle: this.$t('dashboardCourierChartSubtitle', { year: this.currentYear }),
            tone: '#ef7d32',
            formatter: this.formatCurrency,
            values: this.flattenedPackages
              .filter((pkg) => pkg.status === 'DELIVERED')
              .reduce((months, pkg) => this.accumulateByMonth(months, pkg.creationDate, Number(pkg.deliveryPrice || 0)), this.createMonthlyAccumulator()),
          }),
        ];
      }
      return [
        this.createChartConfig({
          key: 'client-shipments',
          title: this.$t('dashboardClientChartTitle'),
          subtitle: this.$t('dashboardClientChartSubtitle', { year: this.currentYear }),
          tone: '#1f5fae',
          formatter: this.formatInteger,
          values: this.flattenedPackages
            .reduce((months, pkg) => this.accumulateByMonth(months, pkg.creationDate, 1), this.createMonthlyAccumulator()),
        }),
      ];
    },
    statsCards() {
      if (this.dashboardType === 'admin') {
        return [
          { label: this.$t('dashboardStatValidationQueue'), value: this.pendingUsersTotal, tone: 'tone-amber' },
          { label: this.$t('validationVehiclesCount'), value: this.pendingVehiclesTotal, tone: 'tone-slate' },
          { label: this.$t('validationDocumentsCount'), value: this.pendingDocumentsTotal, tone: 'tone-indigo' },
          { label: this.$t('dashboardStatVehicleDocs'), value: this.pendingVehicleDocumentsTotal, tone: 'tone-emerald' },
        ];
      }
      if (this.dashboardType === 'courier') {
        const onRoadCount = (this.packageCounts.PICKEDUP || 0) + (this.packageCounts.INDELIVERY || 0);
        return [
          { label: this.$t('dashboardStatPackages'), value: this.flattenedPackages.length, tone: 'tone-indigo' },
          { label: this.$t('dashboardStatReserved'), value: this.packageCounts.RESERVED || 0, tone: 'tone-amber' },
          { label: this.$t('dashboardStatOnRoad'), value: onRoadCount, tone: 'tone-emerald' },
          { label: this.$t('dashboardStatDelivered'), value: this.packageCounts.DELIVERED || 0, tone: 'tone-slate' },
        ];
      }
      const activeCount = (this.packageCounts.RESERVED || 0) + (this.packageCounts.PICKEDUP || 0) + (this.packageCounts.INDELIVERY || 0);
      return [
        { label: this.$t('dashboardStatPackages'), value: this.flattenedPackages.length, tone: 'tone-indigo' },
        { label: this.$t('dashboardStatPending'), value: (this.packageCounts.NEW || 0) + (this.packageCounts.PAYMENTPENDING || 0), tone: 'tone-slate' },
        { label: this.$t('dashboardStatActive'), value: activeCount, tone: 'tone-amber' },
        { label: this.$t('dashboardStatDelivered'), value: this.packageCounts.DELIVERED || 0, tone: 'tone-emerald' },
      ];
    },
    quickActions() {
      if (this.dashboardType === 'admin') {
        return [
          { to: '/usersAccountValidation', icon: 'fact_check', label: this.$t('dashboardActionValidation') },
          { to: '/dashboard/metrics', icon: 'monitoring', label: this.$t('dashboardActionMetrics') },
          { to: '/userAccount', icon: 'person', label: this.$t('dashboardActionAccount') },
          { to: '/createPackage', icon: 'box_add', label: this.$t('dashboardActionCreatePackage') },
        ];
      }
      if (this.dashboardType === 'courier') {
        return [
          { to: '/app', icon: 'map', label: this.$t('dashboardActionOpenMap') },
          { to: '/myPackages', icon: 'inventory_2', label: this.$t('dashboardActionMyPackages') },
          { to: '/userAccount', icon: 'person', label: this.$t('dashboardActionAccount') },
        ];
      }
      return [
        { to: '/createPackage', icon: 'box_add', label: this.$t('dashboardActionCreatePackage') },
        { to: '/myPackages', icon: 'inventory_2', label: this.$t('dashboardActionReviewPackages') },
        { to: '/userAccount', icon: 'person', label: this.$t('dashboardActionAccount') },
      ];
    },
    todoItems() {
      const keys = this.dashboardType === 'admin' 
        ? ['dashboardAdminTodoValidation', 'dashboardAdminTodoOps']
        : this.dashboardType === 'courier'
        ? ['dashboardCourierTodoPickup', 'dashboardCourierTodoDeliver']
        : ['dashboardClientTodoPayment', 'dashboardClientTodoTrack'];
      return keys.map(k => this.$t(k));
    },
    timelineItems() {
      if (this.dashboardType === 'admin') {
        return this.pendingUsers
          .slice(0, 5)
          .map((user) => ({
            key: `user-${user.id}`,
            title: `${this.$t('dashboardTimelinePendingUser')} - ${[user.firstName, user.lastName].filter(Boolean).join(' ') || user.emailAddress}`,
            subtitle: `${user.documentCount || 0} ${this.$t('validationDocumentsLabel')}`,
          }));
      }
      return this.flattenedPackages
        .slice()
        .sort((a, b) => new Date(b.creationDate || 0) - new Date(a.creationDate || 0))
        .slice(0, 5)
        .map((pkg) => ({
          key: pkg.reference || `pkg-${pkg.id}`,
          title: `${this.$t(this.timelineLabelByStatus(pkg.status))} - ${pkg.reference || `PKG${pkg.id}`}`,
          subtitle: this.packageSubtitle(pkg),
        }));
    },
    upcomingItems() {
      if (this.dashboardType === 'admin') {
        return this.pendingUsers
          .slice(0, 3)
          .map((user) => ({
            key: `review-${user.id}`,
            title: [user.firstName, user.lastName].filter(Boolean).join(' ') || user.emailAddress,
            subtitle: `${user.documentCount || 0} ${this.$t('validationDocumentsLabel')}`,
          }));
      }
      return this.flattenedPackages
        .filter((pkg) => ['NEW', 'PAYMENTPENDING', 'RESERVED', 'PICKEDUP', 'INDELIVERY'].includes(pkg.status))
        .sort((a, b) => PACKAGE_ACTIVITY_ORDER.indexOf(a.status) - PACKAGE_ACTIVITY_ORDER.indexOf(b.status))
        .slice(0, 3)
        .map((pkg) => ({
          key: `next-${pkg.reference || pkg.id}`,
          title: `${pkg.reference || `PKG${pkg.id}`} - ${this.$t(pkg.status || 'UNKNOWN')}`,
          subtitle: this.packageSubtitle(pkg),
        }));
    },
    emptyTimelineLabel() {
      return this.dashboardType === 'admin' ? this.$t('dashboardStreamNoUsers') : this.$t('dashboardStreamNoPackages');
    },
  },
  mounted() {
    this.loadDashboard();
  },
  methods: {
    getResolvedConnectedUserId() {
      return Number(this.connectedUser?.id) || null;
    },
    async ensureConnectedUserReady() {
      if (this.getResolvedConnectedUserId()) return true;
      const hydratedUser = await hydrateConnectedUser();
      return Boolean(hydratedUser?.id);
    },
    async loadDashboard() {
      if (this.hasLoadedDashboard || !(await this.ensureConnectedUserReady())) return;
      const connectedUserId = this.getResolvedConnectedUserId();
      this.isLoadingPage = true;
      this.loadError = false;
      try {
        if (this.dashboardType === 'admin') {
          const [usersResponse, packageSummaryResponse] = await Promise.all([
            http.get(`${this.$i18n.t('userRootURL')}${this.$i18n.t('getUsersForValidation')}?page=0&size=5`),
            http.get(`${this.$i18n.t('rootURL')}${this.$i18n.t('getAdminPackageDashboardSummary')}?year=${this.currentYear}`),
          ]);
          this.pendingUsers = usersResponse.data?.items || [];
          this.pendingUsersTotal = usersResponse.data?.totalItems || 0;
          this.pendingVehiclesTotal = usersResponse.data?.totalVehicles || 0;
          this.pendingDocumentsTotal = usersResponse.data?.totalDocuments || 0;
          this.pendingVehicleDocumentsTotal = usersResponse.data?.totalVehicleDocuments || 0;
          const packageSummary = packageSummaryResponse.data || {};
          this.adminPackageCounts = packageSummary.statusCounts || {};
          this.adminMonthlyShipmentCounts = packageSummary.monthlyShipmentCounts || this.createMonthlyAccumulator();
          this.adminMonthlyDeliveredRevenue = packageSummary.monthlyDeliveredRevenue || this.createMonthlyAccumulator();
          this.adminRecentPackages = packageSummary.recentPackages || [];
        } else {
          const endpoint = this.dashboardType === 'courier'
            ? this.$i18n.t('getPackagesByDeliveryPersonUrl')
            : this.$i18n.t('getPackagesBySenderUrl');
          const response = await http.get(`${this.$i18n.t('rootURL')}${endpoint}${connectedUserId}`);
          this.packagesByStatus = response.data || {};
        }
        this.hasLoadedDashboard = true;
      } catch (error) {
        this.loadError = true;
        console.error('Unable to load dashboard data.', error);
      } finally {
        this.isLoadingPage = false;
      }
    },
    getAddressByType(package_, type) {
      return (package_.addresses || []).find((address) => address.type === type) || {};
    },
    formatAddress(address) {
      return [address.line1, address.zipCode, address.town].filter(Boolean).join(', ') || address.addressAuto || '-';
    },
    packageSubtitle(pkg) {
      const departure = this.formatAddress(this.getAddressByType(pkg, 'DEPARTURE'));
      const arrival = this.formatAddress(this.getAddressByType(pkg, 'ARRIVAL'));
      return `${departure} → ${arrival}`;
    },
    createMonthlyAccumulator() {
      return Array.from({ length: MONTH_COUNT }, () => 0);
    },
    accumulateByMonth(months, rawDate, value) {
      const date = rawDate ? new Date(rawDate) : null;
      if (!date || date.getFullYear() !== this.currentYear) return months;
      months[date.getMonth()] += value;
      return months;
    },
    createChartConfig({ key, title, subtitle, tone, formatter, values }) {
      const maxValue = Math.max(...values, 0);
      const points = values.map((value, index) => {
        const ratio = maxValue > 0 ? value / maxValue : 0;
        return {
          label: this.chartMonthLabels[index],
          value,
          x: MONTH_GRAPH_START_X + (index * MONTH_GRAPH_STEP_X),
          y: MONTH_GRAPH_HEIGHT - (ratio * (MONTH_GRAPH_HEIGHT - 32)) - 12,
        };
      });
      return { key, title, subtitle, tone, formatter, points };
    },
    formatCurrency(value) {
      return new Intl.NumberFormat(this.currentLocale, { 
        style: 'currency', currency: 'EUR', maximumFractionDigits: 0 
      }).format(Number(value || 0));
    },
    formatInteger(value) {
      return new Intl.NumberFormat(this.currentLocale, { maximumFractionDigits: 0 }).format(Number(value || 0));
    },
    timelineLabelByStatus(status) {
      const map = { RESERVED: 'dashboardTimelineReserved', PICKEDUP: 'dashboardTimelinePickedUp', INDELIVERY: 'dashboardTimelinePickedUp', DELIVERED: 'dashboardTimelineDelivered' };
      return map[status] || 'dashboardTimelineCreated';
    },
  },
};
</script>

<style scoped>
.dashboard-page {
  min-height: 100%;
  padding: 32px;
  background: 
    radial-gradient(circle at top right, rgba(79, 70, 229, 0.08), transparent 400px),
    linear-gradient(180deg, #f8fafc 0%, #f1f5f9 100%);
  display: flex;
  flex-direction: column;
  gap: 32px;
}

/* Hero Section */
.hero-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
}

.hero-chip {
  display: inline-flex;
  padding: 6px 14px;
  background: rgba(79, 70, 229, 0.1);
  color: var(--qd-primary);
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 12px;
}

.pulse-chip {
  animation: qd-pulse-soft 2s infinite;
}

@keyframes qd-pulse-soft {
  0% { box-shadow: 0 0 0 0 rgba(79, 70, 229, 0.2); }
  70% { box-shadow: 0 0 0 10px rgba(79, 70, 229, 0); }
  100% { box-shadow: 0 0 0 0 rgba(79, 70, 229, 0); }
}

.hero-account-glass {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 18px;
  background: rgba(255, 255, 255, 0.45);
  backdrop-filter: blur(8px);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.4);
}

.account-avatar {
  width: 40px;
  height: 40px;
  background: var(--qd-primary);
  color: #fff;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  font-size: 1.15rem;
}

.account-info strong {
  display: block;
  font-size: 0.95rem;
  color: var(--qd-primary-dark);
}

.role-badge {
  font-size: 0.72rem;
  color: var(--qd-muted);
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.02em;
}

/* Stats */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.stat-premium-card .stat-inner {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 2.25rem;
  font-weight: 800;
  color: #0f172a;
  line-height: 1;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 0.875rem;
  color: #64748b;
  font-weight: 500;
}

/* Main Layout */
.dashboard-main-layout {
  display: grid;
  grid-template-columns: 360px 1fr;
  gap: 32px;
  align-items: start;
}

.priority-actions-column {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.action-grid-premium {
  display: flex;
  flex-direction: column;
}

.premium-action-tile {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px 24px;
  border-bottom: 1px solid rgba(15, 23, 42, 0.04);
  text-decoration: none;
  transition: var(--qd-transition);
}

.premium-action-tile:last-child {
  border-bottom: none;
}

.premium-action-tile:hover {
  background: rgba(79, 70, 229, 0.03);
}

.tile-icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: #f1f5f9;
  color: var(--qd-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: var(--qd-transition);
}

.premium-action-tile:hover .tile-icon {
  background: var(--qd-primary);
  color: #fff;
}

.premium-action-tile strong {
  flex: 1;
  color: #0f172a;
  font-size: 0.9375rem;
}

.tile-arrow {
  font-size: 18px;
  color: #cbd5e1;
  transition: transform 0.3s ease;
}

.premium-action-tile:hover .tile-arrow {
  transform: translateX(4px);
  color: var(--qd-primary);
}

.todo-stack {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.todo-pill {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: rgba(245, 158, 11, 0.08);
  border-radius: 12px;
  color: #b45309;
  font-size: 0.875rem;
  font-weight: 600;
}

.todo-pill span .material-symbols-outlined {
  font-size: 20px;
}

.charts-container {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: 24px;
}

.activity-history-column {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.dual-panel-row {
  display: grid;
  grid-template-columns: 1fr 1.2fr;
  gap: 24px;
}

.premium-timeline {
  display: flex;
  flex-direction: column;
  gap: 20px;
  position: relative;
}

.timeline-entry {
  display: flex;
  gap: 16px;
  position: relative;
}

.entry-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--qd-primary);
  margin-top: 6px;
  flex-shrink: 0;
  box-shadow: 0 0 0 4px rgba(79, 70, 229, 0.1);
}

.entry-content strong {
  display: block;
  font-size: 0.9375rem;
  color: #0f172a;
}

.entry-content span {
  font-size: 0.8125rem;
  color: #64748b;
}

.upcoming-stack {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.upcoming-card-inner {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #f8fafc;
  border-radius: 16px;
  border: 1px solid #f1f5f9;
}

.inner-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
  border: 1px solid #e2e8f0;
}

.inner-copy strong {
  display: block;
  font-size: 0.875rem;
  color: #0f172a;
}

.inner-copy span {
  font-size: 0.75rem;
  color: #94a3b8;
}

.empty-compact {
  padding: 32px 0;
  text-align: center;
  color: #94a3b8;
}

.empty-compact span {
  font-size: 32px;
  margin-bottom: 8px;
}

.empty-compact p {
  font-size: 0.8125rem;
  margin: 0;
}

/* Page states */
.loading-state, .error-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 120px 0;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid rgba(79, 70, 229, 0.1);
  border-top-color: var(--qd-primary);
  border-radius: 50%;
  animation: qd-spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes qd-spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 1200px) {
  .dashboard-main-layout {
    grid-template-columns: 1fr;
  }
}

@media screen and (max-width: 1024px) {
  .dashboard-main-layout {
    grid-template-columns: 1fr;
  }
}

@media screen and (max-width: 768px) {
  .dashboard-page {
    padding: 12px;
    gap: 12px;
  }

  .dashboard-page .qd-page-header {
    align-items: flex-start !important;
    gap: 12px !important;
    padding: 14px !important;
    border-radius: 14px !important;
  }

  .dashboard-page .qd-page-header .header-main {
    width: 100%;
  }

  .dashboard-page .qd-page-header h1 {
    margin: 6px 0 4px !important;
    font-size: clamp(1.25rem, 5.5vw, 1.45rem) !important;
    line-height: 1.08 !important;
  }

  .dashboard-page .qd-page-header p {
    margin: 0 !important;
    font-size: 0.82rem !important;
    line-height: 1.28 !important;
    max-width: 42rem;
  }

  .dashboard-page .page-chip {
    min-height: 26px !important;
    padding: 4px 8px !important;
    font-size: 0.68rem !important;
    letter-spacing: 0.02em !important;
  }

  .dashboard-page .qd-page-header-actions {
    width: 100%;
  }

  .hero-section {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-content h1 {
    font-size: 1.45rem;
    line-height: 1.1;
  }

  .hero-account-glass {
    width: 100%;
    gap: 10px;
    padding: 10px 12px;
    border-radius: 12px;
    box-sizing: border-box;
  }

  .account-avatar {
    width: 34px;
    height: 34px;
    border-radius: 10px;
    font-size: 1rem;
  }

  .account-info strong {
    font-size: 0.86rem;
  }

  .role-badge {
    font-size: 0.66rem;
  }

  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
  }

  .stat-value {
    font-size: 1.18rem;
    line-height: 1.08;
  }

  .stat-label {
    font-size: 0.68rem;
    line-height: 1.2;
  }

  .dashboard-main-layout {
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .priority-actions-column {
    gap: 10px;
  }

  .premium-action-tile {
    gap: 10px;
    padding: 8px 10px;
  }

  .tile-icon {
    width: 28px;
    height: 28px;
    border-radius: 10px;
    font-size: 0.92rem;
  }

  .premium-action-tile strong {
    font-size: 0.84rem;
    line-height: 1.2;
  }

  .tile-arrow {
    font-size: 16px;
  }

  .dual-panel-row {
    grid-template-columns: 1fr;
  }

  .charts-container {
    grid-template-columns: 1fr;
  }
}
</style>
