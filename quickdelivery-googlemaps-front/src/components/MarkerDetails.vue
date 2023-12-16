<template>
  <div class="marker-details">
    <!-- Zone gauche avec la photo -->
    <div class="left-section">
      <img :src="marker.photo" alt="Marqueur Photo" />
    </div>

    <!-- Zone droite avec le texte -->
    <div class="right-section">
      <h3>{{ marker.title }}</h3>
      <p>{{ marker.description }}</p>
    </div>

    <!-- Zone inférieure avec des boutons -->
    <div class="bottom-section">
      <button @click="handleButton1">Bouton 1</button>
      <button @click="handleButton2">Bouton 2</button>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    marker: Object, // Propriété pour recevoir les détails du marqueur
  },
  methods: {
    handleButton1() {
      // Logique du bouton 1
      console.log('Button 1 clicked');
    },
    handleButton2() {
      // Logique du bouton 2
      console.log('Button 2 clicked');
    },
  },
};
</script>

<style scoped>
.marker-details {
  display: flex;
  flex-direction: column;
  padding: 10px;
  border: 1px solid #ccc;
  border-radius: 5px;
}

.left-section {
  flex: 1;
  margin-right: 10px;
}

.left-section img {
  width: 100%;
  height: auto;
}

.right-section {
  flex: 1;
}

.bottom-section {
  display: flex;
  justify-content: space-between;
  margin-top: 10px;
}
</style>
