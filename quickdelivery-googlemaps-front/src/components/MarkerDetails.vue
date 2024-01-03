<template>
  <div class="marker-details">
    <!-- Zone gauche avec la photo -->
    <div class="left-section">
      <img src="" alt="Image">
      <!-- <img :src="getImageSrc()" alt="Image" /> -->
    </div>

    <!-- Zone droite avec le texte -->
    <div class="right-section">
      <h3>{{ package_.id }}</h3>
      <p>{{ $t('packageHeight') }}:
        {{ package_.height }}</p>
      <p>{{ $t('packageWidth') }}:
        {{ package_.width }} </p>
      <p>{{ $t('packageDepth') }}:
        {{ package_.dept }}</p>
      <p>{{ $t('packageWeight') }}:
        {{ package_.weight }}</p>
      <p>{{ $t('packagePrice') }}:
        {{ package_.price }}</p>
      <!-- Zone inférieure avec des boutons -->
      <div class="bottom-section">
        <button class="primary_btn" ref="direction" :key="package_.id"
          @click="showDirection">{{ $t('packagesArroundMArkerDetailActionsShowDirection') }}</button>
        <button class="primary_btn" ref="onMyRoad" :key="package_.id"
          @click="onMyDirection">{{ $t('packagesArroundMArkerDetailActionsShowPackagesOnMyDirection') }}</button>
        <button class="primary_btn" ref="reserveButtons" :key="package_.id"
          @click="reserve">{{ $t('packagesArroundMArkerDetailActionsReserve') }}</button>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  props: {
    package_: Object,
  },
  methods: {
    showDirection() {
      var stringDeparture = "";
      var stringArrival = "";
      this.package_.addresses.forEach(address => {
        if (address.type === "DEPARTURE") {
          stringDeparture = address.latitude + "," + address.longitude;
        } else {
          stringArrival = address.latitude + "," + address.longitude;
        }
      });
      this.$parent.$parent.$refs.mapVue.$refs.map.contentWindow.postMessage("SelectedDirection:" + stringDeparture + ";" + stringArrival, "*");
    },
    onMyDirection() {
      var stringDeparture = "";
      var stringArrival = "";
      this.package_.addresses.forEach(address => {
        if (address.type === "DEPARTURE") {
          stringDeparture = address.latitude + "," + address.longitude;
        } else {
          stringArrival = address.latitude + "," + address.longitude;
        }
      });
      this.$parent.$parent.$refs.mapVue.$refs.map.contentWindow.postMessage("OnMyDirection:" + stringDeparture + ";" + stringArrival, "*");
    },
    reserve() {
      const url = this.$i18n.t('rootURL') + this.$i18n.t('reservePackageUrl') + "packageID=" + this.package_.id + "&deliveryPersonID=" + this.$store.state.connectedUser.id;
      console.log(url);
      return axios.put(url)
        .then(response => {
          return response.data;
        }).catch(() => {
          console.log("unable to process your request this time. please try again latter.");
        });
    },
    getImageSrc() {
      let imgSrc = '';
      if (this.package_.files && this.package_.files[0]) {
        imgSrc = `data:image/png;base64,${this.package_.files[0].data}`;
      }
      return imgSrc;
    },
    destinationAddress() {
      var destinationAddressS = '';
      this.package_.addresses.forEach(address => {
        if(address.type === 'ARRIVAL'){
          destinationAddressS = address.line1+" "+address.zipCode+" "+address.town+" "+address.country;
        }
      });
      return destinationAddressS;
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
  flex-direction: row;
  justify-content: space-between;
  padding: 10px;
  margin-bottom: 10px;
  border: 1px solid #ccc;
  border-radius: 5px;
}

.left-section {
  width: 35%;
  text-align: center;
  background-color: #ccc;
  border-radius: 5px;
}

.left-section img {
  height: auto;
}

.right-section {
  width: 60%;
}
.right-section h3{
  margin: 0 0 10px 0;
}
.right-section p {
  margin: 5px 0;
  font-size: 12px;
}

.bottom-section {
  display: flex;
  flex-direction: column;
  width: 75%;
  margin: 10px auto;
}

.bottom-section button {
  font-size: 11px;
  padding: 5px 0;
  margin: 3px 0;
}
</style>
