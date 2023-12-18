<template>
  <div class="marker-list">
    <h2>Liste des Colis autour de vous</h2>
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
           };
        },
};
</script>

<style scoped>
.marker-list ul{
  width: 100%;
  flex: 1;
  padding: 10px;
  max-height: 530px;
  overflow-y: auto;
}
ul{
 list-style: none;
 padding: 0;
}
</style>
