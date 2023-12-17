<template>
  <div class="marker-list">
    <h2>Liste des Marqueurs</h2>
    <ul>
      <li v-for="package_ in packagesList" :key="package_.id">

        <!-- Utilisez le composant MarkerDetails pour chaque marqueur -->
        <MarkerDetails :package_="package_" />
      </li>
    </ul>
  </div>
</template>

<script>
import MarkerDetails from './MarkerDetails.vue';
export default {
  components: {
    MarkerDetails,
  },
  data() {
    return {
      packagesList: [],
    };
  },
   mounted() {
        window.onmessage = (e) => {
            const rawData = e.data;
            this.packagesList = JSON.parse(JSON.stringify(rawData));
            console.log(this.packagesList);
           };
        },
};
</script>

<style scoped>
.marker-list {
  flex: 1;
  padding: 10px;
}
</style>
