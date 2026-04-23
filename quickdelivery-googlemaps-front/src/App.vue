<template>
  <div class="fullPage">
    <loading v-model:active="showGlobalLoader"
             :can-cancel="true"
             :is-full-page="true"/>
    <SearchBar v-if="showSearchBar" />
    <div class="router-view">
      <router-view v-slot="{ Component, route }">
        <keep-alive include="HomePage">
          <component v-if="route.meta?.keepAlive && Component" :is="Component" :key="route.name" />
        </keep-alive>
        <component v-if="!route.meta?.keepAlive && Component" :is="Component" :key="route.fullPath" />
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
import { getAccessToken, hasValidAccessToken } from '@/config/auth';
import { getGatewayBaseUrl, isMobileCapacitorRuntime } from '@/config/network';
import { Capacitor, CapacitorHttp, registerPlugin } from '@capacitor/core';
import { App as CapacitorApp } from '@capacitor/app';
import { Geolocation } from '@capacitor/geolocation';
import { LocalNotifications } from '@capacitor/local-notifications';
import { PushNotifications } from '@capacitor/push-notifications';

const POSITION_UPDATE_INTERVAL_MS = 10000;
const POSITION_UPDATE_MIN_DISTANCE_METERS = 25;
const TRACKING_POSITION_ENDPOINT = `${getGatewayBaseUrl()}/packages/v1/tracking/position`;
const TRACKING_PACKAGE_POSITION_ENDPOINT = `${getGatewayBaseUrl()}/packages/v1/tracking/package-position`;
const TRACKING_ACTIVE_PACKAGE_ENDPOINT = `${getGatewayBaseUrl()}/packages/v1/tracking/active-package`;
const MOBILE_DEVICE_STORAGE_KEY = 'quickdelivery.mobileDeviceId';
const MOBILE_PUSH_TOKEN_STORAGE_KEY = 'quickdelivery.mobilePushToken';
const BackgroundGeolocation = registerPlugin('BackgroundGeolocation');
const FirebaseStatus = registerPlugin('FirebaseStatus');

export default {
  computed: {
    isLoading() {
      return this.$store.state.isLoading;
    },
    showGlobalLoader() {
      return this.isLoading && this.$route.name !== 'homePage';
    },
    showSearchBar() {
      if (this.$route.name === 'landingPage' || this.$route.meta?.publicOnly) {
        return false;
      }
      if (hasValidAccessToken() && ['createPackage', 'userSignInPage'].includes(this.$route.name)) {
        return true;
      }
      if (!this.$route.meta?.public) {
        return true;
      }
      return false;
    },
    connectedUserId() {
      return this.$store.state.connectedUser?.id || null;
    },
    connectedUserRoles() {
      return this.$store.state.connectedUser?.roles || [];
    },
    currentLocaleCode() {
      const currentLocale = this.$i18n?.locale || this.$i18n?.global?.locale || 'en';
      const normalizedLocale = String(currentLocale).replace('_', '-').toLowerCase();
      if (normalizedLocale.startsWith('fr')) {
        return 'fr';
      }
      if (normalizedLocale.startsWith('en')) {
        return 'en';
      }
      return normalizedLocale;
    },
    isNearbyCourierRole() {
      return this.connectedUserRoles.includes('ROLE_LIVREUR');
    },
  },
  data() {
    return {
        isUserWithOngoingDelivery: false,
        activeTrackingPackageReference: '',
        socket: null,
      socketReady: false,
      watchId: null,
      backgroundWatcherId: null,
      reconnectTimer: null,
      lastSentPosition: null,
      lastPositionSentAt: 0,
      trackingSubscriptions: {},
      showScrollTopButton: false,
      scrollWatcherTimer: null,
      deferredInstallPrompt: null,
      lastNearbyRecoveryKey: '',
      lastNearbyRecoveryAt: 0,
      localNotificationActionListener: null,
      nextLocalNotificationId: 1,
      localNotificationChannelsReady: false,
      mobilePushToken: '',
      pushRegistrationPromise: null,
      pushRegistrationListener: null,
      recentlySeenNotificationIds: [],
      pushRegistrationErrorListener: null,
      pushNotificationReceivedListener: null,
      pushNotificationActionListener: null,
      appStateListener: null,
      isNativeAppForeground: true,
      firebasePushAvailable: null,
    };
  },
  watch: {
    connectedUserId: {
      immediate: true,
      handler(newValue, oldValue) {
        if (oldValue && oldValue !== newValue) {
          this.destroyRealtime();
        }
        this.syncRealtimeConnection();
        this.registerRealtimeNotificationSession();
        this.syncNotificationLocalePreference();
        this.syncMobileDeviceRegistration();
        this.syncNotificationsFromBackend();
        if (newValue && hasValidAccessToken()) {
          this.refreshOngoingDeliveryStatus();
          return;
        }
        this.$store.commit('setReservationAvailability', {
          canReserve: true,
          activeRouteBlocking: false,
          capacityReached: false,
          activeReservations: 0,
          maxReservations: 0,
          reason: 'available',
        });
      },
    },
    '$route.fullPath'() {
      this.syncRealtimeConnection();
    },
    currentLocaleCode() {
      this.syncNotificationLocalePreference();
      this.syncMobileDeviceRegistration();
    },
  },
  mounted() {
    window.addEventListener('qd-track-package-subscribe', this.handleTrackingSubscriptionEvent);
    window.addEventListener('qd-package-soft-lock', this.handleSoftLockEvent);
    window.addEventListener('qd-track-package-unsubscribe', this.handleTrackingUnsubscriptionEvent);
    window.addEventListener('qd-refresh-reservation-availability', this.refreshReservationAvailability);
    window.addEventListener('beforeinstallprompt', this.handleBeforeInstallPrompt);
    window.addEventListener('appinstalled', this.handleAppInstalled);
    window.addEventListener('qd-install-pwa', this.handleInstallRequest);
    this.registerLocalNotificationListener();
    this.registerPushNotificationListeners();
    this.ensureRealtimeLocalNotificationPermission();
    this.bindGlobalScrollWatchers();
    this.refreshPwaInstallState();
  },
  beforeUnmount() {
    window.removeEventListener('qd-track-package-subscribe', this.handleTrackingSubscriptionEvent);
    window.removeEventListener('qd-package-soft-lock', this.handleSoftLockEvent);
    window.removeEventListener('qd-track-package-unsubscribe', this.handleTrackingUnsubscriptionEvent);
    window.removeEventListener('qd-refresh-reservation-availability', this.refreshReservationAvailability);
    window.removeEventListener('beforeinstallprompt', this.handleBeforeInstallPrompt);
    window.removeEventListener('appinstalled', this.handleAppInstalled);
    window.removeEventListener('qd-install-pwa', this.handleInstallRequest);
    this.unregisterLocalNotificationListener();
    this.unregisterPushNotificationListeners();
    this.unbindGlobalScrollWatchers();
    this.destroyRealtime();
  },
  methods: {
    buildNotificationTargetUrl(rawUrl) {
      if (!rawUrl) {
        return '';
      }
      return rawUrl.startsWith('/')
        ? `${window.location.origin}${rawUrl}`
        : rawUrl;
    },
    openNotificationTarget(rawUrl) {
      const normalizedUrl = this.buildNotificationTargetUrl(rawUrl);
      if (!normalizedUrl) {
        return;
      }
      window.location.href = normalizedUrl;
    },
    registerLocalNotificationListener() {
      if (!isMobileCapacitorRuntime() || this.localNotificationActionListener) {
        return;
      }
      this.localNotificationActionListener = LocalNotifications.addListener(
        'localNotificationActionPerformed',
        (event) => {
          const targetUrl = event?.notification?.extra?.targetUrl;
          this.openNotificationTarget(targetUrl);
        },
      );
    },
    async ensureLocalNotificationChannels() {
      if (!isMobileCapacitorRuntime()) {
        return false;
      }
      if (this.localNotificationChannelsReady) {
        return true;
      }

      try {
        console.info('QuickDelivery: creating/ensuring local notification channel');
        await LocalNotifications.createChannel({
          id: 'quickdelivery-realtime',
          name: 'QuickDelivery Notifications',
          description: 'Notifications de service QuickDelivery',
          importance: 5,
          visibility: 1,
          sound: 'default',
          vibration: true,
          lights: true,
        });
        this.localNotificationChannelsReady = true;
        return true;
      } catch (error) {
        console.error('QuickDelivery: Unable to create local notification channel:', error);
        return false;
      }
    },
    async ensureRealtimeLocalNotificationPermission() {
      if (!isMobileCapacitorRuntime()) {
        return false;
      }

      try {
        const permissions = await LocalNotifications.checkPermissions();
        if (permissions.display === 'granted') {
          return true;
        }
        const requestResult = await LocalNotifications.requestPermissions();
        return requestResult.display === 'granted';
      } catch (error) {
        console.warn('Unable to request realtime local notification permission:', error);
        return false;
      }
    },
    getRealtimeNotificationTitle(jsonData) {
      if (!jsonData || !jsonData.type) {
        return this.$t('notificationTitle');
      }

      switch (jsonData.type) {
        case 'PACKAGE_SOFT_LOCKED':
          this.forwardSoftLockToMap(jsonData);
          break;
        case 'NEW_PACKAGE_NOTIFICATION':
          return this.$t('notificationTypeNewPackage');
        case 'PACKAGE_CREATED_NOTIFICATION':
          return this.$t('notificationTypeCreated');
        case 'PACKAGE_RESERVATION_OTP_NOTIFICATION':
          return this.$t('notificationTypeReservationOtp');
        case 'PACKAGE_RESERVED_NOTIFICATION':
          return this.$t('notificationTypeReserved');
        case 'PACKAGE_COURIER_ARRIVED_FOR_PICKUP_NOTIFICATION':
          return this.$t('notificationTypeCourierArrivedForPickup');
        case 'PACKAGE_PICKUP_STOP_ARRIVAL_NOTIFICATION':
          return this.$t('notificationTypePickupArrival');
        case 'PACKAGE_DELIVERY_STOP_ARRIVAL_NOTIFICATION':
          return this.$t('notificationTypeDeliveryArrival');
        case 'PACKAGE_PICKUP_NOTIFICATION':
          return this.$t('notificationTypePickup');
        case 'PACKAGE_DELIVERY_NOTIFICATION':
          return this.$t('notificationTypeDelivery');
        case 'PACKAGE_PICKUP_FAILED_SENDER_ABSENT_NOTIFICATION':
          return this.$t('notificationTypePickupFailedSenderAbsent');
        case 'PACKAGE_DELIVERY_FAILED_RECIPIENT_ABSENT_NOTIFICATION':
          return this.$t('notificationTypeDeliveryFailedRecipientAbsent');
        case 'PACKAGE_RELAY_DROPOFF_REQUIRED_NOTIFICATION':
          return this.$t('notificationTypeRelayDropoffRequired');
        case 'DELIVERY_ROUTE_RESERVED_WARNING_NOTIFICATION':
          return this.$t('notificationTypeRouteReservedWarning');
        case 'DELIVERY_ROUTE_CANCELLED_PENALTY_NOTIFICATION':
          return this.$t('notificationTypeRouteCancelledPenalty');
        default:
          return this.$t('notificationTitle');
      }
    },
    resolveCurrentLocaleCode() {
      return this.currentLocaleCode;
    },
    mapBackendEventTypeToFrontendType(eventType) {
      switch (eventType) {
        case 'PACKAGE_CREATED':
          return 'PACKAGE_CREATED_NOTIFICATION';
        case 'PACKAGE_NEARBY':
          return 'NEW_PACKAGE_NOTIFICATION';
        case 'PACKAGE_RESERVATION_OTP':
          return 'PACKAGE_RESERVATION_OTP_NOTIFICATION';
        case 'PACKAGE_COURIER_ARRIVED_FOR_PICKUP':
          return 'PACKAGE_COURIER_ARRIVED_FOR_PICKUP_NOTIFICATION';
        case 'PACKAGE_PICKUP_STOP_ARRIVAL':
          return 'PACKAGE_PICKUP_STOP_ARRIVAL_NOTIFICATION';
        case 'PACKAGE_DELIVERY_STOP_ARRIVAL':
          return 'PACKAGE_DELIVERY_STOP_ARRIVAL_NOTIFICATION';
        case 'PACKAGE_PICKED_UP':
          return 'PACKAGE_PICKUP_NOTIFICATION';
        case 'PACKAGE_DELIVERED':
          return 'PACKAGE_DELIVERY_NOTIFICATION';
        case 'PACKAGE_PICKUP_FAILED_SENDER_ABSENT':
          return 'PACKAGE_PICKUP_FAILED_SENDER_ABSENT_NOTIFICATION';
        case 'PACKAGE_DELIVERY_FAILED_RECIPIENT_ABSENT':
          return 'PACKAGE_DELIVERY_FAILED_RECIPIENT_ABSENT_NOTIFICATION';
        case 'PACKAGE_RELAY_DROPOFF_REQUIRED':
          return 'PACKAGE_RELAY_DROPOFF_REQUIRED_NOTIFICATION';
        case 'DELIVERY_ROUTE_RESERVED_WARNING':
          return 'DELIVERY_ROUTE_RESERVED_WARNING_NOTIFICATION';
        case 'DELIVERY_ROUTE_CANCELLED_PENALTY':
          return 'DELIVERY_ROUTE_CANCELLED_PENALTY_NOTIFICATION';
        default:
          return 'GENERIC_NOTIFICATION';
      }
    },
    async unregisterLocalNotificationListener() {
      if (!this.localNotificationActionListener) {
        return;
      }
      try {
        const listener = await this.localNotificationActionListener;
        await listener.remove();
      } catch (error) {
        console.warn('Unable to remove local notification listener:', error);
      } finally {
        this.localNotificationActionListener = null;
      }
    },
    registerPushNotificationListeners() {
      if (!isMobileCapacitorRuntime() || this.pushRegistrationListener) {
        return;
      }
      this.pushRegistrationListener = PushNotifications.addListener('registration', (token) => {
        const pushToken = token?.value || '';
        this.mobilePushToken = pushToken;
        if (pushToken) {
          this.storeMobilePushToken(pushToken);
        }
        this.syncMobileDeviceRegistration();
      });
      this.pushRegistrationErrorListener = PushNotifications.addListener('registrationError', (error) => {
        console.warn('Push registration error:', error);
      });
      this.pushNotificationReceivedListener = PushNotifications.addListener('pushNotificationReceived', async (notification) => {
        if (!this.isNativeAppForeground) {
          return;
        }
        const notificationsAllowed = await this.ensureRealtimeLocalNotificationPermission();
        if (!notificationsAllowed) {
          return;
        }
        try {
          const jsonData = notification?.data || {};
          const rawId = jsonData.notificationId || jsonData.id || notification.id;
          if (rawId && this.recentlySeenNotificationIds.includes(String(rawId))) {
            console.info('QuickDelivery: Ignoring duplicate foreground push:', rawId);
            return;
          }
          if (rawId) {
            this.recentlySeenNotificationIds.push(String(rawId));
            setTimeout(() => {
              this.recentlySeenNotificationIds = this.recentlySeenNotificationIds.filter(id => id !== String(rawId));
            }, 10000);
          }

          await this.ensureLocalNotificationChannels();
          const notificationId = (Number(jsonData.notificationId || jsonData.id) || this.nextLocalNotificationId++) % 2147483647;
          
          console.info('QuickDelivery: Scheduling foreground push mirror:', notificationId);
          const targetUrl = this.buildNotificationTargetUrl(jsonData.targetUrl || jsonData.url);
          
          await LocalNotifications.schedule({
            notifications: [
              {
                id: notificationId,
                title: this.getRealtimeNotificationTitle(jsonData),
                body: jsonData.message || notification?.body || notification?.data?.body || '',
                schedule: { at: new Date(Date.now() + 500), allowWhileIdle: true },
                channelId: 'quickdelivery-realtime',
                smallIcon: 'ic_stat_notification',
                importance: 5,
                priority: 2,
                extra: {
                  targetUrl,
                  type: jsonData.type || '',
                  packageReference: jsonData.packageReference || '',
                },
              },
            ],
          });
          console.info('QuickDelivery: Foreground mirror scheduled');
        } catch (error) {
          console.error('QuickDelivery: Error mirroring foreground push:', error);
        }
      });
      this.pushNotificationActionListener = PushNotifications.addListener('pushNotificationActionPerformed', (event) => {
        const targetUrl = event?.notification?.data?.targetUrl || event?.notification?.data?.url || '';
        this.openNotificationTarget(targetUrl);
      });
      this.appStateListener = CapacitorApp.addListener('appStateChange', ({ isActive }) => {
        this.isNativeAppForeground = Boolean(isActive);
        if (isActive) {
          this.syncMobileDeviceRegistration();
        }
      });
    },
    async unregisterPushNotificationListeners() {
      const listeners = [
        this.pushRegistrationListener,
        this.pushRegistrationErrorListener,
        this.pushNotificationReceivedListener,
        this.pushNotificationActionListener,
        this.appStateListener,
      ];
      await Promise.all(listeners.map(async (listenerPromise) => {
        if (!listenerPromise) {
          return;
        }
        try {
          const listener = await listenerPromise;
          await listener.remove();
        } catch (error) {
          console.warn('Unable to remove push/app listener:', error);
        }
      }));
      this.pushRegistrationListener = null;
      this.pushRegistrationErrorListener = null;
      this.pushNotificationReceivedListener = null;
      this.pushNotificationActionListener = null;
      this.appStateListener = null;
    },
    async ensurePushRegistration() {
      if (!isMobileCapacitorRuntime()) {
        return;
      }
      const firebasePushAvailable = await this.checkFirebasePushAvailability();
      if (!firebasePushAvailable) {
        return;
      }
      if (this.pushRegistrationPromise) {
        await this.pushRegistrationPromise;
        return;
      }
      this.pushRegistrationPromise = (async () => {
        try {
          const permissionStatus = await PushNotifications.checkPermissions();
          let receivePermission = permissionStatus.receive;
          if (receivePermission !== 'granted') {
            const permissionRequest = await PushNotifications.requestPermissions();
            receivePermission = permissionRequest.receive;
          }
          if (receivePermission !== 'granted') {
            return;
          }
          await PushNotifications.register();
        } catch (error) {
          console.warn('Unable to register push notifications:', error);
        }
      })();
      try {
        await this.pushRegistrationPromise;
      } finally {
        this.pushRegistrationPromise = null;
      }
    },
    async checkFirebasePushAvailability() {
      if (!isMobileCapacitorRuntime()) {
        return false;
      }
      if (this.firebasePushAvailable !== null) {
        return this.firebasePushAvailable;
      }
      try {
        const result = await FirebaseStatus.isAvailable();
        this.firebasePushAvailable = Boolean(result?.available);
        if (!this.firebasePushAvailable) {
          console.info('Firebase push unavailable on this build, skipping native push registration.');
        }
      } catch (error) {
        this.firebasePushAvailable = false;
        console.warn('Unable to determine Firebase availability, skipping native push registration:', error);
      }
      return this.firebasePushAvailable;
    },
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
      this.activeTrackingPackageReference = '';
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
      if (this.reconnectTimer || !this.shouldMaintainRealtimeConnection()) {
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
      if (this.backgroundWatcherId !== null) {
        BackgroundGeolocation.removeWatcher({ id: this.backgroundWatcherId }).catch((error) => {
          console.warn('Unable to remove background geolocation watcher:', error);
        });
      }
      this.watchId = null;
      this.backgroundWatcherId = null;
      this.lastSentPosition = null;
      this.lastPositionSentAt = 0;
      this.activeTrackingPackageReference = '';
    },
    async refreshActiveTrackingPackage() {
      if (!this.connectedUserId || !hasValidAccessToken()) {
        this.activeTrackingPackageReference = '';
        return;
      }

      try {
        const response = await http.get(
          `${TRACKING_ACTIVE_PACKAGE_ENDPOINT}?deliveryPersonId=${encodeURIComponent(this.connectedUserId)}`,
          { silent: true },
        );
        this.activeTrackingPackageReference = response?.data?.packageReference || '';
      } catch {
        this.activeTrackingPackageReference = '';
      }
    },
    async refreshActiveDeliveryRoute() {
      if (!this.connectedUserId || !hasValidAccessToken()) {
        this.$store.commit('setActiveDeliveryRoute', null);
        return null;
      }
      try {
        const response = await http.get(
          `${this.$i18n.t('rootURL')}${this.$i18n.t('activeDeliveryRouteUrl')}${encodeURIComponent(this.connectedUserId)}`,
          { silent: true },
        );
        const activeRoute = response?.data || null;
        this.$store.commit('setActiveDeliveryRoute', activeRoute);
        return activeRoute;
      } catch {
        this.$store.commit('setActiveDeliveryRoute', null);
        return null;
      }
    },
    async refreshReservationAvailability() {
      if (!this.connectedUserId || !hasValidAccessToken()) {
        this.$store.commit('setReservationAvailability', {
          canReserve: true,
          activeRouteBlocking: false,
          capacityReached: false,
          activeReservations: 0,
          maxReservations: 0,
          reason: 'available',
        });
        return null;
      }
      try {
        const response = await http.get(
          `${this.$i18n.t('rootURL')}${this.$i18n.t('reservationAvailabilityUrl')}${encodeURIComponent(this.connectedUserId)}`,
          { silent: true },
        );
        const reservationAvailability = response?.data || null;
        this.$store.commit('setReservationAvailability', reservationAvailability);
        await this.refreshActiveDeliveryRoute();
        return reservationAvailability;
      } catch {
        this.$store.commit('setReservationAvailability', {
          canReserve: true,
          activeRouteBlocking: false,
          capacityReached: false,
          activeReservations: 0,
          maxReservations: 0,
          reason: 'available',
        });
        return null;
      }
    },
    async refreshOngoingDeliveryStatus() {
      try {
        const response = await http.get(this.$i18n.t('rootURL') + this.$i18n.t('userWithOngoingDelivery') + this.connectedUserId);
        this.isUserWithOngoingDelivery = response.data;
      } catch {
        this.isUserWithOngoingDelivery = false;
      }
      const activeDeliveryRoute = await this.refreshActiveDeliveryRoute();
      this.$store.commit('setOngoingDeliveryState', {
        isUserWithOngoingDelivery: this.isUserWithOngoingDelivery,
        activeDeliveryRoute,
      });
      await this.refreshReservationAvailability();
      await this.refreshActiveTrackingPackage();
      this.startLocationTrackingIfNeeded();
    },
    startLocationTrackingIfNeeded() {
      if (!this.connectedUserId || !this.isUserWithOngoingDelivery) {
        return;
      }
      if (isMobileCapacitorRuntime()) {
        this.startBackgroundLocationTracking();
        return;
      }
      if (this.watchId !== null) {
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
          this.pushTrackingPositionUpdate(newPosition);
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
    async requestTrackingRuntimePermissions() {
      if (!isMobileCapacitorRuntime()) {
        return;
      }

      try {
        await LocalNotifications.requestPermissions();
      } catch (error) {
        console.warn('Unable to request notification permission for background tracking:', error);
      }
    },
    async handleSoftLockEvent(event) {
      if (!this.socketReady) return;
      this.sendSocketMessage({
        type: 'PACKAGE_SOFT_LOCK',
        from: String(event.detail.userId),
        packageId: event.detail.packageId,
        to: 'PACKAGE_SERVICE'
      });
    },
    forwardSoftLockToMap(jsonData) {
      this.$store.commit('patchPackageSoftLock', {
        packageId: jsonData.packageId,
        lockedBy: jsonData.from,
      });
      window.dispatchEvent(new CustomEvent('qd-package-soft-lock-updated', {
        detail: {
          packageId: jsonData.packageId,
          lockedBy: jsonData.from,
        },
      }));
      // Find the map ref and send message
      // Note: HomePage.vue has the ref. We can dispatch a global event or just expect the component to handle it if we broadcast globally.
      // But map expects messages via its window.
      const iframes = document.getElementsByTagName('iframe');
      for (let i = 0; i < iframes.length; i++) {
        if (iframes[i].name === 'map') {
          iframes[i].contentWindow.postMessage(JSON.stringify(jsonData), '*');
        }
      }
    },
    async pushNativeRealtimeNotification(jsonData) {
      if (!isMobileCapacitorRuntime()) {
        return false;
      }

      try {
        const rawId = jsonData.notificationId || jsonData.id;
        if (rawId && this.recentlySeenNotificationIds.includes(String(rawId))) {
          console.info('QuickDelivery: Ignoring duplicate realtime notification:', rawId);
          return true;
        }
        if (rawId) {
          this.recentlySeenNotificationIds.push(String(rawId));
          setTimeout(() => {
            this.recentlySeenNotificationIds = this.recentlySeenNotificationIds.filter(id => id !== String(rawId));
          }, 10000);
        }

        const notificationsAllowed = await this.ensureRealtimeLocalNotificationPermission();
        if (!notificationsAllowed) {
          console.warn('QuickDelivery: Local notification permission NOT granted');
          return false;
        }

        await this.ensureLocalNotificationChannels();
        const notificationId = (this.nextLocalNotificationId++) % 2147483647;
        const targetUrl = this.buildNotificationTargetUrl(jsonData.url);
        
        console.info('QuickDelivery: Scheduling realtime local notification:', notificationId);
        
        await LocalNotifications.schedule({
          notifications: [
            {
              id: notificationId,
              title: this.getRealtimeNotificationTitle(jsonData),
              body: jsonData.message || this.$t('notificationTitle'),
              schedule: { at: new Date(Date.now() + 500), allowWhileIdle: true },
              channelId: 'quickdelivery-realtime',
              smallIcon: 'ic_stat_notification',
              importance: 5,
              priority: 2,
              extra: {
                targetUrl,
                type: jsonData.type || '',
                packageReference: jsonData.packageReference || '',
              },
            },
          ],
        });
        console.info('QuickDelivery: Realtime local notification scheduled successfully');
        return true;
      } catch (error) {
        console.error('QuickDelivery: Unable to publish native realtime notification:', error);
        return false;
      }
    },
    async startBackgroundLocationTracking() {
      if (this.backgroundWatcherId !== null || !this.connectedUserId) {
        return;
      }

      await this.requestTrackingRuntimePermissions();

      try {
        this.backgroundWatcherId = await BackgroundGeolocation.addWatcher(
          {
            requestPermissions: true,
            stale: false,
            distanceFilter: POSITION_UPDATE_MIN_DISTANCE_METERS,
            backgroundMessage: this.$t('backgroundTrackingMessage'),
            backgroundTitle: this.$t('backgroundTrackingTitle'),
          },
          async (location, error) => {
            if (error) {
              console.error('Background geolocation error:', error);
              return;
            }
            if (!location) {
              return;
            }

            const newPosition = {
              latitude: location.latitude,
              longitude: location.longitude,
            };

            if (!this.shouldSendPositionUpdate(newPosition)) {
              return;
            }

            this.lastSentPosition = newPosition;
            this.lastPositionSentAt = Date.now();
            await this.pushTrackingPositionUpdate(newPosition);
          }
        );
      } catch (error) {
        console.error('Unable to start background geolocation watcher:', error);
      }
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
    registerRealtimeNotificationSession() {
      if (!this.connectedUserId) {
        return;
      }
      this.sendSocketMessage({
        type: 'REGISTER_NOTIFICATION_SESSION',
        from: String(this.connectedUserId),
        to: 'PACKAGE_SERVICE',
      });
    },
    getOrCreateMobileDeviceId() {
      if (typeof window === 'undefined') {
        return '';
      }
      const existingId = window.localStorage.getItem(MOBILE_DEVICE_STORAGE_KEY);
      if (existingId) {
        return existingId;
      }
      const generatedId = window.crypto?.randomUUID?.() || `qd-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
      window.localStorage.setItem(MOBILE_DEVICE_STORAGE_KEY, generatedId);
      return generatedId;
    },
    getStoredMobilePushToken() {
      if (typeof window === 'undefined') {
        return '';
      }
      try {
        return window.localStorage.getItem(MOBILE_PUSH_TOKEN_STORAGE_KEY) || '';
      } catch {
        return '';
      }
    },
    storeMobilePushToken(pushToken) {
      if (typeof window === 'undefined' || !pushToken) {
        return;
      }
      try {
        window.localStorage.setItem(MOBILE_PUSH_TOKEN_STORAGE_KEY, pushToken);
      } catch {
        // Ignore storage errors; the token will still be posted for this session.
      }
    },
    resolveMobilePlatform() {
      const platform = Capacitor.getPlatform?.();
      if (platform === 'android') {
        return 'ANDROID';
      }
      if (platform === 'ios') {
        return 'IOS';
      }
      return 'WEB';
    },
    async resolveNearbyCourierLocation() {
      if (!isMobileCapacitorRuntime() || !this.isNearbyCourierRole) {
        return null;
      }
      try {
        const permissionStatus = await Geolocation.checkPermissions();
        let locationPermission = permissionStatus.location || permissionStatus.coarseLocation;
        if (locationPermission !== 'granted') {
          const requestedPermissions = await Geolocation.requestPermissions();
          locationPermission = requestedPermissions.location || requestedPermissions.coarseLocation;
        }
        if (locationPermission !== 'granted') {
          return null;
        }
        const position = await Geolocation.getCurrentPosition({
          enableHighAccuracy: true,
          timeout: 10000,
          maximumAge: 60000,
        });
        return {
          latitude: position?.coords?.latitude ?? null,
          longitude: position?.coords?.longitude ?? null,
        };
      } catch (error) {
        console.warn('Unable to resolve nearby courier location:', error);
        return null;
      }
    },
    async syncMobileDeviceRegistration() {
      if (!isMobileCapacitorRuntime() || !this.connectedUserId || !hasValidAccessToken()) {
        return;
      }
      await this.ensurePushRegistration();
      const currentLocation = await this.resolveNearbyCourierLocation();
      const pushToken = this.mobilePushToken || this.getStoredMobilePushToken();
      if (pushToken && !this.mobilePushToken) {
        this.mobilePushToken = pushToken;
      }
      try {
        await http.post(`${this.$i18n.t('rootURL')}devices/register`, {
          userId: this.connectedUserId,
          deviceId: this.getOrCreateMobileDeviceId(),
          pushToken: pushToken || '',
          locale: this.resolveCurrentLocaleCode(),
          platform: this.resolveMobilePlatform(),
          active: true,
          latitude: currentLocation?.latitude,
          longitude: currentLocation?.longitude,
        }, { silent: true });
      } catch (error) {
        console.warn('Unable to register mobile device for notifications:', error);
      }
    },
    async syncNotificationLocalePreference() {
      if (!this.connectedUserId || !hasValidAccessToken()) {
        return;
      }
      try {
        await http.post(
          `${this.$i18n.t('rootURL')}notifications/preferences?userId=${encodeURIComponent(this.connectedUserId)}&locale=${encodeURIComponent(this.resolveCurrentLocaleCode())}`,
          null,
          { silent: true },
        );
      } catch (error) {
        console.warn('Unable to synchronize notification locale preference:', error);
      }
    },
    async syncNotificationsFromBackend() {
      if (!this.connectedUserId || !hasValidAccessToken()) {
        this.$store.commit('setNotifications', []);
        return;
      }
      try {
        const response = await http.get(`${this.$i18n.t('rootURL')}notifications?userId=${encodeURIComponent(this.connectedUserId)}`, { silent: true });
        const backendNotifications = Array.isArray(response?.data) ? response.data.map((notification) => ({
          id: notification.id,
          type: this.mapBackendEventTypeToFrontendType(notification.eventType),
          title: notification.title || '',
          message: notification.body || '',
          url: notification.targetUrl || '',
          payloadJson: notification.payloadJson || '',
          receivedAt: notification.createdAt || new Date().toISOString(),
          read: Boolean(notification.read),
        })) : [];
        this.$store.commit('setNotifications', backendNotifications);
      } catch (error) {
        console.warn('Unable to synchronize notifications from backend:', error);
      }
    },
    handleTrackingSubscriptionEvent(event) {
      const packageReference = event?.detail?.packageReference;
      const guestAccessToken = event?.detail?.guestAccessToken || '';
      if (!packageReference) {
        return;
      }
      this.trackingSubscriptions[packageReference] = guestAccessToken;
      this.syncRealtimeConnection();
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
      this.syncRealtimeConnection();
    },
    shouldMaintainRealtimeConnection() {
      const publicRouteAllowsRealtime = this.$route.name === 'PackageTrackingPage'
        || this.$route.name === 'PackageTrackingSummaryPage';
      if (this.$route.meta?.public && !publicRouteAllowsRealtime) {
        return false;
      }
      return Boolean(this.connectedUserId) || Object.keys(this.trackingSubscriptions).length > 0;
    },
    syncRealtimeConnection() {
      if (this.shouldMaintainRealtimeConnection()) {
        this.initializeRealtime();
        return;
      }
      this.destroyRealtime();
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
    normalizeNearbyCoordinate(value) {
      return Number.parseFloat(Number(value).toFixed(4));
    },
    async recoverNearbyPackageNotifications() {
      return;
    },
    async initializeRealtime() {
      if (this.socket || !this.shouldMaintainRealtimeConnection()) {
        return;
      }

      this.socket = new WebSocket(this.$i18n.t('wsURL'));
      this.socket.onopen = () => {
        this.socketReady = true;
        this.registerRealtimeNotificationSession();
        this.syncMobileDeviceRegistration();
        this.recoverNearbyPackageNotifications();
        this.flushTrackingSubscriptions();
        if (this.connectedUserId && hasValidAccessToken()) {
          this.refreshOngoingDeliveryStatus();
        }
      };

      this.socket.onmessage = (event) => {
        const jsonData = JSON.parse(event.data);
        const isTargetedNotification = typeof jsonData?.type === 'string'
          && jsonData.type.endsWith('_NOTIFICATION')
          && this.connectedUserId === parseInt(jsonData.to, 10);

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
        this.socketReady = false;
        this.stopLocationTracking();
        this.socket = null;
        this.scheduleRealtimeReconnect();
      };

      this.socket.onerror = (error) => {
        console.error('WebSocket error:', error);
      };
    },
    async pushTrackingPositionUpdate(newPosition) {
      if (!this.connectedUserId || !newPosition) {
        return;
      }

      const url = this.activeTrackingPackageReference
        ? `${TRACKING_PACKAGE_POSITION_ENDPOINT}?packageReference=${encodeURIComponent(this.activeTrackingPackageReference)}`
        : `${TRACKING_POSITION_ENDPOINT}?deliveryPersonId=${encodeURIComponent(this.connectedUserId)}`;
      const payload = {
        latitude: newPosition.latitude,
        longitude: newPosition.longitude,
      };

      try {
        if (isMobileCapacitorRuntime()) {
          const headers = {
            'Content-Type': 'application/json',
          };
          const accessToken = getAccessToken();
          if (accessToken) {
            headers.Authorization = `Bearer ${accessToken}`;
          }
          await CapacitorHttp.post({
            url,
            headers,
            data: payload,
          });
          return;
        }

        await http.post(url, payload, { silent: true });
      } catch (error) {
        console.error('Unable to push tracking position update:', error);
      }
    },
    showRealtimeNotification(jsonData) {
      this.$store.commit('updateShowMessage', true);
      this.$store.commit('updateRequestSuccess', true);
      this.$store.commit('updateRequestMessage', jsonData.message || this.$t('notificationTitle'));
      setTimeout(() => {
        this.$store.commit('updateShowMessage', false);
      }, 9000);

      const openTarget = () => {
        this.openNotificationTarget(jsonData.url);
      };

      if (isMobileCapacitorRuntime()) {
        console.info('QuickDelivery: triggering native system notification for', jsonData.type);
        void this.pushNativeRealtimeNotification(jsonData);
        return;
      }

      if (Notification.permission === 'granted') {
        const notification = new Notification(this.getRealtimeNotificationTitle(jsonData), {
          body: jsonData.message || this.$t('notificationTitle')
        });
        notification.onclick = openTarget;
      } else if (Notification.permission !== 'denied') {
        Notification.requestPermission().then((permission) => {
          if (permission === 'granted') {
            const notification = new Notification(this.getRealtimeNotificationTitle(jsonData), {
              body: jsonData.message || this.$t('notificationTitle')
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
@supports (padding-top: env(safe-area-inset-top)) {
  .fullPage {
    padding-left: env(safe-area-inset-left, 0px);
    padding-right: env(safe-area-inset-right, 0px);
    padding-bottom: env(safe-area-inset-bottom, 0px);
  }
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
    width: 40px;
    height: 40px;
    right: max(10px, env(safe-area-inset-right, 0px) + 8px);
    bottom: max(12px, env(safe-area-inset-bottom, 0px) + 8px);
  }
  .global-scroll-top .material-symbols-outlined {
    font-size: 20px;
  }
}
</style>
