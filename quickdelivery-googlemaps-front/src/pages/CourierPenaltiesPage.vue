<template>
  <div class="courier-penalties-page qd-page">
    <header class="qd-page-header">
      <div class="header-main">
        <span class="page-chip danger uppercase">{{ $t('menuAdminPenalties') }}</span>
        <h1>{{ $t('penaltiesPageTitle') }}</h1>
        <p>{{ $t('penaltiesPageSubtitle') }}</p>
      </div>
      <div class="qd-page-header-actions">
        <router-link class="qd-btn-secondary secondary-action" to="/dashboard/finance">
          <span class="material-symbols-outlined">payments</span>
          {{ $t('penaltiesBackToFinance') }}
        </router-link>
      </div>
    </header>

    <section class="premium-kpi-grid">
      <PremiumDashboardCard
        v-for="item in kpis"
        :key="item.label"
        :tone="item.tone"
        variant="glass"
      >
        <div class="kpi-content">
          <span class="kpi-label">{{ item.label }}</span>
          <strong class="kpi-value">{{ item.value }}</strong>
        </div>
      </PremiumDashboardCard>
    </section>

    <section class="panel-card workspace-panel">
      <div class="toolbar">
        <div class="filter-group">
          <span>{{ $t('penaltiesFilterLabel') }}</span>
          <select v-model="statusFilter">
            <option value="ALL">{{ $t('penaltiesFilterAll') }}</option>
            <option value="ACTIVE">{{ $t('ACTIVE') }}</option>
            <option value="REVIEWED">{{ $t('REVIEWED') }}</option>
            <option value="CANCELLED">{{ $t('CANCELLED') }}</option>
          </select>
        </div>
        <div class="actions-group">
          <button class="qd-btn-primary" type="button" @click="showManualModal = true">
            <span class="material-symbols-outlined">add_alert</span>
            {{ $t('penaltiesActionImpose') }}
          </button>
          <button class="qd-btn-secondary" type="button" @click="fetchPenalties" :disabled="isLoading">
            <span class="material-symbols-outlined" :class="{ 'qd-spin': isLoading }">refresh</span>
            {{ $t('penaltiesRefresh') }}
          </button>
        </div>
      </div>

      <div v-if="isLoading" class="table-state">
        <div class="premium-spinner"></div>
        <span>{{ $t('stateLoading') }}</span>
      </div>
      <div v-else-if="loadError" class="table-state error">{{ $t('stateLoadError') }}</div>
      <div v-else-if="!filteredPenalties.length" class="table-state empty">{{ $t('penaltiesEmpty') }}</div>

      <div v-else class="premium-table-shell">
        <table class="premium-table">
          <thead>
            <tr>
              <th>{{ $t('penaltiesCourier') }}</th>
              <th>{{ $t('penaltiesType') }}</th>
              <th>{{ $t('penaltiesSeverity') }}</th>
              <th>{{ $t('penaltiesAmount') }}</th>
              <th>{{ $t('penaltiesSuspensionUntil') }}</th>
              <th>{{ $t('penaltiesCreatedAt') }}</th>
              <th class="actions-th">{{ $t('packageAddressListActions') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in filteredPenalties" :key="item.id">
              <td :data-label="$t('penaltiesCourier')">
                <div class="courier-cell">
                  <strong>{{ item.deliveryPersonName || '-' }}</strong>
                  <small>{{ item.deliveryPersonEmail || '-' }}</small>
                </div>
              </td>
              <td :data-label="$t('penaltiesType')">
                <div class="type-cell">
                  <span class="badge info">{{ formatPenaltyType(item.type) }}</span>
                  <code v-if="item.packageReference" class="ref-code">#{{ item.packageReference }}</code>
                </div>
              </td>
              <td :data-label="$t('penaltiesSeverity')">
                <span class="badge" :class="getSeverityClass(item.severity)">
                  {{ item.severity || 0 }}
                </span>
              </td>
              <td class="amount-cell" :data-label="$t('penaltiesAmount')">{{ formatCurrency(item.financialPenaltyAmount, item.currency) }}</td>
              <td :data-label="$t('penaltiesSuspensionUntil')">
                <span :class="{ 'text-danger fw-bold': isSuspended(item) }">
                  {{ formatDate(item.suspensionUntil) }}
                </span>
                <span v-if="isSuspended(item)" class="badge danger pulse-small ms-2">LIVE</span>
                <span v-if="item.status === 'RESOLVED'" class="badge success ms-2">RESOLVED</span>
              </td>
              <td :data-label="$t('penaltiesCreatedAt')">{{ formatDate(item.createdAt) }}</td>
              <td class="actions-td" :data-label="$t('packageAddressListActions')">
                <button 
                  v-if="item.status === 'ACTIVE'" 
                  class="btn-text danger-text" 
                  @click="liftPenalty(item)"
                  :disabled="isProcessing"
                >
                  <span class="material-symbols-outlined">{{ isSuspended(item) ? 'restart_alt' : 'check_circle' }}</span>
                  {{ isSuspended(item) ? $t('penaltiesActionLift') : $t('actionMarkResolved') || 'Résoudre' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <!-- Manual Penalty Modal - Premium Style -->
    <div v-if="showManualModal" class="modal-overlay" @click.self="showManualModal = false">
      <div class="modal-premium glassmorphism animated-scale">
        <div class="modal-header">
          <div class="header-content">
            <span class="material-symbols-outlined header-icon">warning</span>
            <div class="header-titles">
              <h2>{{ $t('penaltiesModalTitle') }}</h2>
              <p>{{ $t('penaltiesModalSubtitle') }}</p>
            </div>
          </div>
          <button class="icon-close-btn" @click="showManualModal = false">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>

        <div class="modal-body-scroll">
          <div class="modal-body-content">
            <!-- Group 1: Courier Selection -->
            <div class="form-section mb-4">
              <label class="premium-label">{{ $t('penaltiesModalCourier') }}</label>
              <div class="search-shell">
                <div class="search-container">
                  <input 
                    type="text" 
                    v-model="courierSearchQuery" 
                    :placeholder="$t('penaltiesModalCourierSearch')"
                    @input="debouncedSearchCouriers"
                    class="premium-input"
                  />
                  <div v-if="isSearching" class="search-spinner"></div>
                </div>
                <div v-if="courierResults.length" class="results-dropdown glassmorphism">
                  <div 
                    v-for="c in courierResults" 
                    :key="c.id" 
                    class="result-item" 
                    :class="{ active: selectedCourier?.id === c.id }"
                    @click="selectCourier(c)"
                  >
                    <div class="result-avatar">
                      <span class="material-symbols-outlined">person</span>
                    </div>
                    <div class="result-text">
                      <strong class="result-name">{{ c.firstName }} {{ c.lastName }}</strong>
                      <span class="result-email">{{ c.emailAddress }}</span>
                    </div>
                  </div>
                </div>
                <div v-if="selectedCourier" class="selection-badge glassmorphism mt-2">
                  <span class="material-symbols-outlined">check_circle</span>
                  <div class="selection-info">
                    <strong>{{ selectedCourier.firstName }} {{ selectedCourier.lastName }}</strong>
                    <small>{{ selectedCourier.emailAddress }}</small>
                  </div>
                </div>
              </div>
            </div>

            <div class="form-grid mb-4">
              <!-- Group 2: Duration -->
              <div class="form-group">
                <label class="premium-label">{{ $t('penaltiesModalDuration') }}</label>
                <div class="input-with-icon">
                  <span class="material-symbols-outlined icon-label">schedule</span>
                  <input type="number" v-model="manualDuration" min="0" max="168" class="premium-input has-icon" />
                </div>
              </div>
              <!-- Group 3: Amount -->
              <div class="form-group">
                <label class="premium-label">{{ $t('penaltiesModalAmount') }}</label>
                <div class="input-with-icon">
                  <span class="material-symbols-outlined icon-label">euro</span>
                  <input type="number" v-model="manualAmount" min="0" step="0.5" class="premium-input has-icon" />
                </div>
              </div>
            </div>

            <!-- Group 4: Reason -->
            <div class="form-group mb-4">
              <label class="premium-label">{{ $t('penaltiesModalReason') }}</label>
              <textarea 
                v-model="manualReason" 
                rows="4" 
                class="premium-textarea" 
                :placeholder="$t('penaltiesModalReasonPlaceholder')"
              ></textarea>
            </div>
          </div>
        </div>

        <div class="modal-footer">
          <button class="qd-btn-secondary" @click="showManualModal = false">{{ $t('actionBack') }}</button>
          <button 
            class="qd-btn-primary" 
            :disabled="!isManualFormValid || isProcessing"
            @click="imposeManualPenalty"
            style="flex: 1;"
          >
            <span v-if="isProcessing" class="spinner-small"></span>
            <span v-else class="material-symbols-outlined">gavel</span>
            {{ $t('penaltiesModalActionImpose') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import http from '@/config/httpInterceptor';
import { getAccessToken } from '../config/auth';
import PremiumDashboardCard from '@/components/PremiumDashboardCard.vue';

export default {
  name: 'CourierPenaltiesPage',
  components: {
    PremiumDashboardCard,
  },
  data() {
    return {
      isLoading: false,
      loadError: false,
      isProcessing: false,
      penalties: [],
      statusFilter: 'ALL',
      showManualModal: false,
      isSearching: false,
      courierSearchQuery: '',
      courierResults: [],
      selectedCourier: null,
      manualDuration: 24,
      manualAmount: 0,
      manualReason: '',
      searchTimeout: null,
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
    isManualFormValid() {
      const hasPenalty = this.manualDuration > 0 || this.manualAmount > 0;
      return this.selectedCourier && hasPenalty && this.manualReason.trim().length > 5;
    },
    kpis() {
      const active = this.penalties.filter((item) => item.status === 'ACTIVE');
      const suspended = active.filter(this.isSuspended);
      const amount = active.reduce((sum, item) => sum + Number(item.financialPenaltyAmount || 0), 0);
      return [
        { label: this.$t('penaltiesKpiTotal'), value: this.formatInteger(this.penalties.length), tone: 'indigo' },
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
    debouncedSearchCouriers() {
      if (this.searchTimeout) clearTimeout(this.searchTimeout);
      if (this.courierSearchQuery.length < 3) {
        this.courierResults = [];
        return;
      }
      this.searchTimeout = setTimeout(this.searchCouriers, 500);
    },
    async searchCouriers() {
      this.isSearching = true;
      try {
        const response = await http.get(`${this.$i18n.t('userRootURL')}${this.$i18n.t('searchCouriers')}?query=${encodeURIComponent(this.courierSearchQuery)}&limit=10`);
        this.courierResults = Array.isArray(response.data) ? response.data : [];
      } catch (error) {
        console.error('Courier search failed', error);
      } finally {
        this.isSearching = false;
      }
    },
    selectCourier(courier) {
      this.selectedCourier = courier;
      this.courierResults = []; // Close dropdown immediately
      this.courierSearchQuery = ''; // Clear for next search or just hide? 
      // Actually usually we keep it or clear it. Let's keep it clear to show the selection badge instead.
    },
    async liftPenalty(penalty) {
      if (!confirm(this.$t('penaltiesActionConfirmLift'))) return;
      
      this.isProcessing = true;
      try {
        await http.post(`${this.$i18n.t('rootURL')}admin/courier-penalties/${penalty.id}/lift?reason=Manual admin action`, null, {
          headers: { Authorization: `Bearer ${getAccessToken()}` },
        });
        alert(this.$t('penaltiesSuccessLifted'));
        await this.fetchPenalties();
      } catch (error) {
        alert(this.$t('requestErrorGeneric'));
      } finally {
        this.isProcessing = false;
      }
    },
    async imposeManualPenalty() {
      this.isProcessing = true;
      try {
        const params = new URLSearchParams();
        params.append('courierId', this.selectedCourier.id);
        params.append('suspensionHours', this.manualDuration);
        params.append('reason', this.manualReason);
        if (this.manualAmount > 0) {
          params.append('amount', this.manualAmount);
        }

        await http.post(`${this.$i18n.t('rootURL')}admin/courier-penalties/impose?${params.toString()}`, null, {
          headers: { Authorization: `Bearer ${getAccessToken()}` },
        });
        
        alert(this.$t('penaltiesSuccessImposed'));
        this.showManualModal = false;
        this.resetManualForm();
        await this.fetchPenalties();
      } catch (error) {
        alert(this.$t('requestErrorGeneric'));
      } finally {
        this.isProcessing = false;
      }
    },
    resetManualForm() {
      this.selectedCourier = null;
      this.courierSearchQuery = '';
      this.courierResults = [];
      this.manualDuration = 24;
      this.manualAmount = 0;
      this.manualReason = '';
    },
    isSuspended(item) {
      return item?.status === 'ACTIVE' && item?.suspensionUntil && new Date(item.suspensionUntil).getTime() > Date.now();
    },
    getSeverityClass(severity) {
      if (severity >= 4) return 'danger';
      if (severity >= 2) return 'warning';
      return 'info';
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
  padding-bottom: 40px;
}

.premium-kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.kpi-content {
  display: flex;
  flex-direction: column;
}

.kpi-label {
  font-size: 0.81rem;
  font-weight: 700;
  color: var(--qd-muted);
  text-transform: uppercase;
  margin-bottom: 8px;
}

.kpi-value {
  font-size: 2rem;
  font-weight: 900;
  color: var(--qd-text);
  letter-spacing: -0.02em;
}

.workspace-panel {
  padding: 0;
  overflow: hidden;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid var(--qd-border);
  background: var(--qd-bg);
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.filter-group span {
  font-size: 0.9rem;
  font-weight: 800;
  color: var(--qd-muted);
}

.filter-group select {
  height: 40px;
  padding: 0 12px;
  border-radius: 12px;
  border: 1px solid var(--qd-border-strong);
  background: #fff;
  font-weight: 600;
}

.actions-group {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  width: 100%;
}

.actions-group .qd-btn-primary,
.actions-group .qd-btn-secondary {
  width: 100%;
  border-radius: 999px !important;
}

/* qd-btn-primary and qd-btn-secondary are now handled by design-system.css */

.qd-spin {
  animation: qd-spin 1s linear infinite;
}

.input-with-icon {
  position: relative;
  display: flex;
  align-items: center;
}

.input-with-icon .material-symbols-outlined {
  position: absolute;
  left: 12px;
  font-size: 20px;
  color: var(--qd-muted);
}

.input-with-icon .premium-input {
  padding-left: 40px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.table-state {
  padding: 80px;
  text-align: center;
  color: var(--qd-muted);
}

.courier-cell strong {
  display: block;
  font-size: 0.95rem;
  margin-bottom: 2px;
}

.courier-cell small {
  color: var(--qd-muted);
}

.type-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: flex-start;
}

.ref-code {
  font-size: 0.75rem;
  background: var(--qd-bg);
  padding: 2px 6px;
  border-radius: 4px;
}

.amount-cell {
  font-weight: 800;
  color: var(--qd-danger);
}

.text-danger {
  color: var(--qd-danger);
}

.fw-bold {
  font-weight: 800;
}

.premium-spinner {
  width: 32px;
  height: 32px;
  border: 4px solid var(--qd-primary-soft);
  border-top-color: var(--qd-primary);
  border-radius: 50%;
  animation: qd-spin 1s linear infinite;
  margin: 0 auto 16px;
}

@keyframes qd-spin { to { transform: rotate(360deg); } }

/* Modal Overrides for Premium House Style */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.4);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
  padding: 20px;
}

.modal-premium {
  width: 100%;
  max-width: 600px;
  max-height: 90vh;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 24px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  border: 1px solid rgba(255, 255, 255, 0.5);
  overflow: hidden;
}

.modal-header {
  padding: 24px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  border-bottom: 1px solid #f1f5f9;
  background: #ffffff;
}

.header-content {
  display: flex;
  gap: 16px;
  align-items: center;
}

.header-icon {
  font-size: 1.8rem;
  color: #4f46e5;
  background: #f1f5f9;
  padding: 10px;
  border-radius: 12px;
}

.header-titles h2 {
  margin: 0;
  font-size: 1.15rem;
  font-weight: 800;
  color: #0f172a;
}

.header-titles p {
  margin: 2px 0 0;
  font-size: 0.85rem;
  color: #64748b;
}

.icon-close-btn {
  background: #f1f5f9;
  border: none;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #64748b;
  transition: all 0.2s;
}

.icon-close-btn:hover {
  background: #e2e8f0;
  color: #0f172a;
  transform: rotate(90deg);
}

.modal-body-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  background: #f8fafc;
}

.modal-footer {
  padding: 20px 24px;
  background: #ffffff;
  border-top: 1px solid #f1f5f9;
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

/* qd-btn-* handled by design-system.css */

/* Premium Form Elements */
.premium-label {
  display: block;
  font-size: 0.85rem;
  font-weight: 800;
  color: #475569;
  text-transform: uppercase;
  letter-spacing: 0.02em;
  margin-bottom: 8px;
}

.premium-input, .premium-textarea {
  width: 100%;
  padding: 12px 16px;
  border-radius: 12px;
  border: 1.5px solid #e2e8f0;
  background: #ffffff;
  font-size: 0.95rem;
  font-weight: 600;
  transition: all 0.2s ease;
  box-sizing: border-box;
}

.premium-input.has-icon {
  padding-left: 44px;
}

.premium-input:focus, .premium-textarea:focus {
  border-color: #6366f1;
  background: #fff;
  outline: none;
  box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.1);
}

.input-with-icon {
  position: relative;
  display: flex;
  align-items: center;
}

.icon-label {
  position: absolute;
  left: 14px;
  color: #94a3b8;
  font-size: 1.25rem;
  pointer-events: none;
}

.mb-4 { margin-bottom: 1.5rem; }
.mt-2 { margin-top: 0.5rem; }
.ms-2 { margin-left: 0.5rem; }

.selection-badge {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border-radius: 12px;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  color: #166534;
}

.selection-info {
  display: flex;
  flex-direction: column;
}

.selection-info strong { font-size: 0.95rem; }
.selection-info small { font-size: 0.8rem; opacity: 0.8; }

.search-shell {
  position: relative;
}

.results-dropdown {
  position: absolute;
  top: 52px;
  left: 0;
  right: 0;
  background: #ffffff !important;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.15);
  z-index: 100;
  max-height: 240px;
  overflow-y: auto;
  padding: 6px;
}

.result-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.result-item:hover {
  background: #f1f5f9;
}

.result-avatar {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: #eef2ff;
  color: #4f46e5;
  display: flex;
  align-items: center;
  justify-content: center;
}

.result-text {
  display: flex;
  flex-direction: column;
}

.result-name {
  font-size: 0.95rem;
  color: #1e293b;
}

.result-email {
  font-size: 0.8rem;
  color: #64748b;
  font-weight: 500;
}

.search-spinner {
  position: absolute;
  right: 14px;
  top: 14px;
  width: 20px;
  height: 20px;
  border: 2.5px solid #f1f5f9;
  border-top-color: #6366f1;
  border-radius: 50%;
  animation: qd-spin 0.6s linear infinite;
}

/* Table Button Stylings */
.btn-text {
  background: none;
  border: none;
  padding: 8px 12px;
  border-radius: 8px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
  font-size: 0.85rem;
  transition: background 0.2s;
}

.btn-text:hover:not(:disabled) {
  background: rgba(0, 0, 0, 0.05);
}

.btn-text.danger-text {
  color: #ef4444;
}

.btn-text.danger-text:hover:not(:disabled) {
  background: #fef2f2;
}

@media (max-width: 768px) {
  .courier-penalties-page {
    padding: 12px;
    gap: 16px;
    padding-bottom: 24px;
  }

  .courier-penalties-page .qd-page-header-actions,
  .courier-penalties-page .secondary-action {
    width: 100%;
  }

  .premium-kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
    margin-bottom: 0;
  }

  .kpi-content {
    min-height: 76px;
    justify-content: space-between;
  }

  .kpi-label {
    font-size: 0.68rem;
    line-height: 1.2;
    margin-bottom: 6px;
  }

  .kpi-value {
    font-size: 1.35rem;
    line-height: 1.05;
  }

  .workspace-panel {
    border-radius: 14px !important;
  }

  .toolbar {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
    padding: 14px;
  }

  .filter-group {
    align-items: flex-start;
    flex-direction: column;
    gap: 6px;
  }

  .filter-group select {
    width: 100%;
    height: 42px;
  }

  .actions-group {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
  }

  .actions-group .qd-btn-primary,
  .actions-group .qd-btn-secondary {
    width: 100%;
    min-height: 44px;
    height: auto !important;
    padding: 10px 12px !important;
    border-radius: 999px !important;
    white-space: normal;
    line-height: 1.2;
  }

  .table-state {
    padding: 36px 16px;
  }

  .premium-table-shell {
    border: none;
    border-radius: 0;
    background: transparent;
    overflow: visible;
  }

  .premium-table {
    display: block;
  }

  .premium-table thead {
    display: none;
  }

  .premium-table tbody {
    display: grid;
    gap: 12px;
    padding: 12px;
  }

  .premium-table tr {
    display: grid;
    gap: 0;
    padding: 12px;
    border: 1px solid var(--qd-border);
    border-radius: 12px;
    background: var(--qd-surface-strong);
    box-shadow: var(--qd-shadow-soft);
  }

  .premium-table td {
    display: grid;
    grid-template-columns: minmax(96px, 38%) minmax(0, 1fr);
    gap: 12px;
    align-items: start;
    padding: 9px 0 !important;
    border-bottom: 1px solid var(--qd-border) !important;
    font-size: 0.84rem;
    background: transparent !important;
  }

  .premium-table td::before {
    content: attr(data-label);
    color: var(--qd-muted);
    font-size: 0.68rem;
    font-weight: 800;
    line-height: 1.25;
    text-transform: uppercase;
    letter-spacing: 0.02em;
  }

  .premium-table td:last-child {
    border-bottom: none !important;
    padding-bottom: 0 !important;
  }

  .courier-cell,
  .type-cell {
    min-width: 0;
  }

  .courier-cell strong,
  .courier-cell small,
  .result-name,
  .result-email,
  .selection-info strong,
  .selection-info small {
    overflow-wrap: anywhere;
  }

  .type-cell {
    align-items: flex-end;
  }

  .badge {
    white-space: normal;
    text-align: right;
    justify-content: center;
  }

  .ms-2 {
    margin-left: 0;
    margin-top: 6px;
  }

  .actions-td .btn-text {
    width: 100%;
    justify-content: center;
    padding: 10px 12px;
    border: 1px solid #fecaca;
    background: #fef2f2;
  }

  .modal-overlay {
    align-items: flex-end;
    padding: 0;
  }

  .modal-premium {
    max-width: none;
    max-height: 92vh;
    border-radius: 18px 18px 0 0;
    align-self: flex-end;
  }

  .modal-header,
  .modal-body-scroll,
  .modal-footer {
    padding: 16px;
  }

  .modal-header {
    align-items: flex-start;
    gap: 12px;
  }

  .header-content {
    gap: 10px;
    min-width: 0;
  }

  .header-icon {
    display: none;
  }

  .header-titles h2 {
    font-size: 1rem;
  }

  .header-titles p {
    font-size: 0.78rem;
    line-height: 1.35;
  }

  .form-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .premium-input,
  .premium-textarea {
    font-size: 0.9rem;
  }

  .results-dropdown {
    max-height: 210px;
  }

  .selection-badge,
  .result-item {
    align-items: flex-start;
  }

  .modal-footer {
    display: grid;
    grid-template-columns: 1fr;
  }

  .modal-footer .qd-btn-primary,
  .modal-footer .qd-btn-secondary {
    width: 100%;
    min-height: 44px;
    height: auto !important;
    padding: 10px 14px !important;
    border-radius: 8px !important;
  }
}

@media (max-width: 480px) {
  .premium-table td {
    grid-template-columns: 1fr;
    gap: 4px;
  }

  .premium-table td::before {
    text-align: left;
  }

  .type-cell,
  .badge {
    align-items: flex-start;
    justify-content: flex-start;
    text-align: left;
  }
}
</style>
