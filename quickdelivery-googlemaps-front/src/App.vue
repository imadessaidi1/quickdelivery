<template>
  <div>
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
  methods: {

    },
};
</script>

<style>
.main-content {
  display: flex;
}
</style>
