<template>
  <div class="accounts-shell">
    <div class="accounts-grid">
      <article v-for="user in users" :key="user.id || user.emailAddress" class="account-card">
        <div class="card-head">
          <div>
            <h3>{{ user.firstName }} {{ user.lastName }}</h3>
            <p>{{ user.emailAddress }}</p>
          </div>
          <span class="status-chip">{{ $t('validationPendingBadge') }}</span>
        </div>

        <div class="account-meta">
          <div class="meta-row">
            <span class="material-symbols-outlined">call</span>
            <span>{{ user.phone || '-' }}</span>
          </div>
          <div class="meta-row">
            <span class="material-symbols-outlined">directions_car</span>
            <span>{{ vehicleSummary(user) }}</span>
          </div>
          <div class="meta-row">
            <span class="material-symbols-outlined">description</span>
            <span>{{ documentCount(user) }} {{ $t('validationDocumentsLabel') }}</span>
          </div>
        </div>

        <div class="card-actions">
          <button class="details-btn" @click="showDetails(user)">{{ $t('packagesArroundMArkerDetailActionsDetails') }}</button>
        </div>
      </article>
    </div>

  </div>
</template>

<script>
export default {
  props: {
    users: {
      type: Array,
      required: true,
    },
  },
  data() {
    return {};
  },
  methods: {
    showDetails(user) {
      this.$router.push({
        path: '/userValidationDetails',
        query: {
          id: user.id || '',
          returnTo: this.$route.fullPath,
        },
      });
    },
    vehicleSummary(user) {
      const vehicle = user?.vehicles?.[0];
      if (!vehicle) {
        return '-';
      }
      return [vehicle.brand, vehicle.model, vehicle.registrationNumber].filter(Boolean).join(' - ');
    },
    documentCount(user) {
      return user?.documentCount || Object.keys(user?.documents || user?.document || {}).length || 0;
    },
  },
};
</script>

<style scoped>
.accounts-shell {
  width: 100%;
}

.accounts-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.account-card {
  padding: 20px;
  border: 1px solid #e5e7eb;
  border-radius: 18px;
  background: #ffffff;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.06);
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 18px;
}

.card-head h3 {
  margin: 0;
  font-size: 1.45rem;
  line-height: 1.1;
  color: #0f172a;
}

.card-head p {
  margin: 6px 0 0;
  color: #64748b;
  overflow-wrap: anywhere;
}

.status-chip {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  background: #fef3c7;
  color: #b45309;
  font-size: 0.82rem;
  font-weight: 700;
  white-space: nowrap;
}

.account-meta {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-bottom: 18px;
  margin-bottom: 18px;
  border-bottom: 1px solid #eef2f7;
}

.meta-row {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  color: #334155;
}

.meta-row span:last-child {
  overflow-wrap: anywhere;
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

.card-actions {
  display: flex;
  justify-content: flex-start;
}

@media screen and (max-width: 1200px) {
  .accounts-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media screen and (max-width: 767px) {
  .accounts-grid {
    grid-template-columns: 1fr;
  }
}
</style>
