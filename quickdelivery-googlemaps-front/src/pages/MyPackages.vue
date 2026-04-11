<template>
  <div class="my-packages-page">
    <template v-if="isLoadingPage">
      <header class="hero-section">
        <div class="hero-content">
          <span class="hero-chip">{{ $t('menuMyPackages') }}</span>
          <h1>{{ $t('myPackagesTitle') }}</h1>
          <p>{{ $t('myPackagesSubtitle') }}</p>
        </div>
      </header>
      <div class="page-state loading-state">
        <div class="spinner"></div>
        <span>{{ $t('stateLoading') }}</span>
      </div>
    </template>

    <template v-else-if="loadError">
      <header class="hero-section">
        <div class="hero-content">
          <span class="hero-chip">{{ $t('menuMyPackages') }}</span>
          <h1>{{ $t('myPackagesTitle') }}</h1>
          <p>{{ $t('myPackagesSubtitle') }}</p>
        </div>
      </header>
      <div class="page-state error-state">
        <span class="material-symbols-outlined">inventory_2</span>
        <p>{{ $t('stateLoadError') }}</p>
      </div>
    </template>

    <template v-else>
      <header class="hero-section">
        <div class="hero-content">
          <span class="hero-chip pulse-chip">{{ $t('menuMyPackages') }}</span>
          <h1>{{ $t('myPackagesTitle') }}</h1>
          <p>{{ $t('myPackagesSubtitle') }}</p>
        </div>
        <div class="hero-account-glass">
          <div class="account-avatar">
            {{ connectedUserInitial }}
          </div>
          <div class="account-info">
            <strong>{{ connectedUserName }}</strong>
            <span class="role-badge">{{ displayedPriceColumnLabel }}</span>
          </div>
          <router-link v-if="canCreatePackage" to="/createPackage" class="primary-action">
            {{ $t('myPackagesNewDelivery') }}
          </router-link>
        </div>
      </header>

      <PremiumDashboardCard variant="flat" class="filters-panel" no-padding>
        <div class="filters-row">
          <div class="search-field">
            <span class="material-symbols-outlined">search</span>
            <input v-model.trim="searchTerm" type="text" :placeholder="$t('myPackagesSearchPlaceholder')">
          </div>
          <select v-model="selectedStatus" class="status-select">
            <option value="ALL">{{ $t('myPackagesAllStatuses') }}</option>
            <option v-for="status in availableStatuses" :key="status" :value="status">
              {{ statusLabel(status) }}
            </option>
          </select>
        </div>
      </PremiumDashboardCard>

      <div class="stats-grid">
        <PremiumDashboardCard variant="glass" tone="indigo" class="stat-premium-card">
          <div class="stat-inner">
            <span class="stat-value">{{ filteredPackages.length }}</span>
            <span class="stat-label">{{ $t('myPackagesStatTotal') }}</span>
          </div>
        </PremiumDashboardCard>
        <PremiumDashboardCard variant="glass" tone="amber" class="stat-premium-card">
          <div class="stat-inner">
            <span class="stat-value">{{ countByStatus(['INDELIVERY', 'PICKEDUP', 'RESERVED']) }}</span>
            <span class="stat-label">{{ $t('myPackagesStatInTransit') }}</span>
          </div>
        </PremiumDashboardCard>
        <PremiumDashboardCard variant="glass" tone="emerald" class="stat-premium-card">
          <div class="stat-inner">
            <span class="stat-value">{{ countByStatus(['DELIVERED']) }}</span>
            <span class="stat-label">{{ $t('myPackagesStatDelivered') }}</span>
          </div>
        </PremiumDashboardCard>
        <PremiumDashboardCard variant="glass" tone="slate" class="stat-premium-card">
          <div class="stat-inner">
            <span class="stat-value">{{ countByStatus(['NEW', 'PAYMENTPENDING']) }}</span>
            <span class="stat-label">{{ $t('myPackagesStatPending') }}</span>
          </div>
        </PremiumDashboardCard>
      </div>

      <div class="content-layout">
        <section class="packages-column">
          <div v-if="!isMobile && filteredPackages.length > 0" class="view-switch">
            <button class="view-btn" :class="{ active: displayMode === 'cards' }" @click="displayMode = 'cards'">
              {{ $t('myPackagesCardsView') }}
            </button>
            <button class="view-btn" :class="{ active: displayMode === 'table' }" @click="displayMode = 'table'">
              {{ $t('myPackagesTableView') }}
            </button>
          </div>

          <div v-if="filteredPackages.length === 0" class="page-state">{{ $t('stateEmptyPackages') }}</div>

          <div v-else-if="displayMode === 'cards'" class="status-groups">
            <PremiumDashboardCard
              v-for="group in groupedPackagesByStatus"
              :key="group.status"
              variant="glass"
              class="status-group-card"
              no-padding
            >
              <button class="group-header" @click="toggleGroup(group.status)">
                <div class="group-header-main">
                  <span class="group-title">{{ statusLabel(group.status) }}</span>
                  <span class="group-count">{{ group.packages.length }}</span>
                </div>
                <span class="material-symbols-outlined group-chevron" :class="{ collapsed: isGroupCollapsed(group.status) }">expand_more</span>
              </button>

              <div v-if="!isGroupCollapsed(group.status)" class="packages-grid">
                <article v-for="package_ in group.packages" :key="package_.id" class="package-card">
            <div class="card-top">
              <h3>{{ package_.reference || fallbackReference(package_) }}</h3>
              <span class="status-badge" :class="statusBadgeClass(package_.status)">{{ statusLabel(package_.status) }}</span>
            </div>

            <div class="address-block">
              <div class="address-row">
                <span class="material-symbols-outlined pickup-icon">trip_origin</span>
                <div>
                  <small>{{ $t('packageDeparture') }}</small>
                  <strong>{{ formatAddress(getAddressByType(package_, 'DEPARTURE')) }}</strong>
                </div>
              </div>
              <div class="address-row">
                <span class="material-symbols-outlined delivery-icon">location_on</span>
                <div>
                  <small>{{ $t('packageDestination') }}</small>
                  <strong>{{ formatAddress(getAddressByType(package_, 'ARRIVAL')) }}</strong>
                </div>
              </div>
            </div>

            <div class="card-meta">
              <div>
                <small>{{ displayedPriceColumnLabel }}</small>
                <strong>{{ formatDisplayedPrice(package_) }}</strong>
              </div>
              <div class="meta-right">
                <small>{{ $t('myPackagesCreatedAt') }}</small>
                <strong>{{ formatCreatedDate(package_) }}</strong>
              </div>
            </div>

            <div class="card-actions">
              <button v-if="canTrackPackage(package_)" class="track-btn" @click="openTracking(package_)">
                {{ $t('myPackagesTrackAction') }}
              </button>
              <button class="details-btn" @click="openDetails(package_)">
                {{ $t('packagesArroundMArkerDetailActionsDetails') }}
              </button>
            </div>
                </article>
              </div>
            </PremiumDashboardCard>
          </div>

          <PremiumDashboardCard v-else variant="glass" class="packages-table-shell" no-padding>
            <table class="packages-table">
              <thead>
                <tr>
                  <th>{{ $t('packageReference') }}</th>
                  <th>{{ $t('packageDeparture') }}</th>
                  <th>{{ $t('packageDestination') }}</th>
                  <th>{{ displayedPriceColumnLabel }}</th>
                  <th>{{ $t('myPackagesStatusColumn') }}</th>
                  <th>{{ $t('myPackagesCreatedAt') }}</th>
                  <th>{{ $t('packageAddressListActions') }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="package_ in filteredPackages" :key="package_.id">
                  <td>{{ package_.reference || fallbackReference(package_) }}</td>
                  <td>{{ formatAddress(getAddressByType(package_, 'DEPARTURE')) }}</td>
                  <td>{{ formatAddress(getAddressByType(package_, 'ARRIVAL')) }}</td>
                  <td>{{ formatDisplayedPrice(package_) }}</td>
                  <td>
                    <span class="status-badge" :class="statusBadgeClass(package_.status)">{{ statusLabel(package_.status) }}</span>
                  </td>
                  <td>{{ formatCreatedDate(package_) }}</td>
                  <td>
                    <button v-if="canTrackPackage(package_)" class="track-btn table-btn" @click="openTracking(package_)">
                      {{ $t('myPackagesTrackAction') }}
                    </button>
                    <button class="details-btn table-btn" @click="openDetails(package_)">
                      {{ $t('packagesArroundMArkerDetailActionsDetails') }}
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </PremiumDashboardCard>
        </section>

        <section class="insights-column">
          <PremiumDashboardCard :title="$t('menuMyPackages')" variant="glass" class="insight-card">
            <div class="todo-stack">
              <div class="todo-pill">
                <span class="material-symbols-outlined">inventory_2</span>
                <span>{{ filteredPackages.length }} {{ $t('myPackagesStatTotal') }}</span>
              </div>
              <div class="todo-pill tone-amber">
                <span class="material-symbols-outlined">local_shipping</span>
                <span>{{ countByStatus(['INDELIVERY', 'PICKEDUP', 'RESERVED']) }} {{ $t('myPackagesStatInTransit') }}</span>
              </div>
              <div class="todo-pill tone-emerald">
                <span class="material-symbols-outlined">verified</span>
                <span>{{ countByStatus(['DELIVERED']) }} {{ $t('myPackagesStatDelivered') }}</span>
              </div>
            </div>
          </PremiumDashboardCard>
        </section>
      </div>
    </template>
  </div>
</template>

<script>
import http from '@/config/httpInterceptor';
import { getCurrentUserRoles } from '@/config/auth';
import { formatDisplayedPackageAmount, resolveDisplayedPackagePriceLabel } from '@/config/packagePricing';
import { hydrateConnectedUser } from '@/config/session';
import PremiumDashboardCard from '../components/PremiumDashboardCard.vue';

export default {
  components: {
    PremiumDashboardCard,
  },
  data() {
    return {
      packagesByStatus: [],
      isLoadingPage: false,
      loadError: false,
      searchTerm: '',
      selectedStatus: 'ALL',
      displayMode: 'cards',
      collapsedStatuses: {},
      isMobile: window.innerWidth < 768,
      fetchRequestToken: 0,
    };
  },
  computed: {
    connectedUserId() {
      return this.$store.state.connectedUser?.id ?? null;
    },
    canCreatePackage() {
      const roles = getCurrentUserRoles();
      return roles.includes('ROLE_CLIENT') || roles.includes('ROLE_CLIENT_PRO') || roles.includes('ROLE_ADMIN');
    },
    isClientRole() {
      const roles = getCurrentUserRoles();
      return roles.includes('ROLE_CLIENT') || roles.includes('ROLE_CLIENT_PRO');
    },
    isAdminRole() {
      return getCurrentUserRoles().includes('ROLE_ADMIN');
    },
    displayedPriceColumnLabel() {
      return resolveDisplayedPackagePriceLabel(this.$i18n);
    },
    connectedUserName() {
      const firstName = this.$store.state.connectedUser?.firstName || '';
      const lastName = this.$store.state.connectedUser?.lastName || '';
      return `${firstName} ${lastName}`.trim() || this.$store.state.connectedUser?.email || 'QuickDelivery';
    },
    connectedUserInitial() {
      return this.connectedUserName.charAt(0).toUpperCase();
    },
    flattenedPackages() {
      return this.packagesByStatus.flatMap((entry) => entry.groupedPackagesList || []);
    },
    availableStatuses() {
      return [...new Set(this.flattenedPackages.map((pkg) => pkg.status).filter(Boolean))];
    },
    filteredPackages() {
      return this.flattenedPackages.filter((pkg) => {
        const statusMatch = this.selectedStatus === 'ALL' || pkg.status === this.selectedStatus;
        if (!statusMatch) {
          return false;
        }

        if (!this.searchTerm) {
          return true;
        }

        const haystack = [
          pkg.reference,
          this.formatAddress(this.getAddressByType(pkg, 'DEPARTURE')),
          this.formatAddress(this.getAddressByType(pkg, 'ARRIVAL')),
          pkg.status,
        ].filter(Boolean).join(' ').toLowerCase();

        return haystack.includes(this.searchTerm.toLowerCase());
      });
    },
    groupedPackagesByStatus() {
      const groups = new Map();
      this.filteredPackages.forEach((pkg) => {
        const status = pkg.status || 'UNKNOWN';
        if (!groups.has(status)) {
          groups.set(status, []);
        }
        groups.get(status).push(pkg);
      });
      return Array.from(groups.entries()).map(([status, packages]) => ({ status, packages }));
    },
  },
  mounted() {
    window.addEventListener('resize', this.handleResize);
    this.handleResize();
    this.initializePage();
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.handleResize);
  },
  watch: {
    connectedUserId(newValue, oldValue) {
      if (newValue && newValue !== oldValue) {
        this.fetchData();
      }
    },
    '$store.state.connectedUser.loaded'(isLoaded) {
      if (isLoaded && this.connectedUserId) {
        this.fetchData();
      }
    },
  },
  methods: {
    async initializePage() {
      try {
        if (!this.connectedUserId) {
          await hydrateConnectedUser();
        }
      } catch (error) {
        console.error('Unable to hydrate connected user for my packages.', error);
      }
      await this.fetchData();
    },
    handleResize() {
      this.isMobile = window.innerWidth < 768;
      if (this.isMobile) {
        this.displayMode = 'cards';
      }
    },
    async fetchData() {
      this.fetchRequestToken += 1;
      const requestToken = this.fetchRequestToken;
      this.isLoadingPage = true;
      this.loadError = false;
      this.packagesByStatus = [];
      try {
        if (!this.connectedUserId) {
          this.collapsedStatuses = {};
          return;
        }
        const roles = getCurrentUserRoles();
        const endpoint = roles.includes('ROLE_CLIENT') || roles.includes('ROLE_CLIENT_PRO')
          ? this.$i18n.t('getPackagesBySenderUrl')
          : this.$i18n.t('getPackagesByDeliveryPersonUrl');
        const response = await http.get(this.$i18n.t('rootURL') + endpoint + this.connectedUserId);
        if (requestToken !== this.fetchRequestToken) {
          return;
        }
        Object.entries(response.data).forEach(([status, packagesArray]) => {
          if (typeof status === 'string' && Array.isArray(packagesArray)) {
            this.packagesByStatus.push({ satuts_: status, groupedPackagesList: packagesArray });
          }
        });
        this.collapsedStatuses = Object.fromEntries(
          this.packagesByStatus.map((entry) => [entry.satuts_, true])
        );
      } catch (error) {
        this.loadError = true;
        console.error('Unable to load packages list.', error);
      } finally {
        if (requestToken === this.fetchRequestToken) {
          this.isLoadingPage = false;
        }
      }
    },
    isGroupCollapsed(status) {
      return this.collapsedStatuses[status] === true;
    },
    toggleGroup(status) {
      this.collapsedStatuses = {
        ...this.collapsedStatuses,
        [status]: !this.isGroupCollapsed(status),
      };
    },
    openDetails(package_) {
      this.$store.commit('updatePackage', package_);
      this.$router.push({
        path: '/package',
        query: {
          id: package_.reference,
          returnTo: this.$route.fullPath,
        },
      });
    },
    openTracking(package_) {
      this.$router.push({
        path: '/packageTracking',
        query: {
          packageReference: package_.reference,
          returnTo: this.$route.fullPath,
        },
      });
    },
    canTrackPackage(package_) {
      return (this.isClientRole || this.isAdminRole)
        && ['RESERVED', 'PICKEDUP', 'INDELIVERY'].includes(package_?.status)
        && Boolean(package_?.reference);
    },
    getAddressByType(package_, type) {
      return (package_.addresses || []).find((address) => address.type === type) || {};
    },
    formatAddress(address) {
      if (!address) {
        return '-';
      }
      return [address.line1, address.zipCode, address.town, address.country].filter(Boolean).join(', ') || address.addressAuto || '-';
    },
    formatPrice(price) {
      if (price === null || price === undefined || price === '') {
        return '-';
      }
      return `${price}${this.$t('currency')}`;
    },
    formatDisplayedPrice(package_) {
      return formatDisplayedPackageAmount(this.$i18n, package_);
    },
    formatCreatedDate(package_) {
      const value = package_.creationDate || package_.createdDate || package_.createdAt || package_.dateTime;
      if (!value) {
        return '-';
      }
      const date = new Date(value);
      if (Number.isNaN(date.getTime())) {
        return '-';
      }
      return date.toLocaleDateString();
    },
    fallbackReference(package_) {
      return `PKG${package_.id}`;
    },
    statusLabel(status) {
      return status ? this.$t(status) : '-';
    },
    statusBadgeClass(status) {
      return `status-${(status || 'UNKNOWN').toLowerCase()}`;
    },
    countByStatus(statuses) {
      return this.filteredPackages.filter((pkg) => statuses.includes(pkg.status)).length;
    },
  },
};
</script>

<style scoped>
.my-packages-page {
  min-height: 100%;
  padding: 32px;
  background:
    radial-gradient(circle at top right, rgba(79, 70, 229, 0.08), transparent 400px),
    linear-gradient(180deg, #f8fafc 0%, #f1f5f9 100%);
  display: flex;
  flex-direction: column;
  gap: 28px;
}

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

.hero-content h1 {
  margin: 0;
  font-size: 2.75rem;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: -0.02em;
}

.hero-content p {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 1.05rem;
}

.hero-account-glass {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 20px;
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(8px);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.4);
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.03);
  flex-wrap: wrap;
}

.account-avatar {
  width: 44px;
  height: 44px;
  background: var(--qd-primary);
  color: #fff;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  font-size: 1.25rem;
}

.account-info strong {
  display: block;
  font-size: 1rem;
  color: #0f172a;
}

.role-badge {
  font-size: 0.75rem;
  color: #64748b;
  font-weight: 600;
}

.primary-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 164px;
  max-width: 100%;
  height: 44px;
  padding: 0 18px;
  text-decoration: none;
  font-weight: 700;
}

.filters-panel {
  padding: 20px;
}

.filters-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 14px;
}

.search-field {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 50px;
  padding: 0 16px;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  background: #ffffff;
}

.search-field span {
  color: #94a3b8;
}

.search-field input,
.status-select {
  width: 100%;
  border: none;
  outline: none;
  background: transparent;
  color: #1f2937;
  font-size: 0.95rem;
}

.status-select {
  height: 50px;
  padding: 0 16px;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  background: #ffffff;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.stat-inner {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 2.1rem;
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

.view-switch {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-bottom: 18px;
}

.view-btn {
  min-width: 84px;
  height: 38px;
}

.content-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 28px;
  align-items: start;
}

.packages-column,
.insights-column {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.page-state {
  padding: 14px;
  border-radius: 12px;
  background: #eef3f9;
  color: #334155;
  text-align: center;
}

.loading-state,
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100px 0;
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

.packages-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.status-groups {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.status-group-card {
  overflow: hidden;
}

.group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding: 14px 18px;
  border: 0;
  border-bottom: 1px solid rgba(15, 23, 42, 0.04);
  background: transparent;
  color: #0f172a;
}

.group-header-main {
  display: flex;
  align-items: center;
  gap: 10px;
}

.group-title {
  font-size: 1rem;
  font-weight: 700;
}

.group-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  height: 24px;
  padding: 0 8px;
  border-radius: 999px;
  background: #eef2ff;
  color: #334155;
  font-size: 0.8rem;
  font-weight: 700;
}

.group-chevron {
  transition: transform 0.2s ease;
}

.group-chevron.collapsed {
  transform: rotate(-90deg);
}

.package-card {
  position: relative;
  padding: 20px;
  border: 1px solid rgba(15, 23, 42, 0.05);
  border-radius: 20px;
  background: #ffffff;
  box-shadow: 0 14px 28px rgba(15, 23, 42, 0.05);
  width: 100%;
  box-sizing: border-box;
  overflow: hidden;
}

.card-top {
  display: block;
  min-width: 0;
  margin-bottom: 18px;
  padding-right: 108px;
}

.card-top h3 {
  margin: 0;
  font-size: clamp(1.2rem, 2vw, 1.7rem);
  line-height: 1.1;
  color: #0f172a;
  word-break: break-word;
}

.status-badge {
  position: absolute;
  top: 20px;
  right: 20px;
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 0.85rem;
  font-weight: 600;
  white-space: nowrap;
}

.status-delivered {
  color: #166534;
  background: #dcfce7;
}

.status-indelivery,
.status-pickedup,
.status-reserved {
  color: #b45309;
  background: #fef3c7;
}

.status-new,
.status-paymentpending {
  color: #475569;
  background: #e2e8f0;
}

.address-block {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding-bottom: 18px;
  margin-bottom: 18px;
  border-bottom: 1px solid #eef2f7;
}

.address-row {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.address-row small {
  display: block;
  margin-bottom: 4px;
  color: #94a3b8;
}

.address-row strong {
  color: #334155;
  line-height: 1.45;
}

.pickup-icon {
  color: #65a30d;
  font-size: 18px;
}

.delivery-icon {
  color: #ef4444;
  font-size: 18px;
}

.card-meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 18px;
}

.card-meta small {
  display: block;
  margin-bottom: 6px;
  color: #94a3b8;
}

.card-meta strong {
  color: #0f172a;
  font-size: 1.05rem;
}

.meta-right {
  text-align: right;
}

.card-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-start;
  gap: 10px;
}

.track-btn,
.details-btn {
  min-width: 132px;
  height: 40px;
}

.packages-table-shell {
  overflow-x: auto;
}

.packages-table {
  width: 100%;
  border-collapse: collapse;
}

.packages-table th,
.packages-table td {
  padding: 16px;
  border-bottom: 1px solid #eef2f7;
  text-align: left;
  vertical-align: middle;
}

.packages-table th {
  color: #64748b;
  font-size: 0.78rem;
  font-weight: 700;
  text-transform: uppercase;
  background: rgba(248, 250, 252, 0.85);
}

.table-btn {
  min-width: 108px;
  height: 36px;
  margin-right: 8px;
}

.table-btn:last-child {
  margin-right: 0;
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
  background: rgba(79, 70, 229, 0.08);
  border-radius: 12px;
  color: var(--qd-primary);
  font-size: 0.875rem;
  font-weight: 600;
}

.todo-pill.tone-amber {
  background: rgba(245, 158, 11, 0.08);
  color: #b45309;
}

.todo-pill.tone-emerald {
  background: rgba(16, 185, 129, 0.08);
  color: #047857;
}

@media screen and (max-width: 1100px) {
  .content-layout,
  .filters-row {
    grid-template-columns: 1fr;
  }

  .stats-grid,
  .packages-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media screen and (max-width: 767px) {
  .my-packages-page {
    padding: 20px;
  }

  .hero-section {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-content h1 {
    font-size: 2rem;
  }

  .hero-account-glass {
    width: 100%;
  }

  .primary-action {
    width: 100%;
    min-width: 0;
  }

  .stats-grid,
  .packages-grid {
    grid-template-columns: 1fr;
  }

  .view-switch {
    justify-content: stretch;
  }

  .view-btn {
    flex: 1;
  }

  .group-header {
    padding: 12px 14px;
  }

  .card-top {
    padding-right: 92px;
  }

  .card-meta {
    grid-template-columns: 1fr;
  }

  .meta-right {
    text-align: left;
  }

  .status-badge {
    top: 16px;
    right: 16px;
  }

  .packages-table-shell {
    overflow: auto;
  }
}
</style>
