<template>
  <div class="dashboard-page">
    <section class="hero-card">
      <div>
        <span class="hero-chip">{{ $t('dashboardWelcomeBack') }}</span>
        <h1>{{ dashboardTitle }}</h1>
        <p>{{ dashboardSubtitle }}</p>
      </div>
      <div class="hero-account">
        <strong>{{ connectedUserName }}</strong>
        <span>{{ connectedUserRoleLabel }}</span>
      </div>
    </section>

    <section class="stats-grid" v-if="!isLoadingPage">
      <article v-for="stat in statsCards" :key="stat.label" class="stat-card" :class="stat.tone">
        <strong>{{ stat.value }}</strong>
        <span>{{ stat.label }}</span>
      </article>
    </section>

    <div v-if="isLoadingPage" class="page-state">{{ $t('stateLoading') }}</div>
    <div v-else-if="loadError" class="page-state error">{{ $t('stateLoadError') }}</div>

    <template v-else>
      <section class="dashboard-grid top-grid">
        <article class="panel-card">
          <div class="panel-head">
            <h2>{{ $t('dashboardQuickActions') }}</h2>
          </div>
          <div class="action-grid">
            <router-link
              v-for="action in quickActions"
              :key="action.to"
              class="action-card"
              :to="action.to"
            >
              <span class="material-symbols-outlined">{{ action.icon }}</span>
              <strong>{{ action.label }}</strong>
            </router-link>
          </div>
        </article>

        <article class="panel-card">
          <div class="panel-head">
            <h2>{{ $t('dashboardTodo') }}</h2>
          </div>
          <ul class="todo-list">
            <li v-for="item in todoItems" :key="item">{{ item }}</li>
          </ul>
        </article>
      </section>

      <section class="dashboard-grid">
        <article class="panel-card">
          <div class="panel-head">
            <h2>{{ $t('dashboardActivity') }}</h2>
          </div>
          <div v-if="timelineItems.length" class="timeline-list">
            <div v-for="item in timelineItems" :key="item.key" class="timeline-item">
              <div class="timeline-dot"></div>
              <div>
                <strong>{{ item.title }}</strong>
                <p>{{ item.subtitle }}</p>
              </div>
            </div>
          </div>
          <div v-else class="empty-state">{{ emptyTimelineLabel }}</div>
        </article>

        <article class="panel-card">
          <div class="panel-head">
            <h2>{{ $t('dashboardUpcoming') }}</h2>
          </div>
          <div v-if="upcomingItems.length" class="upcoming-list">
            <div v-for="item in upcomingItems" :key="item.key" class="upcoming-item">
              <strong>{{ item.title }}</strong>
              <span>{{ item.subtitle }}</span>
            </div>
          </div>
          <div v-else class="empty-state">{{ $t('dashboardNoData') }}</div>
        </article>
      </section>
    </template>
  </div>
</template>

<script>
import http from '@/config/httpInterceptor';
import { getCurrentUserRoles } from '@/config/auth';

const PACKAGE_ACTIVITY_ORDER = ['NEW', 'PAYMENTPENDING', 'RESERVED', 'PICKEDUP', 'INDELIVERY', 'DELIVERED'];

export default {
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
      packagesByStatus: {},
      pendingUsers: [],
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
      if (this.dashboardType === 'admin') {
        return this.$t('dashboardRoleAdmin');
      }
      if (this.dashboardType === 'courier') {
        return this.$t('DELIVERY_PERSON');
      }
      if (this.connectedUser.type === 'DELIVERY_PERSON') {
        return this.$t('DELIVERY_PERSON');
      }
      return this.$t('CUSTOMER');
    },
    dashboardTitle() {
      if (this.dashboardType === 'admin') {
        return this.$t('dashboardAdminTitle');
      }
      if (this.dashboardType === 'courier') {
        return this.$t('dashboardCourierTitle');
      }
      return this.$t('dashboardClientTitle');
    },
    dashboardSubtitle() {
      if (this.dashboardType === 'admin') {
        return this.$t('dashboardAdminSubtitle');
      }
      if (this.dashboardType === 'courier') {
        return this.$t('dashboardCourierSubtitle');
      }
      return this.$t('dashboardClientSubtitle');
    },
    flattenedPackages() {
      return Object.values(this.packagesByStatus || {}).flatMap((group) => group || []);
    },
    packageCounts() {
      return this.flattenedPackages.reduce((acc, pkg) => {
        const status = pkg.status || 'UNKNOWN';
        acc[status] = (acc[status] || 0) + 1;
        return acc;
      }, {});
    },
    totalVehicleDocumentsToReview() {
      return this.pendingUsers.reduce((count, user) => {
        const documents = user.document || {};
        return count + ['GRAY_CARD', 'INSURANCE'].filter((key) => !!documents[key]).length;
      }, 0);
    },
    statsCards() {
      if (this.dashboardType === 'admin') {
        return [
          { label: this.$t('dashboardStatValidationQueue'), value: this.pendingUsers.length, tone: 'tone-warn' },
          { label: this.$t('validationVehiclesCount'), value: this.pendingUsers.reduce((count, user) => count + (user.vehicles?.length || 0), 0), tone: 'tone-neutral' },
          { label: this.$t('validationDocumentsCount'), value: this.pendingUsers.reduce((count, user) => count + Object.keys(user.document || {}).length, 0), tone: 'tone-dark' },
          { label: this.$t('dashboardStatVehicleDocs'), value: this.totalVehicleDocumentsToReview, tone: 'tone-success' },
        ];
      }

      if (this.dashboardType === 'courier') {
        return [
          { label: this.$t('dashboardStatPackages'), value: this.flattenedPackages.length, tone: 'tone-dark' },
          { label: this.$t('dashboardStatReserved'), value: this.packageCounts.RESERVED || 0, tone: 'tone-warn' },
          { label: this.$t('dashboardStatOnRoad'), value: (this.packageCounts.PICKEDUP || 0) + (this.packageCounts.INDELIVERY || 0), tone: 'tone-success' },
          { label: this.$t('dashboardStatDelivered'), value: this.packageCounts.DELIVERED || 0, tone: 'tone-neutral' },
        ];
      }

      return [
        { label: this.$t('dashboardStatPackages'), value: this.flattenedPackages.length, tone: 'tone-dark' },
        { label: this.$t('dashboardStatPending'), value: (this.packageCounts.NEW || 0) + (this.packageCounts.PAYMENTPENDING || 0), tone: 'tone-neutral' },
        { label: this.$t('dashboardStatActive'), value: (this.packageCounts.RESERVED || 0) + (this.packageCounts.PICKEDUP || 0) + (this.packageCounts.INDELIVERY || 0), tone: 'tone-warn' },
        { label: this.$t('dashboardStatDelivered'), value: this.packageCounts.DELIVERED || 0, tone: 'tone-success' },
      ];
    },
    quickActions() {
      if (this.dashboardType === 'admin') {
        return [
          { to: '/usersAccountValidation', icon: 'fact_check', label: this.$t('dashboardActionValidation') },
          { to: '/userAccount', icon: 'person', label: this.$t('dashboardActionAccount') },
          { to: '/createPackage', icon: 'box_add', label: this.$t('dashboardActionCreatePackage') },
          { to: '/myPackages', icon: 'inventory_2', label: this.$t('dashboardActionMyPackages') },
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
      if (this.dashboardType === 'admin') {
        return [
          this.$t('dashboardAdminTodoValidation'),
          this.$t('dashboardAdminTodoFollowup'),
          this.$t('dashboardAdminTodoOps'),
          this.$t('dashboardAdminTodoAccount'),
        ];
      }
      if (this.dashboardType === 'courier') {
        return [
          this.$t('dashboardCourierTodoReserve'),
          this.$t('dashboardCourierTodoPickup'),
          this.$t('dashboardCourierTodoDeliver'),
          this.$t('dashboardCourierTodoAccount'),
        ];
      }
      return [
        this.$t('dashboardClientTodoCreate'),
        this.$t('dashboardClientTodoPayment'),
        this.$t('dashboardClientTodoTrack'),
        this.$t('dashboardClientTodoAccount'),
      ];
    },
    timelineItems() {
      if (this.dashboardType === 'admin') {
        return this.pendingUsers
          .slice(0, 5)
          .map((user) => ({
            key: `user-${user.id}`,
            title: `${this.$t('dashboardTimelinePendingUser')} - ${[user.firstName, user.lastName].filter(Boolean).join(' ') || user.emailAddress}`,
            subtitle: `${Object.keys(user.document || {}).length} ${this.$t('validationDocumentsLabel')}`,
          }));
      }

      return this.flattenedPackages
        .slice()
        .sort((a, b) => new Date(b.creationDate || b.createdDate || 0) - new Date(a.creationDate || a.createdDate || 0))
        .slice(0, 6)
        .map((pkg) => ({
          key: pkg.reference || `pkg-${pkg.id}`,
          title: `${this.$t(this.timelineLabelByStatus(pkg.status))} - ${pkg.reference || `PKG${pkg.id}`}`,
          subtitle: this.packageSubtitle(pkg),
        }));
    },
    upcomingItems() {
      if (this.dashboardType === 'admin') {
        return this.pendingUsers
          .slice(0, 4)
          .map((user) => ({
            key: `review-${user.id}`,
            title: [user.firstName, user.lastName].filter(Boolean).join(' ') || user.emailAddress,
            subtitle: `${Object.keys(user.document || {}).length} ${this.$t('validationDocumentsLabel')}`,
          }));
      }

      return this.flattenedPackages
        .filter((pkg) => ['NEW', 'PAYMENTPENDING', 'RESERVED', 'PICKEDUP', 'INDELIVERY'].includes(pkg.status))
        .sort((a, b) => PACKAGE_ACTIVITY_ORDER.indexOf(a.status) - PACKAGE_ACTIVITY_ORDER.indexOf(b.status))
        .slice(0, 4)
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
    async loadDashboard() {
      this.isLoadingPage = true;
      this.loadError = false;
      try {
        if (this.dashboardType === 'admin') {
          const response = await http.get(`${this.$i18n.t('userRootURL')}${this.$i18n.t('getUsersForValidation')}`);
          this.pendingUsers = Array.isArray(response.data) ? response.data : [];
          this.packagesByStatus = {};
          return;
        }

        const endpoint = this.dashboardType === 'courier'
          ? this.$i18n.t('getPackagesByDeliveryPersonUrl')
          : this.$i18n.t('getPackagesBySenderUrl');
        const response = await http.get(`${this.$i18n.t('rootURL')}${endpoint}${this.connectedUser.id}`);
        this.packagesByStatus = response.data || {};
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
      return `${departure} -> ${arrival}`;
    },
    timelineLabelByStatus(status) {
      if (status === 'RESERVED') {
        return 'dashboardTimelineReserved';
      }
      if (status === 'PICKEDUP' || status === 'INDELIVERY') {
        return 'dashboardTimelinePickedUp';
      }
      if (status === 'DELIVERED') {
        return 'dashboardTimelineDelivered';
      }
      return 'dashboardTimelineCreated';
    },
  },
};
</script>

<style scoped>
.dashboard-page {
  min-height: 100%;
  padding: 28px;
  background:
    radial-gradient(circle at top left, rgba(59, 130, 246, 0.12), transparent 28%),
    linear-gradient(180deg, #f4f7fb 0%, #eef3f8 100%);
  box-sizing: border-box;
}

.hero-card,
.panel-card,
.stat-card {
  border: 1px solid #dfe7f2;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.08);
}

.hero-card {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 24px;
  padding: 28px;
  margin-bottom: 20px;
}

.hero-chip {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  background: #e6eefc;
  color: #29548d;
  font-size: 0.78rem;
  font-weight: 700;
  text-transform: uppercase;
}

.hero-card h1 {
  margin: 12px 0 8px;
  font-size: 2.8rem;
  line-height: 1;
  color: #0f172a;
}

.hero-card p {
  margin: 0;
  color: #64748b;
  max-width: 760px;
}

.hero-account {
  min-width: 220px;
  padding: 18px;
  border-radius: 18px;
  background: #0f172a;
  color: #ffffff;
}

.hero-account strong,
.hero-account span {
  display: block;
}

.hero-account strong {
  margin-bottom: 6px;
  font-size: 1.1rem;
}

.hero-account span {
  color: rgba(255, 255, 255, 0.72);
}

.stats-grid,
.dashboard-grid {
  display: grid;
  gap: 18px;
}

.stats-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin-bottom: 18px;
}

.dashboard-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-bottom: 18px;
}

.top-grid {
  align-items: start;
}

.stat-card,
.panel-card {
  padding: 22px;
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

.tone-dark strong {
  color: #0f172a;
}

.tone-warn strong {
  color: #c77b00;
}

.tone-success strong {
  color: #15803d;
}

.tone-neutral strong {
  color: #475569;
}

.panel-head {
  margin-bottom: 16px;
}

.panel-head h2 {
  margin: 0;
  color: #0f172a;
  font-size: 1.35rem;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.action-card {
  display: grid;
  gap: 10px;
  padding: 16px;
  border: 1px solid #e5ebf3;
  border-radius: 18px;
  background: #ffffff;
  color: #0f172a;
  text-decoration: none;
}

.action-card .material-symbols-outlined {
  color: #24558f;
}

.todo-list,
.timeline-list,
.upcoming-list {
  display: grid;
  gap: 12px;
  padding: 0;
  margin: 0;
  list-style: none;
}

.todo-list li,
.upcoming-item {
  padding: 14px 16px;
  border-radius: 16px;
  background: #f8fafc;
  color: #334155;
}

.timeline-item {
  display: grid;
  grid-template-columns: 14px minmax(0, 1fr);
  gap: 12px;
  align-items: start;
}

.timeline-dot {
  width: 14px;
  height: 14px;
  margin-top: 4px;
  border-radius: 50%;
  background: linear-gradient(180deg, #24558f 0%, #38bdf8 100%);
}

.timeline-item strong,
.upcoming-item strong {
  display: block;
  margin-bottom: 4px;
  color: #0f172a;
}

.timeline-item p,
.upcoming-item span {
  margin: 0;
  color: #64748b;
}

.page-state,
.empty-state {
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

@media screen and (max-width: 1100px) {
  .stats-grid,
  .dashboard-grid,
  .action-grid {
    grid-template-columns: 1fr 1fr;
  }

  .hero-card {
    flex-direction: column;
  }

  .hero-account {
    min-width: 0;
    width: 100%;
  }
}

@media screen and (max-width: 767px) {
  .dashboard-page {
    padding: 16px;
  }

  .stats-grid,
  .dashboard-grid,
  .action-grid {
    grid-template-columns: 1fr;
  }

  .hero-card h1 {
    font-size: 2.1rem;
  }
}
</style>
