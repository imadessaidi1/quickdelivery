<template>
    <div class="main-content">
        <MarkerList :style="{ width: '30%' }"/>
        <GoogleMap :style="{ width: '60%' }"/>
    </div>
</template>
<script>
import GoogleMap from '../components/GoogleMap.vue';
import MarkerList from '../components/MarkerList.vue';

export default {
  components: {
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