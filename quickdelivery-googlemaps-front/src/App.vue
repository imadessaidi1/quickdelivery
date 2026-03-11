<template>
  <div class="fullPage">
    <loading v-model:active="isLoading"
             :can-cancel="true"
             :is-full-page="true"/>
    <SearchBar v-if="showSearchBar" />
    <div class="router-view">
      <router-view/>
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
      return !this.$route.meta?.public;
    },
  },
  data() {
    return {
      isUserWithOngoingDelivery: false,
    };
  },
  async mounted() {
    if (!hasValidAccessToken()) {
      return;
    }

    await http.get(this.$i18n.t('rootURL') + this.$i18n.t('userWithOngoingDelivery')+this.$store.state.connectedUser.id)
      .then(response => {
        this.isUserWithOngoingDelivery = response.data;
    }).catch(() => {
      console.error("Unable to process your request this time. Please try again later.");
    });
    const socket = new WebSocket(this.$i18n.t('wsURL'));
    socket.onopen = () => {
        if (navigator.geolocation && this.isUserWithOngoingDelivery) {
            navigator.geolocation.watchPosition(
                (position) => {
                    const newPosition = {
                        latitude: position.coords.latitude,
                        longitude: position.coords.longitude
                    };

                    const updateMessage = {
                         type: 'PACKAGE_POSITION_UPDATE',
                         from: this.$store.state.connectedUser.id,
                         to: 'PACKAGE_SERVICE',
                         message:'PACKAGE_POSITION_UPDATE',
                         positionDTO: newPosition,
                         url: ''
                    };
                     setTimeout(() => {
                        socket.send(JSON.stringify(updateMessage));
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

    socket.onmessage = (event) => {
        const jsonData = JSON.parse(event.data);
        const connectedUser = this.$store.state.connectedUser
        if((jsonData.type === 'NEW_PACKAGE_NOTIFICATION' || jsonData.type === 'PACKAGE_RESERVATION_OTP_NOTIFICATION') &&
        (connectedUser.type === 'DELIVERY_PERSON' && connectedUser.id === parseInt(jsonData.to))){
            if (Notification.permission === 'granted') {
                const notification = new Notification(this.$t('notificationTitle'), {
                    body: jsonData.message
                });
                notification.onclick = function() {
                  window.location.href = jsonData.url;
                };
            } else if (Notification.permission !== 'denied') {
                Notification.requestPermission().then((permission) => {
                    if (permission === 'granted') {
                        const notification = new Notification(this.$t('notificationTitle'), {
                            body: jsonData.message
                        });
                        notification.onclick = function() {
                          window.location.href = jsonData.url;
                        };
                    }
                });
            }
        }else if(jsonData.type === 'PACKAGE_POSITION_UPDATE'){
            this.$store.commit('updatePackageLastPosition', JSON.parse(jsonData.message));
        }
    };

    socket.onclose = () => {};

    socket.onerror = (error) => {
        console.error('WebSocket error:', error);
    };
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
