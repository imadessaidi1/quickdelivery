<template>
  <div class="finance-dashboard-page qd-page">
    <template v-if="isLoading">
      <header class="qd-page-header">
        <div class="header-main">
          <span class="page-chip info uppercase">{{ $t('menuAdminFinance') }}</span>
          <h1>{{ $t('financePageTitle') }}</h1>
          <p>{{ $t('financePageSubtitle') }}</p>
        </div>
      </header>
      <div class="page-state loading-state">
        <div class="premium-spinner"></div>
        <span>{{ $t('stateLoading') }}</span>
      </div>
    </template>

    <template v-else-if="loadError && !dashboard">
      <header class="qd-page-header">
        <div class="header-main">
          <span class="page-chip info uppercase">{{ $t('menuAdminFinance') }}</span>
          <h1>{{ $t('financePageTitle') }}</h1>
        </div>
      </header>
      <div class="page-state error-state">
        <span class="material-symbols-outlined large-icon">payments</span>
        <p>{{ $t('stateLoadError') }}</p>
      </div>
    </template>

    <template v-else-if="dashboard">
      <header class="qd-page-header">
        <div class="header-main">
          <span class="page-chip info uppercase" style="margin-bottom: 8px;">{{ $t('menuAdminFinance') }}</span>
          <h1>{{ $t('financePageTitle') }}</h1>
          <p>{{ $t('financePageSubtitle') }}</p>
        </div>
        
        <div class="qd-page-header-actions">
          <div class="hero-main-card glass-pane" style="padding: 16px 24px; min-width: 260px; text-align: right;">
            <div class="main-stat-label" style="display: flex; align-items: center; justify-content: flex-end; gap: 8px; color: var(--qd-muted); font-weight: 700; margin-bottom: 4px; font-size: 0.85rem;">
              <span class="material-symbols-outlined success-text" style="color: var(--qd-success); font-size: 1.2rem;">account_balance_wallet</span>
              {{ $t('financePlatformMargin') }}
            </div>
            <strong class="main-stat-value text-gradient-indigo" style="font-size: 2rem; font-weight: 900; display: block;">{{ formatCurrency(safeDashboard.platformMargin) }}</strong>
            <div class="main-stat-meta" style="font-size: 0.75rem; color: var(--qd-muted);">
              {{ $t('financeTakeRate') }}: <span class="page-chip info" style="font-size: 0.65rem; padding: 2px 8px;">{{ formatPercent(safeDashboard.platformTakeRate) }}</span>
            </div>
          </div>
        </div>
      </header>

      <section class="premium-stats-grid">
        <PremiumDashboardCard
          v-for="stat in financeStats"
          :key="stat.label"
          variant="glass"
          :tone="stat.tone"
          class="f-stat-card"
        >
          <div class="f-stat-body">
            <span class="f-stat-label">{{ stat.label }}</span>
            <strong class="f-stat-value">{{ formatCurrency(stat.value) }}</strong>
          </div>
        </PremiumDashboardCard>
      </section>

      <div class="finance-content-layout">
        <section class="main-column">
          <div class="charts-grid">
            <DashboardTrendChart
              :title="$t('financeRevenueChartTitle')"
              :subtitle="$t('financeRevenueChartSubtitle')"
              :points="customerRevenuePoints"
              tone="var(--qd-primary)"
              :formatter="formatCurrency"
              :empty-label="$t('dashboardChartEmpty')"
            />
            <DashboardTrendChart
              :title="$t('financeMarginChartTitle')"
              :subtitle="$t('financeMarginChartSubtitle')"
              :points="platformMarginPoints"
              tone="var(--qd-success)"
              :formatter="formatCurrency"
              :empty-label="$t('dashboardChartEmpty')"
            />
          </div>
          
          <PremiumDashboardCard :title="$t('financeRecentSettlementsTitle')" variant="glass" class="full-width-card mt-4">
            <div v-if="dashboard.recentSettlements.length" class="premium-table-shell">
              <table class="premium-table">
                <thead>
                  <tr>
                    <th>{{ $t('financeTablePackage') }}</th>
                    <th>{{ $t('financeTableStatus') }}</th>
                    <th>{{ $t('financeTableCustomerTotal') }}</th>
                    <th>{{ $t('financeTableServiceFee') }}</th>
                    <th>{{ $t('financeTableCourierPayout') }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in dashboard.recentSettlements" :key="item.packageReference">
                    <td><code class="ref-id">{{ item.packageReference }}</code></td>
                    <td><span class="badge success outline">{{ item.packageStatus }}</span></td>
                    <td class="fw-bold">{{ formatCurrency(item.customerTotalPrice) }}</td>
                    <td class="text-indigo fw-bold">{{ formatCurrency(item.platformServiceFee) }}</td>
                    <td class="text-success fw-bold">{{ formatCurrency(item.courierPayoutAmount) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </PremiumDashboardCard>
        </section>

        <section class="side-column">
          <PremiumDashboardCard :title="$t('financePayoutHealthTitle')" variant="flat" tone="amber" class="side-panel">
            <div class="payout-metrics">
              <div class="m-row">
                <span>{{ $t('financePendingPayoutCount') }}</span>
                <strong>{{ formatInteger(dashboard.pendingPayoutCount) }}</strong>
              </div>
              <div class="m-row">
                <span>{{ $t('financePaidPayoutCount') }}</span>
                <strong>{{ formatInteger(dashboard.paidPayoutCount) }}</strong>
              </div>
              <div class="m-divider"></div>
              <div class="m-row total">
                <span>{{ $t('financeCourierShareLabel') }}</span>
                <strong class="text-indigo">{{ formatCurrency(totalCourierShare) }}</strong>
              </div>
            </div>
          </PremiumDashboardCard>

          <PremiumDashboardCard :title="$t('financePendingPayoutsTitle')" variant="glass" class="side-panel">
            <div v-if="dashboard.pendingPayouts.length" class="mini-stack">
              <div v-for="item in dashboard.pendingPayouts" :key="item.deliveryPersonId" class="mini-item">
                <div class="mini-item-info">
                  <strong>{{ item.deliveryPersonName }}</strong>
                  <small>{{ item.packageCount }} {{ $t('totalPackages') }}</small>
                </div>
                <div class="mini-item-amount">{{ formatCurrency(item.amount) }}</div>
              </div>
            </div>
            <div v-else class="empty-compact">{{ $t('financeNoPendingPayouts') }}</div>
          </PremiumDashboardCard>

          <PremiumDashboardCard :title="$t('financePenaltiesTitle')" variant="glass" class="side-panel">
            <div class="payout-metrics mb-4">
              <div class="m-row">
                <span>{{ $t('financePenaltyCount') }}</span>
                <strong>{{ formatInteger(dashboard.courierPenaltyCount) }}</strong>
              </div>
              <div class="m-row total">
                <span>{{ $t('financeCourierPenaltiesAmount') }}</span>
                <strong class="text-danger">{{ formatCurrency(dashboard.courierFinancialPenaltyAmount) }}</strong>
              </div>
            </div>
            <div v-if="dashboard.recentCourierPenalties?.length" class="mini-stack border-danger">
              <div v-for="item in dashboard.recentCourierPenalties" :key="item.id" class="mini-item danger">
                <div class="mini-item-info">
                  <strong>{{ item.deliveryPersonName || item.deliveryPersonEmail || '-' }}</strong>
                  <small>{{ formatPenaltyType(item.type) }}</small>
                </div>
                <div class="mini-item-amount text-danger">{{ formatCurrency(item.financialPenaltyAmount) }}</div>
              </div>
            </div>
            <router-link class="qd-btn-secondary full-width mt-3" to="/dashboard/courier-penalties" style="justify-content: center; height: 42px; border-radius: 12px;">
              {{ $t('financeOpenPenaltiesPage') }}
              <span class="material-symbols-outlined">trending_flat</span>
            </router-link>
          </PremiumDashboardCard>
        </section>
      </div>
    </template>
  </div>
</template>

<script>
import http from '@/config/httpInterceptor';
import DashboardTrendChart from '../components/DashboardTrendChart.vue';
import PremiumDashboardCard from '../components/PremiumDashboardCard.vue';
import { getAccessToken } from '../config/auth';

const CHART_START_X = 32;
const CHART_STEP_X = 42;
const CHART_HEIGHT = 160;
const CHART_TOP = 20;

export default {
  name: 'FinanceDashboardPage',
  components: {
    DashboardTrendChart,
    PremiumDashboardCard,
  },
  data() {
    return {
      isLoading: false,
      loadError: false,
      dashboard: null,
    };
  },
  computed: {
    currentLocale() {
      const locale = this.$i18n?.locale;
      return typeof locale === 'string' ? locale : locale?.value || 'fr';
    },
    safeDashboard() {
      return this.dashboard || { platformMargin: 0, platformTakeRate: 0, customerRevenueTrend: [], platformMarginTrend: [], recentSettlements: [], pendingPayouts: [] };
    },
    financeStats() {
      const d = this.safeDashboard;
      return [
        { label: this.$t('financeCustomerRevenue'), value: d.customerRevenue, tone: 'indigo' },
        { label: this.$t('financeServiceFees'), value: d.platformServiceFees, tone: 'slate' },
        { label: this.$t('financeCommissionAmount'), value: d.platformCommissionAmount, tone: 'amber' },
        { label: this.$t('financePaidPayoutAmount'), value: d.courierPayoutPaidAmount, tone: 'emerald' },
        { label: this.$t('financeCourierPenaltiesAmount'), value: d.courierFinancialPenaltyAmount, tone: 'rose' },
      ];
    },
    customerRevenuePoints() { return this.buildChartPoints(this.safeDashboard.customerRevenueTrend || []); },
    platformMarginPoints() { return this.buildChartPoints(this.safeDashboard.platformMarginTrend || []); },
    totalCourierShare() {
      return Number(this.safeDashboard.courierPayoutPendingAmount || 0) + Number(this.safeDashboard.courierPayoutPaidAmount || 0);
    },
  },
  mounted() {
    this.fetchDashboard();
  },
  methods: {
    async fetchDashboard() {
      this.isLoading = true;
      try {
        const response = await http.get(`${this.$i18n.t('rootURL')}${this.$i18n.t('getAdminPackageFinancialDashboard')}`, {
          headers: { Authorization: `Bearer ${getAccessToken()}` }
        });
        this.dashboard = response.data;
      } catch (error) {
        this.loadError = true;
      } finally {
        this.isLoading = false;
      }
    },
    buildChartPoints(items) {
      const values = items.map(item => Number(item.value || 0));
      const max = Math.max(0.1, ...values);
      return items.map((item, index) => ({
        label: this.formatPeriod(item.periodKey),
        value: item.value,
        x: CHART_START_X + (index * CHART_STEP_X),
        y: CHART_TOP + CHART_HEIGHT - ((Number(item.value || 0) / max) * CHART_HEIGHT)
      }));
    },
    formatPeriod(pk) {
      if (!pk) return '-';
      const [y, m] = pk.split('-').map(Number);
      return new Intl.DateTimeFormat(this.currentLocale, { month: 'short' }).format(new Date(y, m - 1, 1));
    },
    formatCurrency(v) {
      return new Intl.NumberFormat(this.currentLocale, { style: 'currency', currency: this.dashboard?.currency || 'EUR' }).format(v || 0);
    },
    formatPenaltyType(type) {
      return this.$t(`courierPenaltyType_${type || 'UNKNOWN'}`);
    },
    formatInteger(v) { return new Intl.NumberFormat(this.currentLocale).format(v || 0); },
    formatPercent(v) { return new Intl.NumberFormat(this.currentLocale, { style: 'percent', maximumFractionDigits: 1 }).format(v || 0); },
  }
};
</script>

<style scoped>
.text-gradient-indigo {
  background: linear-gradient(135deg, var(--qd-primary-dark), var(--qd-primary));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.premium-stats-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
  margin-bottom: 32px;
}

.f-stat-body {
  display: flex;
  flex-direction: column;
}

.f-stat-label {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--qd-muted);
  margin-bottom: 6px;
  text-transform: uppercase;
}

.f-stat-value {
  font-size: 1.6rem;
  font-weight: 800;
}

.finance-content-layout {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: 32px;
}

.charts-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.ref-id {
  font-family: monospace;
  background: var(--qd-bg);
  padding: 4px 10px;
  border-radius: 6px;
  font-weight: 700;
}

.text-indigo { color: var(--qd-primary); }
.text-success { color: var(--qd-success); }
.text-danger { color: var(--qd-danger); }
.fw-bold { font-weight: 800; }

.side-panel {
  margin-bottom: 24px;
}

.payout-metrics {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.m-row {
  display: flex;
  justify-content: space-between;
}

.m-row span { color: var(--qd-muted); font-weight: 600; }
.m-row strong { color: var(--qd-text); }

.m-divider { height: 1px; background: var(--qd-border); margin: 4px 0; }

.m-row.total span { color: var(--qd-text); font-weight: 800; }
.m-row.total strong { font-size: 1.2rem; }

.mini-stack {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.mini-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: var(--qd-bg);
  border-radius: 14px;
  border: 1px solid var(--qd-border);
}

.mini-item-info strong { display: block; font-size: 0.95rem; }
.mini-item-info small { font-size: 0.75rem; color: var(--qd-muted); }
.mini-item-amount { font-weight: 800; font-size: 1rem; }

.mini-item.danger {
  border-left: 3px solid var(--qd-danger);
}

.full-width { width: 100%; justify-content: center; }

.large-icon { font-size: 4rem; color: var(--qd-primary-soft); margin-bottom: 16px; }

.premium-spinner {
  width: 40px; height: 40px; border: 4px solid var(--qd-primary-soft); border-top-color: var(--qd-primary); border-radius: 50%; animation: qd-spin 1s linear infinite; margin-bottom: 16px;
}

@keyframes qd-spin { to { transform: rotate(360deg); } }

@media (max-width: 1400px) {
  .premium-stats-grid { grid-template-columns: repeat(3, 1fr); }
}

@media (max-width: 1200px) {
  .finance-content-layout { grid-template-columns: 1fr; }
}

@media (max-width: 768px) {
  .finance-dashboard-page { padding: 12px; width: 100%; box-sizing: border-box; overflow-x: hidden; }
  .finance-hero { flex-direction: column; align-items: stretch; text-align: center; gap: 14px; max-width: 100%; }
  .hero-left h1 { font-size: 1.55rem; line-height: 1.1; overflow-wrap: break-word; }
  .hero-left p { margin-left: auto; margin-right: auto; font-size: 0.88rem; line-height: 1.35; }
  
  .hero-main-card { 
    text-align: center; 
    white-space: normal; 
    padding: 12px !important;
    min-width: 0 !important;
    width: 100%;
    box-sizing: border-box;
  }
  .main-stat-label { justify-content: center; }
  .main-stat-value { font-size: 1.35rem !important; line-height: 1.1; }
  
  .premium-stats-grid { 
    grid-template-columns: repeat(2, 1fr); 
    gap: 10px;
    width: 100%;
  }
  .f-stat-value { font-size: 1.25rem; }
  .f-stat-label { font-size: 0.65rem; }
  
  .charts-grid { grid-template-columns: 1fr; gap: 12px; width: 100%; }
  .finance-content-layout { grid-template-columns: 1fr; gap: 14px; width: 100%; }
  .main-column, .side-column { width: 100%; min-width: 0; }
  
  .premium-table-shell {
    overflow-x: auto;
    width: 100%;
    -webkit-overflow-scrolling: touch;
    border-radius: 12px;
  }
  .premium-table {
    min-width: 550px;
  }
  .large-icon { font-size: 2.25rem; margin-bottom: 8px; }
}

@media (max-width: 480px) {
  .hero-left h1 { font-size: 1.6rem; }
}
</style>
