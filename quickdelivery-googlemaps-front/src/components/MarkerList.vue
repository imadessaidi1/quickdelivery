<template>
  <div class="marker-list">
    <h2>Liste des Marqueurs</h2>
    <ul>
      <li v-for="marker in markers" :key="marker.id">
        <!-- Utilisez le composant MarkerDetails pour chaque marqueur -->
        <MarkerDetails :marker="marker" />
      </li>
      <li v-for="marker in markers" :key="marker.id">
              <!-- Utilisez le composant MarkerDetails pour chaque marqueur -->
              <MarkerDetails :marker="marker" />
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
      markers: [
        { id: 1, title: 'Marqueur 1', description: 'Description du marqueur 1', photo: 'url_de_la_photo1.jpg' },
        // Ajoutez plus de marqueurs selon vos besoins
      ],
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
