<template>
  <div class="finance-dashboard-page">
    <template v-if="isLoading">
      <section class="hero-card">
        <div>
          <span class="hero-chip">{{ $t('menuAdminFinance') }}</span>
          <h1>{{ $t('financePageTitle') }}</h1>
          <p>{{ $t('financePageSubtitle') }}</p>
        </div>
      </section>
      <div class="page-state">{{ $t('stateLoading') }}</div>
    </template>

    <template v-else-if="loadError && !dashboard">
      <section class="hero-card">
        <div>
          <span class="hero-chip">{{ $t('menuAdminFinance') }}</span>
          <h1>{{ $t('financePageTitle') }}</h1>
          <p>{{ $t('financePageSubtitle') }}</p>
        </div>
      </section>
      <div class="page-state error">{{ $t('stateLoadError') }}</div>
    </template>

    <template v-else-if="dashboard">
      <section class="hero-card">
        <div>
          <span class="hero-chip">{{ $t('menuAdminFinance') }}</span>
          <h1>{{ $t('financePageTitle') }}</h1>
          <p>{{ $t('financePageSubtitle') }}</p>
        </div>
        <div class="hero-side">
          <strong>{{ formatCurrency(safeDashboard.platformMargin) }}</strong>
          <span>{{ $t('financeMarginLabel') }}</span>
          <small>{{ $t('financePageLastRefresh') }}: {{ formatDateTime(safeDashboard.generatedAt) }}</small>
        </div>
      </section>

      <section class="stats-grid">
        <article class="stat-card tone-dark">
          <strong>{{ formatCurrency(dashboard.customerRevenue) }}</strong>
          <span>{{ $t('financeCustomerRevenue') }}</span>
        </article>
        <article class="stat-card tone-success">
          <strong>{{ formatCurrency(dashboard.platformMargin) }}</strong>
          <span>{{ $t('financePlatformMargin') }}</span>
        </article>
        <article class="stat-card tone-neutral">
          <strong>{{ formatCurrency(dashboard.platformServiceFees) }}</strong>
          <span>{{ $t('financeServiceFees') }}</span>
        </article>
        <article class="stat-card tone-warn">
          <strong>{{ formatCurrency(dashboard.platformCommissionAmount) }}</strong>
          <span>{{ $t('financeCommissionAmount') }}</span>
        </article>
        <article class="stat-card tone-dark">
          <strong>{{ formatCurrency(dashboard.courierPayoutPendingAmount) }}</strong>
          <span>{{ $t('financePendingPayoutAmount') }}</span>
        </article>
        <article class="stat-card tone-success">
          <strong>{{ formatCurrency(dashboard.courierPayoutPaidAmount) }}</strong>
          <span>{{ $t('financePaidPayoutAmount') }}</span>
        </article>
      </section>

      <section class="dashboard-grid finance-overview-grid">
        <article class="panel-card">
          <div class="panel-head">
            <h2>{{ $t('financeOverviewTitle') }}</h2>
            <p>{{ $t('financeOverviewSubtitle') }}</p>
          </div>
          <div class="metric-list">
            <div class="metric-row">
              <strong>{{ $t('financeSettlementsCount') }}</strong>
              <span>{{ formatInteger(dashboard.settlementCount) }}</span>
            </div>
            <div class="metric-row">
              <strong>{{ $t('financeDeliveredPackagesCount') }}</strong>
              <span>{{ formatInteger(dashboard.deliveredPackageCount) }}</span>
            </div>
            <div class="metric-row">
              <strong>{{ $t('financeAverageOrderValue') }}</strong>
              <span>{{ formatCurrency(dashboard.averageOrderValue) }}</span>
            </div>
            <div class="metric-row">
              <strong>{{ $t('financeAverageCourierPayout') }}</strong>
              <span>{{ formatCurrency(dashboard.averageCourierPayout) }}</span>
            </div>
            <div class="metric-row">
              <strong>{{ $t('financeTakeRate') }}</strong>
              <span>{{ formatPercent(dashboard.platformTakeRate) }}</span>
            </div>
          </div>
        </article>

        <article class="panel-card">
          <div class="panel-head">
            <h2>{{ $t('financePayoutHealthTitle') }}</h2>
            <p>{{ $t('financePayoutHealthSubtitle') }}</p>
          </div>
          <div class="metric-list">
            <div class="metric-row">
              <strong>{{ $t('financePendingPayoutCount') }}</strong>
              <span>{{ formatInteger(dashboard.pendingPayoutCount) }}</span>
            </div>
            <div class="metric-row">
              <strong>{{ $t('financePaidPayoutCount') }}</strong>
              <span>{{ formatInteger(dashboard.paidPayoutCount) }}</span>
            </div>
            <div class="metric-row">
              <strong>{{ $t('financeCourierShareLabel') }}</strong>
              <span>{{ formatCurrency(totalCourierShare) }}</span>
            </div>
            <div class="metric-row">
              <strong>{{ $t('financePlatformShareLabel') }}</strong>
              <span>{{ formatCurrency(dashboard.platformMargin) }}</span>
            </div>
          </div>
        </article>
      </section>

      <section class="chart-grid">
        <DashboardTrendChart
          :title="$t('financeRevenueChartTitle')"
          :subtitle="$t('financeRevenueChartSubtitle')"
          :points="customerRevenuePoints"
          tone="#1f5fae"
          :formatter="formatCurrency"
          :empty-label="$t('dashboardChartEmpty')"
        />
        <DashboardTrendChart
          :title="$t('financeMarginChartTitle')"
          :subtitle="$t('financeMarginChartSubtitle')"
          :points="platformMarginPoints"
          tone="#ef7d32"
          :formatter="formatCurrency"
          :empty-label="$t('dashboardChartEmpty')"
        />
        <DashboardTrendChart
          :title="$t('financePayoutChartTitle')"
          :subtitle="$t('financePayoutChartSubtitle')"
          :points="courierPayoutPoints"
          tone="#159947"
          :formatter="formatCurrency"
          :empty-label="$t('dashboardChartEmpty')"
        />
      </section>

      <section class="dashboard-grid finance-tables-grid">
        <article class="panel-card">
          <div class="panel-head">
            <h2>{{ $t('financePendingPayoutsTitle') }}</h2>
            <p>{{ $t('financePendingPayoutsSubtitle') }}</p>
          </div>
          <div v-if="dashboard.pendingPayouts.length" class="table-shell">
            <table class="finance-table">
              <thead>
                <tr>
                  <th>{{ $t('financeTableCourier') }}</th>
                  <th>{{ $t('financeTablePackages') }}</th>
                  <th>{{ $t('financeTableOldest') }}</th>
                  <th>{{ $t('financeTableAmount') }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in dashboard.pendingPayouts" :key="`${item.deliveryPersonId || 'unknown'}-${item.oldestCreatedAt}`">
                  <td>{{ item.deliveryPersonName }}</td>
                  <td>{{ formatInteger(item.packageCount) }}</td>
                  <td>{{ formatDateTime(item.oldestCreatedAt) }}</td>
                  <td>{{ formatCurrency(item.amount) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="empty-state">{{ $t('financeNoPendingPayouts') }}</div>
        </article>

        <article class="panel-card">
          <div class="panel-head">
            <h2>{{ $t('financeRecentSettlementsTitle') }}</h2>
            <p>{{ $t('financeRecentSettlementsSubtitle') }}</p>
          </div>
          <div v-if="dashboard.recentSettlements.length" class="table-shell">
            <table class="finance-table">
              <thead>
                <tr>
                  <th>{{ $t('financeTablePackage') }}</th>
                  <th>{{ $t('financeTableStatus') }}</th>
                  <th>{{ $t('financeTableCalculatedAt') }}</th>
                  <th>{{ $t('financeTableCustomerTotal') }}</th>
                  <th>{{ $t('financeTableServiceFee') }}</th>
                  <th>{{ $t('financeTableCommission') }}</th>
                  <th>{{ $t('financeTableCourierPayout') }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in dashboard.recentSettlements" :key="`${item.packageReference}-${item.calculatedAt}`">
                  <td>{{ item.packageReference }}</td>
                  <td>{{ item.packageStatus }}</td>
                  <td>{{ formatDateTime(item.calculatedAt) }}</td>
                  <td>{{ formatCurrency(item.customerTotalPrice) }}</td>
                  <td>{{ formatCurrency(item.platformServiceFee) }}</td>
                  <td>{{ formatCurrency(item.platformCommissionAmount) }}</td>
                  <td>{{ formatCurrency(item.courierPayoutAmount) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="empty-state">{{ $t('financeNoSettlements') }}</div>
        </article>
      </section>
    </template>
  </div>
</template>

<script>
import http from '@/config/httpInterceptor';
import DashboardTrendChart from '../components/DashboardTrendChart.vue';
import { getAccessToken } from '../config/auth';

const CHART_START_X = 32;
const CHART_STEP_X = 55;
const CHART_HEIGHT = 180;
const CHART_TOP = 16;

export default {
  components: {
    DashboardTrendChart,
  },
  data() {
    return {
      isLoading: false,
      loadError: false,
      dashboard: null,
    };
  },
  computed: {
    safeDashboard() {
      return this.dashboard || {
        generatedAt: null,
        currency: 'EUR',
        settlementCount: 0,
        deliveredPackageCount: 0,
        pendingPayoutCount: 0,
        paidPayoutCount: 0,
        customerRevenue: 0,
        platformServiceFees: 0,
        platformCommissionAmount: 0,
        platformMargin: 0,
        courierPayoutPendingAmount: 0,
        courierPayoutPaidAmount: 0,
        averageOrderValue: 0,
        averageCourierPayout: 0,
        platformTakeRate: 0,
        customerRevenueTrend: [],
        platformMarginTrend: [],
        courierPayoutTrend: [],
        recentSettlements: [],
        pendingPayouts: [],
      };
    },
    customerRevenuePoints() {
      return this.buildChartPoints(this.safeDashboard.customerRevenueTrend || []);
    },
    platformMarginPoints() {
      return this.buildChartPoints(this.safeDashboard.platformMarginTrend || []);
    },
    courierPayoutPoints() {
      return this.buildChartPoints(this.safeDashboard.courierPayoutTrend || []);
    },
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
      this.loadError = false;
      try {
        const headers = {
          Authorization: `Bearer ${getAccessToken()}`,
        };
        const response = await http.get(
          `${this.$i18n.t('rootURL')}${this.$i18n.t('getAdminPackageFinancialDashboard')}`,
          { headers },
        );
        this.dashboard = response.data;
      } catch (error) {
        this.loadError = true;
        console.error('Unable to load finance dashboard', error);
      } finally {
        this.isLoading = false;
      }
    },
    buildChartPoints(items) {
      const values = items.map((item) => Number(item.value || 0));
      const maxValue = Math.max(0, ...values);
      return items.map((item, index) => {
        const value = Number(item.value || 0);
        return {
          label: this.formatPeriod(item.periodKey),
          value,
          x: CHART_START_X + (index * CHART_STEP_X),
          y: CHART_TOP + CHART_HEIGHT - (maxValue <= 0 ? 0 : ((value / maxValue) * CHART_HEIGHT)),
        };
      });
    },
    formatPeriod(periodKey) {
      if (!periodKey) {
        return '-';
      }
      const [year, month] = periodKey.split('-').map((value) => Number(value));
      if (!year || !month) {
        return periodKey;
      }
      return new Intl.DateTimeFormat(this.currentLocale(), {
        month: 'short',
      }).format(new Date(year, month - 1, 1));
    },
    currentLocale() {
      const locale = this.$i18n?.locale;
      return typeof locale === 'string' ? locale : locale?.value || 'fr';
    },
    formatCurrency(value) {
      return new Intl.NumberFormat(this.currentLocale(), {
        style: 'currency',
        currency: this.dashboard?.currency || 'EUR',
        minimumFractionDigits: 2,
      }).format(Number(value || 0));
    },
    formatInteger(value) {
      return new Intl.NumberFormat(this.currentLocale(), {
        maximumFractionDigits: 0,
      }).format(Number(value || 0));
    },
    formatPercent(value) {
      return new Intl.NumberFormat(this.currentLocale(), {
        style: 'percent',
        maximumFractionDigits: 1,
      }).format(Number(value || 0));
    },
    formatDateTime(value) {
      if (!value) {
        return '-';
      }
      return new Intl.DateTimeFormat(this.currentLocale(), {
        dateStyle: 'medium',
        timeStyle: 'short',
      }).format(new Date(value));
    },
  },
};
</script>

<style scoped>
.finance-dashboard-page {
  display: grid;
  gap: 24px;
  padding: 24px;
  background:
    radial-gradient(circle at top left, rgba(239, 125, 50, 0.12), transparent 28%),
    linear-gradient(180deg, #f8fafc 0%, #eef3f9 100%);
  min-height: calc(100vh - 62px);
}

.hero-card,
.panel-card,
.stat-card {
  border: 1px solid #dde3ec;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08);
}

.hero-card {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  padding: 26px 28px;
  align-items: flex-start;
}

.hero-chip {
  display: inline-flex;
  align-items: center;
  padding: 7px 12px;
  border-radius: 999px;
  background: #f97316;
  color: #fff;
  font-size: 0.78rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.hero-card h1 {
  margin: 12px 0 10px;
  font-size: clamp(2rem, 3vw, 2.7rem);
  color: #0f172a;
}

.hero-card p,
.hero-side span,
.hero-side small {
  color: #64748b;
}

.hero-side {
  display: grid;
  gap: 6px;
  text-align: right;
}

.hero-side strong {
  color: #159947;
  font-size: 1.5rem;
}

.stats-grid,
.chart-grid,
.dashboard-grid {
  display: grid;
  gap: 18px;
}

.stats-grid {
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
}

.stat-card,
.panel-card {
  padding: 18px 20px;
}

.stat-card strong,
.stat-card span {
  display: block;
}

.stat-card strong {
  margin-bottom: 8px;
  color: #0f172a;
  font-size: 1.4rem;
}

.stat-card span {
  color: #64748b;
}

.tone-dark {
  border-color: rgba(15, 23, 42, 0.12);
}

.tone-success {
  border-color: rgba(21, 153, 71, 0.25);
}

.tone-neutral {
  border-color: rgba(31, 95, 174, 0.22);
}

.tone-warn {
  border-color: rgba(239, 125, 50, 0.28);
}

.finance-overview-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.finance-tables-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.chart-grid {
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
}

.panel-head {
  margin-bottom: 16px;
}

.panel-head h2 {
  margin: 0 0 6px;
  color: #0f172a;
}

.panel-head p {
  margin: 0;
  color: #64748b;
}

.metric-list {
  display: grid;
  gap: 10px;
}

.metric-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 14px;
  background: #f8fafc;
  border: 1px solid #e5ebf3;
}

.metric-row strong {
  color: #0f172a;
}

.metric-row span {
  color: #1f5fae;
  font-weight: 700;
}

.table-shell {
  overflow-x: auto;
}

.finance-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 640px;
}

.finance-table th,
.finance-table td {
  padding: 12px 10px;
  border-bottom: 1px solid #e5ebf3;
  text-align: left;
  white-space: nowrap;
}

.finance-table th {
  color: #475569;
  font-size: 0.82rem;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.finance-table td {
  color: #0f172a;
}

.page-state,
.empty-state {
  padding: 18px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.85);
  color: #64748b;
  text-align: center;
}

.page-state.error {
  color: #b91c1c;
}

@media (max-width: 1024px) {
  .finance-overview-grid,
  .finance-tables-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .finance-dashboard-page {
    padding: 16px;
  }

  .hero-card {
    flex-direction: column;
    align-items: stretch;
  }

  .hero-side {
    text-align: left;
  }
}
</style>
