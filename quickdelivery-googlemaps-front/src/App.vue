<template>
  <div>
    <SearchBar />
    <div class="main-content">
      <MarkerList :style="{ width: '30%' }"/>
      <GoogleMap :style="{ width: '60%' }"/>
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
