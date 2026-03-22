<template>
  <div class="fullPage">
    <loading v-model:active="isLoading"
             :can-cancel="true"
             :is-full-page="true"/>
    <SearchBar v-if="showSearchBar" />
    <div class="router-view">
      <router-view v-slot="{ Component, route }">
        <keep-alive include="homePage">
          <component v-if="route.meta?.keepAlive" :is="Component" :key="route.name" />
        </keep-alive>
        <component v-if="!route.meta?.keepAlive" :is="Component" :key="route.fullPath" />
      </router-view>
      <ScrollUp/>
    </div>
    <AppMessages />
  </div>
</template>

<script>
import SearchBar from './components/SearchBar.vue';
import ScrollUp from './components/ScrollUp.vue';
import AppMessages from './components/RequestMessage.vue';
import Loading from 'vue-loading-overlay';
import 'vue-loading-overlay/dist/css/index.css';
import http from '@/config/httpInterceptor';
import { hasValidAccessToken } from '@/config/auth';

const NEARBY_PACKAGE_RECOVERY_RADIUS = 20000;
const POSITION_UPDATE_INTERVAL_MS = 10000;
const POSITION_UPDATE_MIN_DISTANCE_METERS = 25;

export default {
  computed: {
    isLoading() {
      return this.$store.state.isLoading;
    },
    showSearchBar() {
      if (this.$route.name === 'landingPage') {
        return false;
      }
      if (!this.$route.meta?.public) {
        return true;
      }
      return hasValidAccessToken();
    },
    connectedUserId() {
      return this.$store.state.connectedUser?.id || null;
    },
    connectedUserRoles() {
      return this.$store.state.connectedUser?.roles || [];
    },
  },
  data() {
    return {
      isUserWithOngoingDelivery: false,
      socket: null,
      socketReady: false,
      watchId: null,
      reconnectTimer: null,
      lastSentPosition: null,
      lastPositionSentAt: 0,
      trackingSubscriptions: {},
    };
  },
  watch: {
    connectedUserId: {
      immediate: true,
      handler(newValue, oldValue) {
        if (!newValue) {
          this.destroyRealtime();
          return;
        }
        if (oldValue && oldValue !== newValue) {
          this.destroyRealtime();
        }
        this.initializeRealtime();
      },
    },
  },
  mounted() {
    window.addEventListener('qd-track-package-subscribe', this.handleTrackingSubscriptionEvent);
    window.addEventListener('qd-track-package-unsubscribe', this.handleTrackingUnsubscriptionEvent);
  },
  beforeUnmount() {
    window.removeEventListener('qd-track-package-subscribe', this.handleTrackingSubscriptionEvent);
    window.removeEventListener('qd-track-package-unsubscribe', this.handleTrackingUnsubscriptionEvent);
    this.destroyRealtime();
  },
  methods: {
    destroyRealtime() {
      this.stopLocationTracking();
      this.socketReady = false;
      if (this.reconnectTimer) {
        clearTimeout(this.reconnectTimer);
        this.reconnectTimer = null;
      }
      if (this.socket) {
        this.socket.close();
        this.socket = null;
      }
    },
    scheduleRealtimeReconnect() {
      if (this.reconnectTimer || !hasValidAccessToken() || !this.connectedUserId) {
        return;
      }
      this.reconnectTimer = setTimeout(() => {
        this.reconnectTimer = null;
        this.initializeRealtime();
      }, 3000);
    },
    stopLocationTracking() {
      if (this.watchId !== null && navigator.geolocation) {
        navigator.geolocation.clearWatch(this.watchId);
      }
      this.watchId = null;
      this.lastSentPosition = null;
      this.lastPositionSentAt = 0;
    },
    async refreshOngoingDeliveryStatus() {
      try {
        const response = await http.get(this.$i18n.t('rootURL') + this.$i18n.t('userWithOngoingDelivery') + this.connectedUserId);
        this.isUserWithOngoingDelivery = response.data;
      } catch {
        this.isUserWithOngoingDelivery = false;
      }
      console.info('QuickDelivery WS ongoing delivery status:', this.isUserWithOngoingDelivery);
      this.startLocationTrackingIfNeeded();
    },
    startLocationTrackingIfNeeded() {
      if (!this.socketReady || !this.isUserWithOngoingDelivery || this.watchId !== null) {
        return;
      }
      if (!navigator.geolocation) {
        console.error('Geolocation is not supported by this browser.');
        return;
      }

      this.watchId = navigator.geolocation.watchPosition(
        (position) => {
          const newPosition = {
            latitude: position.coords.latitude,
            longitude: position.coords.longitude,
          };

          if (!this.shouldSendPositionUpdate(newPosition)) {
            return;
          }

          this.lastSentPosition = newPosition;
          this.lastPositionSentAt = Date.now();
          this.sendSocketMessage({
            type: 'PACKAGE_POSITION_UPDATE',
            from: String(this.connectedUserId),
            to: 'PACKAGE_SERVICE',
            message: 'PACKAGE_POSITION_UPDATE',
            positionDTO: newPosition,
            url: '',
          });
        },
        (error) => {
          console.error('Error getting location:', error);
        },
        {
          enableHighAccuracy: true,
          maximumAge: 5000,
          timeout: 15000,
        }
      );
    },
    shouldSendPositionUpdate(newPosition) {
      if (!this.lastSentPosition) {
        return true;
      }

      const elapsed = Date.now() - this.lastPositionSentAt;
      const distance = this.calculateDistanceInMeters(this.lastSentPosition, newPosition);
      return elapsed >= POSITION_UPDATE_INTERVAL_MS && distance >= POSITION_UPDATE_MIN_DISTANCE_METERS;
    },
    calculateDistanceInMeters(previousPosition, nextPosition) {
      const earthRadius = 6371000;
      const toRadians = (value) => (value * Math.PI) / 180;
      const deltaLatitude = toRadians(nextPosition.latitude - previousPosition.latitude);
      const deltaLongitude = toRadians(nextPosition.longitude - previousPosition.longitude);
      const latitude1 = toRadians(previousPosition.latitude);
      const latitude2 = toRadians(nextPosition.latitude);

      const a = Math.sin(deltaLatitude / 2) ** 2
        + Math.cos(latitude1) * Math.cos(latitude2) * Math.sin(deltaLongitude / 2) ** 2;
      return 2 * earthRadius * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    },
    sendSocketMessage(payload) {
      if (this.socket?.readyState !== WebSocket.OPEN) {
        return false;
      }
      this.socket.send(JSON.stringify(payload));
      return true;
    },
    handleTrackingSubscriptionEvent(event) {
      const packageReference = event?.detail?.packageReference;
      const guestAccessToken = event?.detail?.guestAccessToken;
      if (!packageReference || !guestAccessToken) {
        return;
      }
      this.trackingSubscriptions[packageReference] = guestAccessToken;
      this.sendSocketMessage({
        type: 'TRACK_PACKAGE_SUBSCRIBE',
        from: String(this.connectedUserId || ''),
        to: 'PACKAGE_SERVICE',
        packageReference,
        guestAccessToken,
      });
    },
    handleTrackingUnsubscriptionEvent(event) {
      const packageReference = event?.detail?.packageReference;
      if (!packageReference) {
        return;
      }
      delete this.trackingSubscriptions[packageReference];
      this.sendSocketMessage({
        type: 'TRACK_PACKAGE_UNSUBSCRIBE',
        from: String(this.connectedUserId || ''),
        to: 'PACKAGE_SERVICE',
        packageReference,
      });
    },
    flushTrackingSubscriptions() {
      Object.entries(this.trackingSubscriptions).forEach(([packageReference, guestAccessToken]) => {
        this.sendSocketMessage({
          type: 'TRACK_PACKAGE_SUBSCRIBE',
          from: String(this.connectedUserId || ''),
          to: 'PACKAGE_SERVICE',
          packageReference,
          guestAccessToken,
        });
      });
    },
    seenNearbyPackagesStorageKey() {
      return `quickdelivery.seenNearbyPackages.${this.connectedUserId}`;
    },
    loadSeenNearbyPackages() {
      if (!this.connectedUserId || typeof window === 'undefined') {
        return [];
      }

      try {
        const rawValue = window.localStorage.getItem(this.seenNearbyPackagesStorageKey());
        const parsedValue = rawValue ? JSON.parse(rawValue) : [];
        return Array.isArray(parsedValue) ? parsedValue : [];
      } catch (error) {
        console.warn('Unable to load seen nearby packages:', error);
        return [];
      }
    },
    saveSeenNearbyPackages(references) {
      if (!this.connectedUserId || typeof window === 'undefined') {
        return;
      }

      try {
        window.localStorage.setItem(this.seenNearbyPackagesStorageKey(), JSON.stringify(references.slice(-200)));
      } catch (error) {
        console.warn('Unable to save seen nearby packages:', error);
      }
    },
    canRecoverNearbyPackageNotifications() {
      return this.connectedUserRoles.includes('ROLE_LIVREUR') || this.connectedUserRoles.includes('ROLE_ADMIN');
    },
    async recoverNearbyPackageNotifications() {
      if (!this.canRecoverNearbyPackageNotifications() || !navigator.geolocation) {
        return;
      }

      navigator.geolocation.getCurrentPosition(async (position) => {
        const { latitude, longitude } = position.coords;
        const seenReferences = this.loadSeenNearbyPackages();

        try {
          const response = await http.get(
            `${this.$i18n.t('rootURL')}${this.$i18n.t('getPackagesAroundMe')}${latitude}&longitude=${longitude}&rayonEnMetres=${NEARBY_PACKAGE_RECOVERY_RADIUS}`
          );

          const packages = Array.isArray(response?.data) ? response.data : [];
          const newPackages = packages.filter((aPackage) => aPackage?.reference && !seenReferences.includes(aPackage.reference));

          if (!newPackages.length) {
            return;
          }

          const updatedReferences = [...seenReferences];
          newPackages.forEach((aPackage) => {
            updatedReferences.push(aPackage.reference);
            const notificationPayload = {
              type: 'NEW_PACKAGE_NOTIFICATION',
              from: 'PACKAGE_SERVICE',
              to: String(this.connectedUserId),
              message: 'There is a new package around you :)',
              url: `/package?id=${aPackage.reference}`,
              receivedAt: new Date().toISOString(),
            };
            this.$store.commit('pushNotification', notificationPayload);
            this.showRealtimeNotification(notificationPayload);
          });

          this.saveSeenNearbyPackages(updatedReferences);
        } catch (error) {
          console.warn('Unable to recover nearby package notifications:', error);
        }
      }, (error) => {
        console.warn('Unable to recover nearby package notifications without location:', error);
      });
    },
    async initializeRealtime() {
      if (!hasValidAccessToken() || !this.connectedUserId || this.socket) {
        return;
      }

      console.info('QuickDelivery WS initializing for user:', this.connectedUserId, 'url:', this.$i18n.t('wsURL'));
      await this.refreshOngoingDeliveryStatus();

      this.socket = new WebSocket(this.$i18n.t('wsURL'));
      this.socket.onopen = () => {
        console.info('QuickDelivery WS connected');
        this.socketReady = true;
        this.recoverNearbyPackageNotifications();
        this.flushTrackingSubscriptions();
        this.startLocationTrackingIfNeeded();
      };

      this.socket.onmessage = (event) => {
        console.info('QuickDelivery WS received raw:', event.data);
        const jsonData = JSON.parse(event.data);
        const isTargetedNotification = [
          'NEW_PACKAGE_NOTIFICATION',
          'PACKAGE_RESERVATION_OTP_NOTIFICATION',
          'PACKAGE_PICKUP_NOTIFICATION',
          'PACKAGE_DELIVERY_NOTIFICATION',
        ].includes(jsonData.type) && this.connectedUserId === parseInt(jsonData.to);

        if (isTargetedNotification) {
            if (jsonData.type === 'NEW_PACKAGE_NOTIFICATION' && jsonData.url) {
              const packageReference = new URL(jsonData.url, window.location.origin).searchParams.get('id');
              if (packageReference) {
                const seenReferences = this.loadSeenNearbyPackages();
                if (!seenReferences.includes(packageReference)) {
                  seenReferences.push(packageReference);
                  this.saveSeenNearbyPackages(seenReferences);
                }
              }
            }
            this.$store.commit('pushNotification', jsonData);
            this.showRealtimeNotification(jsonData);
        } else if (jsonData.type === 'PACKAGE_POSITION_UPDATE' && jsonData.packageReference && jsonData.positionDTO) {
            this.$store.commit('updatePackageLastPosition', {
              packageReference: jsonData.packageReference,
              position: jsonData.positionDTO,
            });
        }
      };

      this.socket.onclose = () => {
        console.info('QuickDelivery WS closed');
        this.socketReady = false;
        this.stopLocationTracking();
        this.socket = null;
        this.scheduleRealtimeReconnect();
      };

      this.socket.onerror = (error) => {
        console.error('WebSocket error:', error);
      };
    },
    showRealtimeNotification(jsonData) {
      this.$store.commit('updateShowMessage', true);
      this.$store.commit('updateRequestSuccess', true);
      this.$store.commit('updateRequestMessage', jsonData.message || this.$t('notificationTitle'));
      setTimeout(() => {
        this.$store.commit('updateShowMessage', false);
      }, 9000);

      const openTarget = () => {
        if (jsonData.url) {
          const normalizedUrl = jsonData.url.startsWith('/')
            ? `${window.location.origin}${jsonData.url}`
            : jsonData.url;
          window.location.href = normalizedUrl;
        }
      };

      if (Notification.permission === 'granted') {
        const notification = new Notification(this.$t('notificationTitle'), {
          body: jsonData.message
        });
        notification.onclick = openTarget;
      } else if (Notification.permission !== 'denied') {
        Notification.requestPermission().then((permission) => {
          if (permission === 'granted') {
            const notification = new Notification(this.$t('notificationTitle'), {
              body: jsonData.message
            });
            notification.onclick = openTarget;
          }
        });
      }
    },
  },
  components: {
    SearchBar,
    Loading,
    AppMessages,
    ScrollUp,
  },
};
</script>

<style>
.fullPage{
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  min-height: 100dvh;
  width: 100%;
  max-width: 100%;
  overflow-x: hidden;
}
.router-view{
  flex-grow: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 100%;
  overflow-x: hidden;
}
</style>
