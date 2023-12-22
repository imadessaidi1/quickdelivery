<template>
  <div class="marker-details">
    <!-- Zone gauche avec la photo -->
    <div class="left-section">
      PHOTO
    </div>

    <!-- Zone droite avec le texte -->
    <div class="right-section">
      <h3>{{ package_.id }}</h3>
      <p><label for="height">{{$t('packageHeight')}}:</label>
        <label for="height">{{package_.height}}</label></p>
      <p><label for="width">{{$t('packageWidth')}}:</label>
        <label for="width">{{package_.width}} </label></p>
      <p><label for="depth">{{$t('packageDepth')}}:</label>
        <label for="depth">{{package_.dept}}</label></p>
      <p><label for="weight">{{$t('packageWeight')}}:</label>
        <label for="weight">{{package_.weight}}</label></p>
      <p><label for="price">{{$t('packagePrice')}}:</label>
        <label for="weight">{{package_.price}}</label></p>
    </div>

    <!-- Zone inférieure avec des boutons -->
    <div class="bottom-section">
      <button ref="reserveButtons" :key="package_.id" @click="handleButton1">Reserve</button>
      <button @click="handleButton2">Bouton 2</button>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    package_: Object, // Propriété pour recevoir les détails du marqueur
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
    setFocusOnReserveButton() {
          const reserveButton = this.$refs.reserveButtons;
          if (reserveButton) {
            reserveButton.focus();
          }
        },
  },
  mounted() {

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
