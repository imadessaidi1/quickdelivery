<template>
  <div class="marker-details">
    <!-- Zone gauche avec la photo -->
    <div class="left-section">
      PHOTO
    </div>

    <!-- Zone droite avec le texte -->
    <div class="right-section">
      <h3>{{ package_.id }}</h3>
      <p>{{$t('packageHeight')}}:
        {{package_.height}}</p>
      <p>{{$t('packageWidth')}}:
        {{package_.width}} </p>
      <p>{{$t('packageDepth')}}:
        {{package_.dept}}</p>
      <p>{{$t('packageWeight')}}:
        {{package_.weight}}</p>
      <p>{{$t('packagePrice')}}:
        {{package_.price}}</p>
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
