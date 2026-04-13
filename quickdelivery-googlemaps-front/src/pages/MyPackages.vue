<template>
  <div class="my-packages-page qd-page">
    <template v-if="isLoadingPage">
      <header class="qd-page-header">
        <div class="header-main">
          <span class="page-chip">{{ $t('menuMyPackages') }}</span>
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
      <header class="qd-page-header">
        <div class="header-main">
          <span class="page-chip">{{ $t('menuMyPackages') }}</span>
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
      <header class="qd-page-header">
        <div class="header-main">
          <span class="page-chip pulse-chip">{{ $t('menuMyPackages') }}</span>
          <h1>{{ $t('myPackagesTitle') }}</h1>
          <p>{{ $t('myPackagesSubtitle') }}</p>
        </div>
        <div class="qd-page-header-actions">
          <div class="hero-account-glass">
            <div class="account-avatar">
              {{ connectedUserInitial }}
            </div>
            <div class="account-info">
              <strong>{{ connectedUserName }}</strong>
              <span class="role-badge">{{ displayedPriceColumnLabel }}</span>
            </div>
            <router-link v-if="canCreatePackage" to="/createPackage" class="qd-btn-primary primary-action">
              {{ $t('myPackagesNewDelivery') }}
            </router-link>
          </div>
        </div>
      </header>

      <div class="stats-banner-premium">
        <PremiumDashboardCard variant="glass" tone="indigo" class="stat-meta-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><span class="material-symbols-outlined">inventory_2</span></div>
            <div class="stat-text">
              <span class="stat-value">{{ filteredPackages.length }}</span>
              <span class="stat-label">{{ $t('myPackagesStatTotal') }}</span>
            </div>
          </div>
        </PremiumDashboardCard>
        <PremiumDashboardCard variant="glass" tone="amber" class="stat-meta-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><span class="material-symbols-outlined">local_shipping</span></div>
            <div class="stat-text">
              <span class="stat-value">{{ countByStatus(['INDELIVERY', 'PICKEDUP', 'RESERVED']) }}</span>
              <span class="stat-label">{{ $t('myPackagesStatInTransit') }}</span>
            </div>
          </div>
        </PremiumDashboardCard>
        <PremiumDashboardCard variant="glass" tone="emerald" class="stat-meta-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><span class="material-symbols-outlined">verified</span></div>
            <div class="stat-text">
              <span class="stat-value">{{ countByStatus(['DELIVERED']) }}</span>
              <span class="stat-label">{{ $t('myPackagesStatDelivered') }}</span>
            </div>
          </div>
        </PremiumDashboardCard>
        <PremiumDashboardCard variant="glass" tone="slate" class="stat-meta-card">
          <div class="stat-content">
            <div class="stat-icon-wrap"><span class="material-symbols-outlined">hourglass_empty</span></div>
            <div class="stat-text">
              <span class="stat-value">{{ countByStatus(['NEW', 'PAYMENTPENDING']) }}</span>
              <span class="stat-label">{{ $t('myPackagesStatPending') }}</span>
            </div>
          </div>
        </PremiumDashboardCard>
      </div>

      <PremiumDashboardCard variant="flat" class="filters-panel" no-padding>
        <div class="filters-row-unified">
          <div class="search-field-premium">
            <span class="material-symbols-outlined">search</span>
            <input v-model.trim="searchTerm" type="text" :placeholder="$t('myPackagesSearchPlaceholder')">
          </div>
          
          <div class="filter-controls-right">
            <select v-model="selectedStatus" class="status-select-premium">
              <option value="ALL">{{ $t('myPackagesAllStatuses') }}</option>
              <option v-for="status in availableStatuses" :key="status" :value="status">
                {{ statusLabel(status) }}
              </option>
            </select>

            <div v-if="!isMobile" class="segmented-control">
              <div class="segmented-control-bg" :class="{ 'at-right': displayMode === 'table' }"></div>
              <button class="segment-btn" :class="{ active: displayMode === 'cards' }" @click="displayMode = 'cards'">
                <span class="material-symbols-outlined">grid_view</span>
                {{ $t('myPackagesCardsView') }}
              </button>
              <button class="segment-btn" :class="{ active: displayMode === 'table' }" @click="displayMode = 'table'">
                <span class="material-symbols-outlined">table_chart</span>
                {{ $t('myPackagesTableView') }}
              </button>
            </div>
          </div>
        </div>
      </PremiumDashboardCard>

      <div class="content-layout-single">
        <section class="packages-column-full">
          <div v-if="filteredPackages.length === 0" class="empty-state-card">
            <span class="material-symbols-outlined">inventory</span>
            <p>{{ $t('stateEmptyPackages') }}</p>
          </div>

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
              <button v-if="canTrackPackage(package_)" class="qd-btn-primary" @click="openTracking(package_)">
                {{ $t('myPackagesTrackAction') }}
              </button>
              <button class="qd-btn-secondary" @click="openDetails(package_)">
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
                    <div class="table-actions-unified">
                        <button v-if="canTrackPackage(package_)" class="qd-btn-primary table-btn" @click="openTracking(package_)">
                        {{ $t('myPackagesTrackAction') }}
                        </button>
                        <button class="qd-btn-secondary table-btn" @click="openDetails(package_)">
                        {{ $t('packagesArroundMArkerDetailActionsDetails') }}
                        </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
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

.primary-action {
  height: 44px !important;
  border-radius: 12px !important;
  font-size: 0.9rem !important;
}

/* Stats Banner Premium */
.stats-banner-premium {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}

.stat-meta-card {
  transition: transform 0.3s ease;
}

.stat-meta-card:hover {
  transform: translateY(-5px);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px;
}

.stat-icon-wrap {
  width: 52px;
  height: 52px;
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(4px);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #0f172a;
}

.stat-icon-wrap .material-symbols-outlined {
  font-size: 24px;
}

.stat-text {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 1.75rem;
  font-weight: 800;
  color: #0f172a;
  line-height: 1.1;
}

.stat-label {
  font-size: 0.875rem;
  color: #64748b;
  font-weight: 600;
}

/* Unified Filters & Toggle */
.filters-panel {
  padding: 24px;
}

.filters-row-unified {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
}

.search-field-premium {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  height: 54px;
  padding: 0 20px;
  background: #f1f5f9;
  border-radius: 18px;
  border: 2px solid transparent;
  transition: all 0.2s;
}

.search-field-premium:focus-within {
  background: #fff;
  border-color: #4f46e5;
  box-shadow: 0 0 0 4px rgba(79, 70, 229, 0.1);
}

.search-field-premium .material-symbols-outlined {
  color: #94a3b8;
}

.search-field-premium input {
  width: 100%;
  border: none;
  background: none;
  font-size: 1rem;
  font-weight: 500;
  color: #1e293b;
  outline: none;
}

.filter-controls-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.status-select-premium {
  height: 54px;
  padding: 0 20px;
  background: #f1f5f9;
  border: 2px solid transparent;
  border-radius: 18px;
  font-weight: 600;
  color: #475569;
  outline: none;
  cursor: pointer;
}

/* Custom Segmented Control */
.segmented-control {
  position: relative;
  height: 54px;
  display: flex;
  background: #f1f5f9;
  border-radius: 18px;
  padding: 4px;
  box-sizing: border-box;
}

.segmented-control-bg {
  position: absolute;
  top: 4px;
  left: 4px;
  width: calc(50% - 4px);
  height: calc(100% - 8px);
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.segmented-control-bg.at-right {
  transform: translateX(100%);
}

.segment-btn {
  position: relative;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: none;
  background: none;
  padding: 0 20px;
  font-weight: 700;
  font-size: 0.9rem;
  color: #64748b;
  cursor: pointer;
  z-index: 1;
  transition: color 0.3s;
}

.segment-btn.active {
  color: #0f172a;
}

.segment-btn .material-symbols-outlined {
  font-size: 20px;
}

/* Content Layout */
.content-layout-single {
  width: 100%;
}

.packages-column-full {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.empty-state-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px;
  background: #fff;
  border-radius: 24px;
  color: #94a3b8;
  gap: 16px;
  text-align: center;
}

.empty-state-card .material-symbols-outlined {
  font-size: 48px;
  opacity: 0.3;
}

.status-groups {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.status-group-card {
  overflow: hidden;
}

.group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding: 20px 24px;
  border: none;
  background: rgba(248, 250, 252, 0.5);
  cursor: pointer;
  transition: background 0.2s;
}

.group-header:hover {
  background: rgba(248, 250, 252, 1);
}

.group-header-main {
  display: flex;
  align-items: center;
  gap: 12px;
}

.group-title {
  font-size: 1.1rem;
  font-weight: 800;
  color: #0f172a;
}

.group-count {
  padding: 2px 10px;
  background: #eef2ff;
  color: #4f46e5;
  border-radius: 99px;
  font-size: 0.8rem;
  font-weight: 700;
}

.group-chevron {
  color: #94a3b8;
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.group-chevron.collapsed {
  transform: rotate(-90deg);
}

/* Package Grid */
.packages-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
  padding: 24px;
}

.package-card {
  position: relative;
  background: #fff;
  border: 1px solid #f1f5f9;
  border-radius: 24px;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.02);
}

.package-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.08);
  border-color: #e2e8f0;
}

.card-top h3 {
  margin: 0;
  font-size: 1.4rem;
  font-weight: 900;
  color: #0f172a;
  letter-spacing: -0.01em;
  padding-right: 80px;
}

.status-badge {
  position: absolute;
  top: 24px;
  right: 24px;
  padding: 6px 12px;
  border-radius: 99px;
  font-size: 0.75rem;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.status-delivered { background: #dcfce7; color: #166534; }
.status-indelivery, .status-pickedup, .status-reserved { background: #fef9c3; color: #854d0e; }
.status-new, .status-paymentpending { background: #f1f5f9; color: #475569; }

.address-block {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 16px 0;
  border-top: 1px solid #f1f5f9;
  border-bottom: 1px solid #f1f5f9;
}

.address-row {
  display: flex;
  gap: 12px;
}

.address-row small {
  display: block;
  font-size: 0.75rem;
  font-weight: 700;
  color: #94a3b8;
  margin-bottom: 2px;
}

.address-row strong {
  font-size: 0.9375rem;
  color: #334155;
  line-height: 1.5;
}

.pickup-icon { color: #10b981; }
.delivery-icon { color: #f43f5e; }

.card-meta {
  display: flex;
  justify-content: space-between;
}

.card-meta small {
  display: block;
  font-size: 0.75rem;
  color: #94a3b8;
  margin-bottom: 4px;
}

.card-meta strong {
  font-size: 1.1rem;
  font-weight: 800;
  color: #0f172a;
}

.card-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

/* .track-btn, .details-btn handled by design-system.css qd-btn classes */

/* Table View Unified */
.packages-table-shell {
  padding: 0;
}

.packages-table {
  width: 100%;
  border-collapse: collapse;
}

.packages-table th {
  padding: 20px 24px;
  text-align: left;
  font-size: 0.75rem;
  font-weight: 800;
  color: #64748b;
  text-transform: uppercase;
  background: rgba(248, 250, 252, 0.8);
  border-bottom: 1px solid #f1f5f9;
}

.packages-table td {
  padding: 20px 24px;
  border-bottom: 1px solid #f1f5f9;
  font-size: 0.9375rem;
  color: #334155;
}

.table-actions-unified {
    display: flex;
    gap: 8px;
}

.table-btn {
    min-width: 100px;
}

@media screen and (max-width: 1400px) {
  .packages-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media screen and (max-width: 1100px) {
  .stats-banner-premium {
    grid-template-columns: repeat(2, 1fr);
  }
  .filters-row-unified {
    flex-direction: column;
    align-items: stretch;
  }
  .filter-controls-right {
    justify-content: space-between;
  }
}

@media screen and (max-width: 768px) {
  .my-packages-page {
    padding: 12px;
    gap: 14px;
  }

  .hero-section {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-content h1 {
    font-size: 1.65rem;
    line-height: 1.1;
  }

  .hero-account-glass {
    width: 100%;
    padding: 12px 16px;
  }

  .stats-banner-premium {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  .filters-row-unified {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }

  .status-select-premium {
    width: 100%;
  }

  .stat-value {
    font-size: 1.25rem;
  }

  .stat-label {
    font-size: 0.68rem;
    line-height: 1.2;
  }
}

@media screen and (max-width: 767px) {
  .my-packages-page {
    padding: 12px;
  }
  .hero-section {
    flex-direction: column;
    align-items: flex-start;
  }
  .hero-content h1 {
    font-size: 1.55rem;
    line-height: 1.1;
  }
  .stats-banner-premium {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
  }
  .packages-grid {
    grid-template-columns: 1fr;
    padding: 10px;
  }
  .empty-state-card {
    padding: 24px 12px;
    gap: 10px;
    border-radius: 12px;
  }
  .empty-state-card .material-symbols-outlined {
    font-size: 32px;
  }
  .status-badge {
    position: relative;
    top: 0;
    right: 0;
    margin-top: 12px;
  }
}
</style>
