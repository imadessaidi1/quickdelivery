<template>
  <div class="finance-dashboard-page">
    <template v-if="isLoading">
      <header class="hero-section">
        <div class="hero-content">
          <span class="hero-chip">{{ $t('menuAdminFinance') }}</span>
          <h1>{{ $t('financePageTitle') }}</h1>
          <p>{{ $t('financePageSubtitle') }}</p>
        </div>
      </header>
      <div class="page-state loading-state">
        <div class="spinner"></div>
        <span>{{ $t('stateLoading') }}</span>
      </div>
    </template>

    <template v-else-if="loadError && !dashboard">
      <header class="hero-section">
        <div class="hero-content">
          <span class="hero-chip">{{ $t('menuAdminFinance') }}</span>
          <h1>{{ $t('financePageTitle') }}</h1>
        </div>
      </header>
      <div class="page-state error-state">
        <span class="material-symbols-outlined">payments</span>
        <p>{{ $t('stateLoadError') }}</p>
      </div>
    </template>

    <template v-else-if="dashboard">
      <header class="hero-section premium-finance-hero">
        <div class="hero-content">
          <span class="hero-chip luxury-chip">{{ $t('menuAdminFinance') }}</span>
          <h1>{{ $t('financePageTitle') }}</h1>
          <p>{{ $t('financePageSubtitle') }}</p>
        </div>
        
        <div class="hero-main-stat">
          <div class="main-label">
            <span class="material-symbols-outlined">account_balance_wallet</span>
            {{ $t('financePlatformMargin') }}
          </div>
          <strong class="main-amount">{{ formatCurrency(safeDashboard.platformMargin) }}</strong>
          <div class="main-subinfo">
            {{ $t('financeTakeRate') }}: <strong>{{ formatPercent(safeDashboard.platformTakeRate) }}</strong>
          </div>
        </div>
      </header>

      <section class="stats-premium-grid">
        <PremiumDashboardCard
          v-for="stat in financeStats"
          :key="stat.label"
          variant="glass"
          :tone="stat.tone"
          class="finance-stat-card"
        >
          <div class="f-stat-inner">
            <span class="f-label">{{ stat.label }}</span>
            <strong class="f-value">{{ formatCurrency(stat.value) }}</strong>
          </div>
        </PremiumDashboardCard>
      </section>

      <div class="finance-main-layout">
        <section class="charts-column">
          <div class="charts-row">
            <DashboardTrendChart
              :title="$t('financeRevenueChartTitle')"
              :subtitle="$t('financeRevenueChartSubtitle')"
              :points="customerRevenuePoints"
              tone="#6366f1"
              :formatter="formatCurrency"
              :empty-label="$t('dashboardChartEmpty')"
            />
            <DashboardTrendChart
              :title="$t('financeMarginChartTitle')"
              :subtitle="$t('financeMarginChartSubtitle')"
              :points="platformMarginPoints"
              tone="#10b981"
              :formatter="formatCurrency"
              :empty-label="$t('dashboardChartEmpty')"
            />
          </div>
          
          <PremiumDashboardCard :title="$t('financeRecentSettlementsTitle')" variant="glass" class="table-card">
            <div v-if="dashboard.recentSettlements.length" class="table-shell">
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
                    <td><span class="ref-badge">{{ item.packageReference }}</span></td>
                    <td><span class="status-pill">{{ item.packageStatus }}</span></td>
                    <td class="text-bold">{{ formatCurrency(item.customerTotalPrice) }}</td>
                    <td class="text-indigo">{{ formatCurrency(item.platformServiceFee) }}</td>
                    <td class="text-emerald">{{ formatCurrency(item.courierPayoutAmount) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </PremiumDashboardCard>
        </section>

        <section class="payout-column">
          <PremiumDashboardCard :title="$t('financePayoutHealthTitle')" variant="flat" tone="amber" class="payout-summary">
            <div class="payout-metrics">
              <div class="p-row">
                <span>{{ $t('financePendingPayoutCount') }}</span>
                <strong>{{ formatInteger(dashboard.pendingPayoutCount) }}</strong>
              </div>
              <div class="p-row">
                <span>{{ $t('financePaidPayoutCount') }}</span>
                <strong>{{ formatInteger(dashboard.paidPayoutCount) }}</strong>
              </div>
              <div class="p-divider"></div>
              <div class="p-row total">
                <span>{{ $t('financeCourierShareLabel') }}</span>
                <strong>{{ formatCurrency(totalCourierShare) }}</strong>
              </div>
            </div>
          </PremiumDashboardCard>

          <PremiumDashboardCard :title="$t('financePendingPayoutsTitle')" variant="glass" class="pending-table-card">
            <div v-if="dashboard.pendingPayouts.length" class="pending-stack">
              <div v-for="item in dashboard.pendingPayouts" :key="item.deliveryPersonId" class="pending-item">
                <div class="p-item-info">
                  <strong>{{ item.deliveryPersonName }}</strong>
                  <span>{{ item.packageCount }} Colis</span>
                </div>
                <div class="p-item-amount">
                  {{ formatCurrency(item.amount) }}
                </div>
              </div>
            </div>
            <div v-else class="empty-compact">
              <p>{{ $t('financeNoPendingPayouts') }}</p>
            </div>
          </PremiumDashboardCard>

          <PremiumDashboardCard :title="$t('financePenaltiesTitle')" variant="glass" class="penalty-table-card">
            <div class="payout-metrics penalty-summary">
              <div class="p-row">
                <span>{{ $t('financePenaltyCount') }}</span>
                <strong>{{ formatInteger(dashboard.courierPenaltyCount) }}</strong>
              </div>
              <div class="p-row">
                <span>{{ $t('financeActiveSuspensions') }}</span>
                <strong>{{ formatInteger(dashboard.courierActiveSuspensionCount) }}</strong>
              </div>
              <div class="p-row total">
                <span>{{ $t('financeCourierPenaltiesAmount') }}</span>
                <strong>{{ formatCurrency(dashboard.courierFinancialPenaltyAmount) }}</strong>
              </div>
            </div>
            <div v-if="dashboard.recentCourierPenalties?.length" class="pending-stack">
              <div v-for="item in dashboard.recentCourierPenalties" :key="item.id" class="pending-item penalty-item">
                <div class="p-item-info">
                  <strong>{{ item.deliveryPersonName || item.deliveryPersonEmail || '-' }}</strong>
                  <span>{{ formatPenaltyType(item.type) }}</span>
                </div>
                <div class="p-item-amount text-rose">
                  {{ formatCurrency(item.financialPenaltyAmount) }}
                </div>
              </div>
            </div>
            <router-link class="penalty-link" to="/dashboard/courier-penalties">
              {{ $t('financeOpenPenaltiesPage') }}
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
.finance-dashboard-page {
  min-height: 100vh;
  padding: 32px;
  background: #f8fafc;
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.premium-finance-hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 40px;
}

.luxury-chip {
  background: #0f172a;
  color: #fff;
  padding: 6px 16px;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
  margin-bottom: 12px;
  display: inline-block;
}

.hero-content h1 {
  font-size: 3rem;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: -0.03em;
  margin: 0;
}

.hero-main-stat {
  padding: 32px 48px;
  background: #fff;
  border-radius: 24px;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.05);
  text-align: right;
  border: 1px solid rgba(15, 23, 42, 0.05);
}

.main-label {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  font-size: 0.875rem;
  color: #64748b;
  margin-bottom: 8px;
}

.main-label span { color: #10b981; }

.main-amount {
  font-size: 2.5rem;
  font-weight: 900;
  color: #0f172a;
  display: block;
  letter-spacing: -0.02em;
}

.main-subinfo {
  font-size: 0.8125rem;
  color: #94a3b8;
  margin-top: 4px;
}

.main-subinfo strong { color: #0f172a; }

/* Stats Grid */
.stats-premium-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.f-stat-inner {
  display: flex;
  flex-direction: column;
}

.f-label {
  font-size: 0.8125rem;
  font-weight: 600;
  color: #64748b;
  margin-bottom: 4px;
}

.f-value {
  font-size: 1.5rem;
  font-weight: 800;
  color: #0f172a;
}

/* Layout */
.finance-main-layout {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: 32px;
}

.charts-column {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.charts-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.premium-table {
  width: 100%;
  border-collapse: collapse;
}

.premium-table th {
  text-align: left;
  padding: 16px;
  font-size: 0.75rem;
  color: #94a3b8;
  text-transform: uppercase;
  border-bottom: 1px solid #f1f5f9;
}

.premium-table td {
  padding: 16px;
  font-size: 0.9375rem;
  border-bottom: 1px solid #f1f5f9;
}

.ref-badge {
  font-family: monospace;
  background: #f1f5f9;
  padding: 4px 8px;
  border-radius: 6px;
  color: #475569;
}

.status-pill {
  padding: 4px 10px;
  background: #e4f4ea;
  color: #15803d;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 700;
}

.text-bold { font-weight: 700; color: #0f172a; }
.text-indigo { color: #6366f1; font-weight: 600; }
.text-emerald { color: #10b981; font-weight: 600; }
.text-rose { color: #e11d48; font-weight: 800; }

.payout-column {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.payout-metrics {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.p-row {
  display: flex;
  justify-content: space-between;
  font-size: 0.9375rem;
}

.p-divider { height: 1px; background: rgba(0,0,0,0.05); margin: 4px 0; }

.p-row.total {
  font-weight: 700;
  color: #0f172a;
  font-size: 1.1rem;
}

.pending-stack {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.pending-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: rgba(15, 23, 42, 0.03);
  border-radius: 12px;
}

.p-item-info strong { display: block; font-size: 0.9375rem; color: #0f172a; }
.p-item-info span { font-size: 0.75rem; color: #94a3b8; }
.p-item-amount { font-weight: 800; color: #0f172a; }

.penalty-summary {
  margin-bottom: 16px;
}

.penalty-item {
  border-left: 3px solid #e11d48;
}

.penalty-link {
  display: inline-flex;
  align-items: center;
  margin-top: 16px;
  font-weight: 800;
  color: #be123c;
  text-decoration: none;
}

/* States */
.loading-state, .error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100px;
  color: #64748b;
}

.spinner {
  width: 40px; height: 40px; border: 4px solid rgba(0,0,0,0.1); border-top-color: #6366f1; border-radius: 50%; animation: qd-spin 1s linear infinite; margin-bottom: 16px;
}

@keyframes qd-spin { to { transform: rotate(360deg); } }

@media (max-width: 1200px) {
  .finance-main-layout { grid-template-columns: 1fr; }
}

@media (max-width: 768px) {
  .premium-finance-hero { flex-direction: column; align-items: flex-start; }
  .hero-main-stat { width: 100%; text-align: left; }
  .main-label { justify-content: flex-start; }
  .stats-premium-grid { grid-template-columns: repeat(2, 1fr); }
  .charts-row { grid-template-columns: 1fr; }
}
</style>
