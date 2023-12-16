<template>
  <div>
    <SearchBar />
    <div class="main-content">
      <MarkerList />
      <GoogleMap />

    </div>
  </div>
</template>

<script>
import SearchBar from './components/SearchBar.vue';
import GoogleMap from './components/GoogleMap.vue';
import MarkerList from './components/MarkerList.vue';

export default {
  components: {
    SearchBar,
    GoogleMap,
    MarkerList,
  },
};
</script>

<style>
.main-content {
  display: flex;
}
</style>
