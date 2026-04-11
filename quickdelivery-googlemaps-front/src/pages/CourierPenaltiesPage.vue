<template>
  <div class="courier-penalties-page">
    <header class="penalty-header">
      <div>
        <span class="header-chip">{{ $t('menuAdminPenalties') }}</span>
        <h1>{{ $t('penaltiesPageTitle') }}</h1>
        <p>{{ $t('penaltiesPageSubtitle') }}</p>
      </div>
      <router-link class="header-link" to="/dashboard/finance">
        {{ $t('penaltiesBackToFinance') }}
      </router-link>
    </header>

    <section class="penalty-kpis">
      <article v-for="item in kpis" :key="item.label" class="kpi-item" :class="item.tone">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </article>
    </section>

    <section class="penalty-workspace">
      <div class="toolbar">
        <label>
          <span>{{ $t('penaltiesFilterLabel') }}</span>
          <select v-model="statusFilter">
            <option value="ALL">{{ $t('penaltiesFilterAll') }}</option>
            <option value="ACTIVE">{{ $t('ACTIVE') }}</option>
            <option value="REVIEWED">{{ $t('REVIEWED') }}</option>
            <option value="CANCELLED">{{ $t('CANCELLED') }}</option>
          </select>
        </label>
        <button type="button" @click="fetchPenalties">
          {{ $t('penaltiesRefresh') }}
        </button>
      </div>

      <div v-if="isLoading" class="page-state">{{ $t('stateLoading') }}</div>
      <div v-else-if="loadError" class="page-state error">{{ $t('stateLoadError') }}</div>
      <div v-else-if="!filteredPenalties.length" class="page-state">{{ $t('penaltiesEmpty') }}</div>

      <div v-else class="table-shell">
        <table class="penalty-table">
          <thead>
            <tr>
              <th>{{ $t('penaltiesCourier') }}</th>
              <th>{{ $t('penaltiesType') }}</th>
              <th>{{ $t('penaltiesSeverity') }}</th>
              <th>{{ $t('penaltiesAmount') }}</th>
              <th>{{ $t('penaltiesSuspensionUntil') }}</th>
              <th>{{ $t('penaltiesCreatedAt') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in filteredPenalties" :key="item.id">
              <td>
                <strong>{{ item.deliveryPersonName || '-' }}</strong>
                <span>{{ item.deliveryPersonEmail || '-' }}</span>
              </td>
              <td>
                <span class="type-pill">{{ formatPenaltyType(item.type) }}</span>
                <small v-if="item.packageReference">#{{ item.packageReference }}</small>
              </td>
              <td>
                <span class="severity-pill" :class="`severity-${item.severity || 0}`">{{ item.severity || 0 }}</span>
              </td>
              <td class="amount">{{ formatCurrency(item.financialPenaltyAmount, item.currency) }}</td>
              <td :class="{ suspended: isSuspended(item) }">{{ formatDate(item.suspensionUntil) }}</td>
              <td>{{ formatDate(item.createdAt) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<script>
import http from '@/config/httpInterceptor';
import { getAccessToken } from '../config/auth';

export default {
  name: 'CourierPenaltiesPage',
  data() {
    return {
      isLoading: false,
      loadError: false,
      penalties: [],
      statusFilter: 'ALL',
    };
  },
  computed: {
    currentLocale() {
      const locale = this.$i18n?.locale;
      return typeof locale === 'string' ? locale : locale?.value || 'fr';
    },
    filteredPenalties() {
      if (this.statusFilter === 'ALL') {
        return this.penalties;
      }
      return this.penalties.filter((item) => item.status === this.statusFilter);
    },
    kpis() {
      const active = this.penalties.filter((item) => item.status === 'ACTIVE');
      const suspended = active.filter(this.isSuspended);
      const amount = active.reduce((sum, item) => sum + Number(item.financialPenaltyAmount || 0), 0);
      return [
        { label: this.$t('penaltiesKpiTotal'), value: this.formatInteger(this.penalties.length), tone: 'slate' },
        { label: this.$t('penaltiesKpiActive'), value: this.formatInteger(active.length), tone: 'amber' },
        { label: this.$t('penaltiesKpiSuspended'), value: this.formatInteger(suspended.length), tone: 'rose' },
        { label: this.$t('penaltiesKpiAmount'), value: this.formatCurrency(amount, 'EUR'), tone: 'emerald' },
      ];
    },
  },
  mounted() {
    this.fetchPenalties();
  },
  methods: {
    async fetchPenalties() {
      this.isLoading = true;
      this.loadError = false;
      try {
        const response = await http.get(`${this.$i18n.t('rootURL')}${this.$i18n.t('getAdminCourierPenalties')}?limit=200`, {
          headers: { Authorization: `Bearer ${getAccessToken()}` },
        });
        this.penalties = Array.isArray(response.data) ? response.data : [];
      } catch (error) {
        this.loadError = true;
      } finally {
        this.isLoading = false;
      }
    },
    isSuspended(item) {
      return item?.suspensionUntil && new Date(item.suspensionUntil).getTime() > Date.now();
    },
    formatPenaltyType(type) {
      return this.$t(`courierPenaltyType_${type || 'UNKNOWN'}`);
    },
    formatCurrency(value, currency) {
      return new Intl.NumberFormat(this.currentLocale, { style: 'currency', currency: currency || 'EUR' }).format(value || 0);
    },
    formatInteger(value) {
      return new Intl.NumberFormat(this.currentLocale).format(value || 0);
    },
    formatDate(value) {
      if (!value) {
        return '-';
      }
      return new Intl.DateTimeFormat(this.currentLocale, {
        dateStyle: 'short',
        timeStyle: 'short',
      }).format(new Date(value));
    },
  },
};
</script>

<style scoped>
.courier-penalties-page {
  min-height: 100vh;
  padding: 32px;
  background: #f8fafc;
  color: #0f172a;
}

.penalty-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 24px;
  margin-bottom: 28px;
}

.header-chip {
  display: inline-flex;
  margin-bottom: 12px;
  padding: 6px 14px;
  border-radius: 999px;
  background: #fff1f2;
  color: #be123c;
  font-size: 0.78rem;
  font-weight: 900;
  text-transform: uppercase;
}

.penalty-header h1 {
  margin: 0;
  font-size: 2.7rem;
  letter-spacing: -0.04em;
}

.penalty-header p {
  margin: 10px 0 0;
  max-width: 760px;
  color: #64748b;
}

.header-link,
.toolbar button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 42px;
  padding: 0 16px;
  border-radius: 14px;
  border: 1px solid #fecdd3;
  background: #fff;
  color: #be123c;
  text-decoration: none;
  font-weight: 900;
}

.penalty-kpis {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 22px;
}

.kpi-item {
  padding: 20px;
  border-radius: 20px;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.kpi-item span {
  display: block;
  color: #64748b;
  font-size: 0.85rem;
  font-weight: 800;
}

.kpi-item strong {
  display: block;
  margin-top: 10px;
  font-size: 1.8rem;
  letter-spacing: -0.03em;
}

.kpi-item.rose { border-color: #fecdd3; background: #fff1f2; }
.kpi-item.amber { border-color: #fde68a; background: #fffbeb; }
.kpi-item.emerald { border-color: #bbf7d0; background: #f0fdf4; }

.penalty-workspace {
  border-radius: 24px;
  background: #fff;
  border: 1px solid #e5e7eb;
  overflow: hidden;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 18px;
  border-bottom: 1px solid #e5e7eb;
}

.toolbar label {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #64748b;
  font-weight: 800;
}

.toolbar select {
  height: 42px;
  border: 1px solid #dbe3ef;
  border-radius: 12px;
  padding: 0 12px;
  background: #fff;
}

.table-shell {
  overflow: auto;
}

.penalty-table {
  width: 100%;
  border-collapse: collapse;
}

.penalty-table th,
.penalty-table td {
  padding: 16px 18px;
  border-bottom: 1px solid #f1f5f9;
  text-align: left;
  vertical-align: top;
}

.penalty-table th {
  color: #94a3b8;
  font-size: 0.75rem;
  text-transform: uppercase;
}

.penalty-table td span,
.penalty-table td small {
  display: block;
  margin-top: 4px;
  color: #64748b;
}

.type-pill {
  display: inline-flex !important;
  width: fit-content;
  margin: 0 0 4px;
  padding: 5px 10px;
  border-radius: 999px;
  background: #f1f5f9;
  color: #334155 !important;
  font-size: 0.75rem;
  font-weight: 900;
}

.severity-pill {
  display: inline-flex !important;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 999px;
  background: #e2e8f0;
  color: #0f172a !important;
  font-weight: 900;
}

.severity-3,
.severity-4 {
  background: #ffe4e6;
  color: #be123c !important;
}

.amount,
.suspended {
  color: #be123c;
  font-weight: 900;
}

.page-state {
  padding: 42px;
  color: #64748b;
  text-align: center;
}

.page-state.error {
  color: #be123c;
}

@media (max-width: 900px) {
  .courier-penalties-page {
    padding: 18px;
  }

  .penalty-header,
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .penalty-header h1 {
    font-size: 2.1rem;
  }

  .penalty-kpis {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 560px) {
  .penalty-kpis {
    grid-template-columns: 1fr;
  }
}
</style>
