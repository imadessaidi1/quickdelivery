<template>
  <div class="marker-details">
    <!-- Zone gauche avec la photo -->
    <div class="left-section">
      <img :src="getImageSrc()" alt="Image" />
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
    package_: Object,
  },
  methods: {
    handleButton1() {
    var stringDeparture="";
    var stringArrival="";
    this.package_.addresses.forEach(address =>{
      if(address.type==="DEPARTURE"){
        stringDeparture = address.latitude+","+address.longitude;
      }else{
        stringArrival = address.latitude+","+address.longitude;
      }
    });
      this.$parent.$parent.$refs.mapVue.$refs.map.contentWindow.postMessage("SelectedDirection:"+stringDeparture+";"+stringArrival, "*");
    },
    handleButton2() {
      // Logique du bouton 2
      console.log('Button 2 clicked');
    },
   getImageSrc() {
      let imgSrc = '';
      if (this.package_.files && this.package_.files[0]) {
        imgSrc = `data:image/png;base64,${this.package_.files[0].data}`;
      }
      return imgSrc;
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
