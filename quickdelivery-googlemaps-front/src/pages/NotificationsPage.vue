<template>
  <div class="notifications-page">
    <section class="page-header notifications-header">
      <div class="page-header-copy">
        <span class="page-chip">{{ $t('menuNotifications') }}</span>
        <h1>{{ $t('notificationsPageTitle') }}</h1>
        <p>{{ $t('notificationsPageSubtitle') }}</p>
      </div>
      <div class="notifications-header-actions">
        <span class="header-chip">{{ unreadNotificationsCount }} {{ $t('notificationsUnreadLabel') }}</span>
        <button
          type="button"
          class="notifications-action"
          :disabled="!notifications.length"
          @click="markAllAsRead"
        >
          {{ $t('notificationsMarkAllRead') }}
        </button>
      </div>
    </section>

    <section v-if="notifications.length" class="notifications-list">
      <article
        v-for="notification in notifications"
        :key="notification.id"
        class="notification-card"
        :class="{ 'notification-card-unread': !notification.read, 'notification-card-clickable': Boolean(notification.url) }"
        @click="openNotification(notification)"
      >
        <div class="notification-meta">
          <span class="notification-type">{{ notificationTypeLabel(notification.type) }}</span>
          <span class="notification-date">{{ formatDate(notification.receivedAt) }}</span>
        </div>
        <p class="notification-message">{{ notification.message }}</p>
      </article>
    </section>

    <section v-else class="panel-card notifications-empty-state">
      <p>{{ $t('notificationsEmptyState') }}</p>
    </section>
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

      const normalizedUrl = new URL(notification.url, window.location.origin);
      this.$router.push(`${normalizedUrl.pathname}${normalizedUrl.search}`);
    },
  },
};
</script>

<style scoped>
.notifications-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding: clamp(20px, 4vw, 40px);
}

.notifications-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 18px;
}

.notifications-header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.notifications-action {
  border: none;
  border-radius: 999px;
  background: #0f172a;
  color: #fff;
  padding: 10px 16px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.notifications-action:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.notifications-list {
  display: grid;
  gap: 14px;
}

.notification-card {
  background: #fff;
  border: 1px solid #dde3ec;
  border-radius: 18px;
  box-shadow: 0 14px 32px rgba(15, 23, 42, 0.08);
  padding: 18px 20px;
}

.notification-card-clickable {
  cursor: pointer;
}

.notification-card-unread {
  border-color: #f97316;
  box-shadow: 0 18px 34px rgba(249, 115, 22, 0.14);
}

.notification-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.notification-type {
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: #c2410c;
}

.notification-date {
  font-size: 13px;
  color: #64748b;
}

.notification-message {
  margin: 0;
  color: #0f172a;
  line-height: 1.55;
}

.notifications-empty-state {
  padding: 28px;
  text-align: center;
}

@media (max-width: 720px) {
  .notifications-header {
    flex-direction: column;
  }

  .notification-meta {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
