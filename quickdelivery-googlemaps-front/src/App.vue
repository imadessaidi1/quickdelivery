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
  },
  data() {
    return {
      isUserWithOngoingDelivery: false,
      socket: null,
    };
  },
  watch: {
    connectedUserId: {
      immediate: true,
      handler() {
        this.initializeRealtime();
      },
    },
  },
  beforeUnmount() {
    if (this.socket) {
      this.socket.close();
      this.socket = null;
    }
  },
  methods: {
    async initializeRealtime() {
      if (!hasValidAccessToken() || !this.connectedUserId || this.socket) {
        return;
      }

      console.info('QuickDelivery WS initializing for user:', this.connectedUserId, 'url:', this.$i18n.t('wsURL'));

      http.get(this.$i18n.t('rootURL') + this.$i18n.t('userWithOngoingDelivery') + this.connectedUserId)
        .then(response => {
          this.isUserWithOngoingDelivery = response.data;
          console.info('QuickDelivery WS ongoing delivery status:', this.isUserWithOngoingDelivery);
        }).catch(() => {
          this.isUserWithOngoingDelivery = false;
        });

      this.socket = new WebSocket(this.$i18n.t('wsURL'));
      this.socket.onopen = () => {
        console.info('QuickDelivery WS connected');
        if (navigator.geolocation && this.isUserWithOngoingDelivery) {
            navigator.geolocation.watchPosition(
                (position) => {
                    const newPosition = {
                        latitude: position.coords.latitude,
                        longitude: position.coords.longitude
                    };

                    const updateMessage = {
                         type: 'PACKAGE_POSITION_UPDATE',
                         from: this.connectedUserId,
                         to: 'PACKAGE_SERVICE',
                         message:'PACKAGE_POSITION_UPDATE',
                         positionDTO: newPosition,
                         url: ''
                    };
                     setTimeout(() => {
                        if (this.socket?.readyState === WebSocket.OPEN) {
                          console.info('QuickDelivery WS send:', updateMessage);
                          this.socket.send(JSON.stringify(updateMessage));
                        }
                      }, 9000);

                },
                (error) => {
                    console.error('Error getting location:', error);
                }
            );
        } else {
            console.error('Geolocation is not supported by this browser.');
        }
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
            this.showRealtimeNotification(jsonData);
        } else if(jsonData.type === 'PACKAGE_POSITION_UPDATE'){
            this.$store.commit('updatePackageLastPosition', JSON.parse(jsonData.message));
        }
      };

      this.socket.onclose = () => {
        console.info('QuickDelivery WS closed');
        this.socket = null;
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
  height: 100vh;
}
.router-view{
  flex-grow: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
</style>
