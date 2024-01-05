<template>
  <div class="fullPage">
    <SearchBar />
    <router-view/>
    <AppFooter />
  </div>
</template>

<script>
import SearchBar from './components/SearchBar.vue';
import AppFooter from './components/AppFooter.vue';

export default {
  components: {
    SearchBar,
    AppFooter,
  },
};
</script>

<style>
.fullPage{
  height: 100%;
}
</style>
