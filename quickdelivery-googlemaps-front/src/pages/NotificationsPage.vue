<template>
  <div class="notifications-page qd-page">
    <header class="qd-page-header">
      <div class="header-main">
        <span class="page-chip gold-gradient">{{ $t('menuNotifications') }}</span>
        <h1>{{ $t('notificationsPageTitle') }}</h1>
        <p>{{ $t('notificationsPageSubtitle') }}</p>
      </div>
      <div class="qd-page-header-actions">
        <div class="unread-badge-premium" :class="{ 'has-unread': unreadNotificationsCount > 0 }">
            <span class="pulse-ring"></span>
            <span class="unread-count">{{ unreadNotificationsCount }}</span>
            <span class="unread-label">{{ $t('notificationsUnreadLabel') }}</span>
        </div>
        <button
          type="button"
          class="qd-btn-primary mark-all-btn"
          :disabled="!notifications.length"
          @click="markAllAsRead"
        >
          <span class="material-symbols-outlined">done_all</span>
          {{ $t('notificationsMarkAllRead') }}
        </button>
      </div>
    </header>

    <div class="notifications-container">
      <transition-group name="staggered-list" tag="section" v-if="notifications.length" class="notifications-list">
        <article
          v-for="(notification, index) in notifications"
          :key="notification.id"
          class="notification-card-premium"
          :class="{ 'is-unread': !notification.read, 'is-link': Boolean(notification.url) }"
          :style="{ '--index': index }"
          @click="openNotification(notification)"
        >
          <div class="card-indicator"></div>
          <div class="card-body">
            <div class="card-meta">
              <span class="type-pill" :class="notification.type.toLowerCase()">{{ notificationTypeLabel(notification.type) }}</span>
              <span class="timestamp">{{ formatDate(notification.receivedAt) }}</span>
            </div>
            <p class="message-text">{{ notification.message }}</p>
          </div>
          <span v-if="notification.url" class="material-symbols-outlined arrow-icon">chevron_right</span>
        </article>
      </transition-group>

      <section v-else class="empty-state-premium">
        <div class="empty-icon-box">
            <span class="material-symbols-outlined">notifications_off</span>
        </div>
        <h3>{{ $t('notificationsEmptyState') }}</h3>
        <p>{{ $t('notificationsEmptySubtitle') || 'Revenez plus tard pour de nouveaux messages.' }}</p>
      </section>
    </div>
  </div>
</template>

<script>
import http from '@/config/httpInterceptor';

export default {
  computed: {
    notifications() {
      return this.$store.state.notifications || [];
    },
    unreadNotificationsCount() {
      return this.$store.getters.unreadNotificationsCount || 0;
    },
  },
  methods: {
    notificationTypeLabel(type) {
      const mapping = {
        PACKAGE_CREATED_NOTIFICATION: 'notificationTypeCreated',
        NEW_PACKAGE_NOTIFICATION: 'notificationTypeNewPackage',
        PACKAGE_RESERVATION_OTP_NOTIFICATION: 'notificationTypeReservationOtp',
        PACKAGE_COURIER_ARRIVED_FOR_PICKUP_NOTIFICATION: 'notificationTypeCourierArrivedForPickup',
        PACKAGE_PICKUP_NOTIFICATION: 'notificationTypePickup',
        PACKAGE_DELIVERY_NOTIFICATION: 'notificationTypeDelivery',
      };

      return this.$t(mapping[type] || 'notificationTypeGeneric');
    },
    formatDate(value) {
      if (!value) {
        return '';
      }

      return new Intl.DateTimeFormat(this.$i18n.locale, {
        dateStyle: 'medium',
        timeStyle: 'short',
      }).format(new Date(value));
    },
    markAllAsRead() {
      const userId = this.$store.state.connectedUser?.id;
      if (!userId) {
        return;
      }
      http.post(`${this.$i18n.t('rootURL')}notifications/mark-all-read?userId=${encodeURIComponent(userId)}`, null, { silent: true })
        .then(() => {
          this.$store.commit('markAllNotificationsRead');
        })
        .catch((error) => {
          console.warn('Unable to mark all notifications as read:', error);
        });
    },
    openNotification(notification) {
      const userId = this.$store.state.connectedUser?.id;
      if (notification.id && userId && !notification.read) {
        http.post(
          `${this.$i18n.t('rootURL')}notifications/mark-read?notificationId=${encodeURIComponent(notification.id)}&userId=${encodeURIComponent(userId)}`,
          null,
          { silent: true },
        )
          .then(() => {
            this.$store.commit('markNotificationRead', notification.id);
          })
          .catch((error) => {
            console.warn('Unable to mark notification as read:', error);
          });
      } else if (notification.id && !notification.read) {
        this.$store.commit('markNotificationRead', notification.id);
      }

      if (!notification.url) {
        return;
      }

      if (notification.url.startsWith('/')) {
        this.$router.push(notification.url);
        return;
      }

      try {
        const normalizedUrl = new URL(notification.url, window.location.origin);
        this.$router.push(`${normalizedUrl.pathname}${normalizedUrl.search}`);
      } catch (e) {
        this.$router.push(notification.url);
      }
    },
  },
};
</script>

<style scoped>
.notifications-page {
  max-width: 1200px;
  margin: 0 auto;
  padding-bottom: 40px;
}

.gold-gradient {
  background: linear-gradient(135deg, #f59e0b, #d97706);
  color: #fff;
}

.header-actions-wrap {
  display: flex;
  align-items: center;
  gap: 20px;
}

.unread-badge-premium {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 18px;
  background: #f1f5f9;
  border-radius: 12px;
  position: relative;
}

.unread-badge-premium.has-unread {
  background: #fff;
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.05);
}

.unread-count {
  font-size: 1.25rem;
  font-weight: 900;
  color: #0f172a;
}

.unread-label {
  font-size: 0.85rem;
  font-weight: 700;
  color: #64748b;
  text-transform: uppercase;
}

.pulse-ring {
    position: absolute;
    left: 8px;
    width: 8px;
    height: 8px;
    background: #f59e0b;
    border-radius: 50%;
    display: none;
}

.has-unread .pulse-ring {
    display: block;
    animation: badge-pulse 1.5s infinite;
}

@keyframes badge-pulse {
    0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(245, 158, 11, 0.7); }
    70% { transform: scale(1); box-shadow: 0 0 0 10px rgba(245, 158, 11, 0); }
    100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(245, 158, 11, 0); }
}

.mark-all-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  flex: 1 1 auto;
  min-width: 0;
  border-radius: 999px !important;
}

.notifications-page .qd-page-header-actions {
  display: flex;
  align-items: stretch;
  gap: 10px;
  flex-wrap: nowrap;
}

.notifications-page .unread-badge-premium {
  flex: 0 0 auto;
}

.notifications-container {
    width: 100%;
}

.notifications-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.notification-card-premium {
  position: relative;
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 24px;
  background: #fff;
  border: 1px solid #f1f5f9;
  border-radius: 24px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  animation: slide-in 0.5s ease backwards;
  animation-delay: calc(var(--index) * 0.05s);
}

.notification-card-premium:hover {
  border-color: #e2e8f0;
  transform: translateX(8px);
  box-shadow: 0 15px 35px rgba(15, 23, 42, 0.06);
}

.notification-card-premium.is-unread {
  background: #fff;
  border-color: rgba(245, 158, 11, 0.3);
}

.notification-card-premium.is-unread .card-indicator {
  background: #f59e0b;
}

.card-indicator {
  width: 4px;
  height: 48px;
  background: #e2e8f0;
  border-radius: 99px;
  transition: background 0.3s;
}

.card-body {
  flex: 1;
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.type-pill {
  font-size: 0.7rem;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  padding: 4px 10px;
  background: #f1f5f9;
  color: #64748b;
  border-radius: 8px;
}

.type-pill.package_delivery_notification { background: #dcfce7; color: #166534; }
.type-pill.new_package_notification { background: #e0f2fe; color: #0369a1; }
.type-pill.package_reservation_otp_notification { background: #fef3c7; color: #92400e; }

.timestamp {
  font-size: 0.85rem;
  color: #94a3b8;
  font-weight: 500;
}

.message-text {
  margin: 0;
  font-size: 1.05rem;
  color: #1e293b;
  line-height: 1.5;
  font-weight: 500;
}

.arrow-icon {
  color: #cbd5e1;
  transition: transform 0.2s, color 0.2s;
}

.notification-card-premium:hover .arrow-icon {
  color: #0f172a;
  transform: translateX(4px);
}

.is-link {
  cursor: pointer;
}

/* Empty State */
.empty-state-premium {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 40px;
  background: #fff;
  border-radius: 32px;
  border: 1px dashed #e2e8f0;
  text-align: center;
}

.empty-icon-box {
  width: 80px;
  height: 80px;
  background: #f8fafc;
  border-radius: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
}

.empty-icon-box .material-symbols-outlined {
    font-size: 40px;
    color: #cbd5e1;
}

.empty-state-premium h3 {
    margin: 0 0 8px;
    font-size: 1.5rem;
    font-weight: 800;
    color: #0f172a;
}

.empty-state-premium p {
    color: #94a3b8;
    font-size: 1.1rem;
    margin: 0;
}

@keyframes slide-in {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

/* Responsive */
@media (max-width: 900px) {
  .page-header-premium {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 600px) {
    .notifications-page {
        padding: 12px;
    }
    .header-main-content h1 {
        font-size: 1.5rem;
        line-height: 1.12;
    }
    .header-actions-wrap {
        width: 100%;
        flex-direction: column;
        align-items: stretch;
    }
    .notifications-page .qd-page-header-actions {
        flex-direction: row;
        align-items: stretch;
        gap: 8px;
        flex-wrap: nowrap;
    }
    .unread-badge-premium {
        padding: 8px 10px;
        gap: 6px;
    }
    .unread-count {
        font-size: 1rem;
    }
    .unread-label {
        font-size: 0.68rem;
    }
    .mark-all-btn {
        min-width: 0;
        padding-left: 10px !important;
        padding-right: 10px !important;
    }
    .notification-card-premium {
        padding: 12px;
        gap: 10px;
    }
    .empty-icon-box {
        width: 52px;
        height: 52px;
        margin-bottom: 12px;
        border-radius: 14px;
    }
    .empty-icon-box .material-symbols-outlined {
        font-size: 28px;
    }
    .empty-state-premium h3 {
        font-size: 1rem;
        line-height: 1.18;
    }
    .empty-state-premium p {
        font-size: 0.82rem;
        line-height: 1.3;
    }
    .card-indicator {
        display: none;
    }
}
</style>
