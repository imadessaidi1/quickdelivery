<template>
  <div class="my-packages-page">
    <div class="page-header">
      <div>
        <h1>{{ $t('myPackagesTitle') }}</h1>
        <p>{{ $t('myPackagesSubtitle') }}</p>
      </div>
      <router-link v-if="canCreatePackage" to="/createPackage" class="primary-action">
        {{ $t('myPackagesNewDelivery') }}
      </router-link>
    </div>

    <div class="filters-panel">
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
    </div>

    <div class="stats-grid" v-if="!isLoadingPage && !loadError">
      <div class="stat-card">
        <strong>{{ filteredPackages.length }}</strong>
        <span>{{ $t('myPackagesStatTotal') }}</span>
      </div>
      <div class="stat-card accent-warn">
        <strong>{{ countByStatus(['INDELIVERY', 'PICKEDUP', 'RESERVED']) }}</strong>
        <span>{{ $t('myPackagesStatInTransit') }}</span>
      </div>
      <div class="stat-card accent-success">
        <strong>{{ countByStatus(['DELIVERED']) }}</strong>
        <span>{{ $t('myPackagesStatDelivered') }}</span>
      </div>
      <div class="stat-card accent-neutral">
        <strong>{{ countByStatus(['NEW', 'PAYMENTPENDING']) }}</strong>
        <span>{{ $t('myPackagesStatPending') }}</span>
      </div>
    </div>

    <div v-if="!isMobile && !isLoadingPage && !loadError && filteredPackages.length > 0" class="view-switch">
      <button class="view-btn" :class="{ active: displayMode === 'cards' }" @click="displayMode = 'cards'">
        {{ $t('myPackagesCardsView') }}
      </button>
      <button class="view-btn" :class="{ active: displayMode === 'table' }" @click="displayMode = 'table'">
        {{ $t('myPackagesTableView') }}
      </button>
    </div>

    <div v-if="isLoadingPage" class="page-state">{{ $t('stateLoading') }}</div>
    <div v-else-if="loadError" class="page-state error">{{ $t('stateLoadError') }}</div>
    <div v-else-if="filteredPackages.length === 0" class="page-state">{{ $t('stateEmptyPackages') }}</div>

    <div v-else-if="displayMode === 'cards'" class="status-groups">
      <section v-for="group in groupedPackagesByStatus" :key="group.status" class="status-group">
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
                <small>{{ $t('packagePrice') }}</small>
                <strong>{{ formatPrice(package_.deliveryPrice) }}</strong>
              </div>
              <div class="meta-right">
                <small>{{ $t('myPackagesCreatedAt') }}</small>
                <strong>{{ formatCreatedDate(package_) }}</strong>
              </div>
            </div>

            <div class="card-actions">
              <button class="details-btn" @click="openDetails(package_)">
                {{ $t('packagesArroundMArkerDetailActionsDetails') }}
              </button>
            </div>
          </article>
        </div>
      </section>
    </div>

    <div v-else class="packages-table-shell">
      <table class="packages-table">
        <thead>
          <tr>
            <th>{{ $t('packageReference') }}</th>
            <th>{{ $t('packageDeparture') }}</th>
            <th>{{ $t('packageDestination') }}</th>
            <th>{{ $t('packagePrice') }}</th>
            <th>{{ $t('myPackagesStatusColumn') }}</th>
            <th>{{ $t('myPackagesCreatedAt') }}</th>
            <th>{{ $t('packagesArroundMArkerDetailActionsDetails') }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="package_ in filteredPackages" :key="package_.id">
            <td>{{ package_.reference || fallbackReference(package_) }}</td>
            <td>{{ formatAddress(getAddressByType(package_, 'DEPARTURE')) }}</td>
            <td>{{ formatAddress(getAddressByType(package_, 'ARRIVAL')) }}</td>
            <td>{{ formatPrice(package_.deliveryPrice) }}</td>
            <td>
              <span class="status-badge" :class="statusBadgeClass(package_.status)">{{ statusLabel(package_.status) }}</span>
            </td>
            <td>{{ formatCreatedDate(package_) }}</td>
            <td>
              <button class="details-btn table-btn" @click="openDetails(package_)">
                {{ $t('packagesArroundMArkerDetailActionsDetails') }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script>
import http from '@/config/httpInterceptor';
import { getCurrentUserRoles } from '@/config/auth';

export default {
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
    this.fetchData();
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.handleResize);
  },
  methods: {
    handleResize() {
      this.isMobile = window.innerWidth < 768;
      if (this.isMobile) {
        this.displayMode = 'cards';
      }
    },
    async fetchData() {
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
        this.isLoadingPage = false;
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

<style>
.my-packages-page {
  min-height: 100%;
  padding: 28px;
  background: #f6f7f9;
  box-sizing: border-box;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 0;
  font-size: 3rem;
  line-height: 1;
  color: #0f172a;
}

.page-header p {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 1rem;
}

.primary-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 152px;
  max-width: 100%;
  height: 42px;
  padding: 0 18px;
  border-radius: 12px;
  background: #020617;
  color: #ffffff;
  text-decoration: none;
  font-weight: 600;
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.12);
  box-sizing: border-box;
}

.filters-panel {
  padding: 20px;
  border: 1px solid #e5e7eb;
  border-radius: 18px;
  background: #ffffff;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05);
  margin-bottom: 22px;
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
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 18px;
}

.stat-card {
  padding: 26px 20px;
  border: 1px solid #e5e7eb;
  border-radius: 18px;
  background: #ffffff;
  text-align: center;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.04);
}

.stat-card strong {
  display: block;
  margin-bottom: 8px;
  font-size: 2rem;
  color: #0f172a;
}

.stat-card span {
  color: #64748b;
}

.accent-warn strong {
  color: #d97706;
}

.accent-success strong {
  color: #16a34a;
}

.accent-neutral strong {
  color: #475569;
}

.view-switch {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-bottom: 18px;
}

.view-btn {
  min-width: 84px;
  height: 38px;
  border: 1px solid #dbe1ea;
  border-radius: 12px;
  background: #ffffff;
  color: #334155;
  font-weight: 600;
}

.view-btn.active {
  background: #020617;
  border-color: #020617;
  color: #ffffff;
}

.page-state {
  padding: 14px;
  border-radius: 12px;
  background: #eef3f9;
  color: #334155;
  text-align: center;
}

.page-state.error {
  background: #fef2f2;
  color: #b91c1c;
}

.packages-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
  width: 100%;
  min-width: 0;
}

.status-groups {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.status-group {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding: 14px 18px;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  background: #ffffff;
  color: #0f172a;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.04);
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
  border: 1px solid #e5e7eb;
  border-radius: 18px;
  background: #ffffff;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.06);
  width: 100%;
  min-width: 0;
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
  min-width: 0;
  overflow-wrap: anywhere;
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
  justify-content: flex-start;
}

.details-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 132px;
  height: 40px;
  padding: 0 16px;
  border: none;
  border-radius: 12px;
  background: #020617;
  color: #ffffff;
  font-weight: 600;
}

.packages-table-shell {
  overflow-x: auto;
  border: 1px solid #e5e7eb;
  border-radius: 18px;
  background: #ffffff;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.06);
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
  font-size: 0.9rem;
  font-weight: 700;
  background: #f8fafc;
}

.table-btn {
  min-width: 108px;
  height: 36px;
}

@media screen and (max-width: 1100px) {
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
    padding: 16px;
  }

  .page-header {
    flex-direction: column;
    align-items: stretch;
  }

  .page-header h1 {
    font-size: 2.2rem;
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
}
</style>
