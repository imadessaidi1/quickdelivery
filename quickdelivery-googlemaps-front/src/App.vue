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
