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
    </div>
    <teleport to="body">
      <button
        v-show="showScrollTopButton"
        class="global-scroll-top"
        type="button"
        aria-label="Scroll to top"
        @click="scrollPageToTop"
      >
        <span class="material-symbols-outlined">arrow_upward</span>
      </button>
    </teleport>
    <AppMessages />
  </div>
</template>

<script>
import SearchBar from './components/SearchBar.vue';
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
      showScrollTopButton: false,
      scrollWatcherTimer: null,
      deferredInstallPrompt: null,
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
    window.addEventListener('beforeinstallprompt', this.handleBeforeInstallPrompt);
    window.addEventListener('appinstalled', this.handleAppInstalled);
    window.addEventListener('qd-install-pwa', this.handleInstallRequest);
    this.bindGlobalScrollWatchers();
    this.refreshPwaInstallState();
  },
  beforeUnmount() {
    window.removeEventListener('qd-track-package-subscribe', this.handleTrackingSubscriptionEvent);
    window.removeEventListener('qd-track-package-unsubscribe', this.handleTrackingUnsubscriptionEvent);
    window.removeEventListener('beforeinstallprompt', this.handleBeforeInstallPrompt);
    window.removeEventListener('appinstalled', this.handleAppInstalled);
    window.removeEventListener('qd-install-pwa', this.handleInstallRequest);
    this.unbindGlobalScrollWatchers();
    this.destroyRealtime();
  },
  methods: {
    isStandaloneMode() {
      return window.matchMedia?.('(display-mode: standalone)').matches || window.navigator.standalone === true;
    },
    refreshPwaInstallState() {
      this.$store.commit('updateCanInstallPwa', !!this.deferredInstallPrompt && !this.isStandaloneMode());
    },
    handleBeforeInstallPrompt(event) {
      event.preventDefault();
      this.deferredInstallPrompt = event;
      this.refreshPwaInstallState();
    },
    handleAppInstalled() {
      this.deferredInstallPrompt = null;
      this.refreshPwaInstallState();
    },
    async handleInstallRequest() {
      if (!this.deferredInstallPrompt) {
        return;
      }

      try {
        await this.deferredInstallPrompt.prompt();
        await this.deferredInstallPrompt.userChoice;
      } catch (error) {
        console.warn('PWA installation prompt failed:', error);
      } finally {
        this.deferredInstallPrompt = null;
        this.refreshPwaInstallState();
      }
    },
    bindGlobalScrollWatchers() {
      window.addEventListener('scroll', this.updateScrollTopButton, true);
      document.addEventListener('scroll', this.updateScrollTopButton, true);
      this.scrollWatcherTimer = window.setInterval(this.updateScrollTopButton, 300);
      this.updateScrollTopButton();
    },
    unbindGlobalScrollWatchers() {
      window.removeEventListener('scroll', this.updateScrollTopButton, true);
      document.removeEventListener('scroll', this.updateScrollTopButton, true);
      if (this.scrollWatcherTimer) {
        window.clearInterval(this.scrollWatcherTimer);
        this.scrollWatcherTimer = null;
      }
    },
    currentScrollOffset() {
      const elementOffsets = Array.from(document.querySelectorAll('*'))
        .map((element) => element.scrollTop || 0);
      return Math.max(
        window.pageYOffset || 0,
        document.documentElement.scrollTop || 0,
        document.body.scrollTop || 0,
        ...elementOffsets
      );
    },
    updateScrollTopButton() {
      this.showScrollTopButton = this.currentScrollOffset() > 120;
    },
    scrollPageToTop() {
      window.scrollTo({ top: 0, behavior: 'smooth' });
      document.documentElement.scrollTo?.({ top: 0, behavior: 'smooth' });
      document.body.scrollTo?.({ top: 0, behavior: 'smooth' });
      Array.from(document.querySelectorAll('*')).forEach((element) => {
        if ((element.scrollTop || 0) > 0) {
          element.scrollTo?.({ top: 0, behavior: 'smooth' });
        }
      });
    },
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
      if (this.$route?.name !== 'homePage' || !this.canRecoverNearbyPackageNotifications() || !navigator.geolocation) {
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
.global-scroll-top {
  position: fixed;
  right: max(16px, env(safe-area-inset-right, 0px) + 12px);
  bottom: max(20px, env(safe-area-inset-bottom, 0px) + 16px);
  width: 52px;
  height: 52px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 999px;
  background: linear-gradient(135deg, #ef7d32, #cf6320);
  color: #fff;
  font-size: 0;
  box-shadow: 0 14px 28px rgba(239, 125, 50, 0.28);
  z-index: 10000;
  cursor: pointer;
  transition: transform 0.2s ease, filter 0.2s ease, box-shadow 0.2s ease;
}
.global-scroll-top:hover {
  filter: brightness(1.05);
  transform: translateY(-2px);
  box-shadow: 0 18px 34px rgba(239, 125, 50, 0.34);
}
.global-scroll-top .material-symbols-outlined {
  font-size: 24px;
  line-height: 1;
  color: #fff;
  font-variation-settings:
    'FILL' 1,
    'wght' 500,
    'GRAD' 0,
    'opsz' 24;
}
@media screen and (max-width: 767px) {
  .global-scroll-top {
    width: 46px;
    height: 46px;
  }
  .global-scroll-top .material-symbols-outlined {
    font-size: 22px;
  }
}
</style>
