<template>
  <div class="fullPage">
    <loading v-model:active="isLoading"
             :can-cancel="true"
             :is-full-page="true"/>
    <SearchBar />
    <router-view/>
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

export default {
  computed: {
    isLoading() {
      return this.$store.state.isLoading;
    },
  },
  mounted() {
    const socket = new WebSocket('ws://localhost:8082/ws');
    socket.onopen = () => {
        console.log('WebSocket connected');
    };

    socket.onmessage = (event) => {
        const jsonData = JSON.parse(event.data);
        const connectedUser = this.$store.state.connectedUser
        if(connectedUser.type === 'DELIVERY_PERSON' && connectedUser.id === parseInt(jsonData.to)){
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
  height: 100%;
}
</style>
