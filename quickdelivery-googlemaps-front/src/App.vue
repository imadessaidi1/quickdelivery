<template>
  <div class="fullPage">
    <loading v-model:active="isLoading"
             :can-cancel="true"
             :is-full-page="true"/>
    <SearchBar />
    <div class="router-view">
      <router-view/>
    </div>
    <AppMessages />
    <AppFooter />
  </div>
</template>

<script>
import SearchBar from './components/SearchBar.vue';
import AppFooter from './components/AppFooter.vue';
import AppMessages from './components/RequestMessage.vue';
import Loading from 'vue-loading-overlay';
import 'vue-loading-overlay/dist/css/index.css';
import http from '@/config/httpInterceptor';

export default {
  computed: {
    isLoading() {
      return this.$store.state.isLoading;
    },
  },
  data() {
    return {
      isUserWithOngoingDelivery: false,
    };
  },
  async mounted() {
    await http.get(this.$i18n.t('rootURL') + this.$i18n.t('userWithOngoingDelivery')+this.$store.state.connectedUser.id)
      .then(response => {
        this.isUserWithOngoingDelivery = response.data;
    }).catch(() => {
      console.log("unable to process your request this time. please try again latter.");
    });
    const socket = new WebSocket(this.$i18n.t('wsURL'));
    socket.onopen = () => {
        console.log('WebSocket connected');
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
                        console.log(newPosition);
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

    socket.onclose = () => {
        console.log('WebSocket closed');
    };

    socket.onerror = (error) => {
        console.error('WebSocket error:', error);
    };
  },
  components: {
    SearchBar,
    AppFooter,
    Loading,
    AppMessages,
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
}
</style>
