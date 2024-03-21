<template>
  <div class="marker-details">
    <!-- Zone droite avec le texte -->
      <h3>{{$t('packageReference')}} : {{ package_.reference }}</h3>
      <!--<p>{{ $t('packageHeight') }}:
        {{ package_.height }}</p>
      <p>{{ $t('packageWidth') }}:
        {{ package_.width }} </p>
      <p>{{ $t('packageDepth') }}:
        {{ package_.dept }}</p>
      <p>{{ $t('packageWeight') }}:
        {{ package_.weight }}</p>-->
      <p>{{ $t('packagePrice') }}:
        {{ package_.deliveryPrice }}&nbsp;{{ $t('currency') }}</p>
    <p>{{$t('packageDeparture')}}:
      {{departureAddress()}}</p>
    <p>{{$t('packageDestination')}}:
        {{destinationAddress()}}</p>
      <p>{{$t('packageDistanceToDestination')}}:
        {{package_.distanceToDestination}}</p>
      <p>{{$t('packagesArroundDistanceFromYou')}}:
        {{package_.fromYou}}</p>
      <!-- Zone inférieure avec des boutons -->
      <p>
        <button class="btn primary_btn" ref="onMyRoad"
          @click="onMyDirection">{{ $t('packagesArroundMArkerDetailActionsShowPackagesOnMyDirection') }}</button>&nbsp;
        <button class="btn primary_btn" ref="reserveButtons"
          @click="details">{{ $t('packagesArroundMArkerDetailActionsDetails') }}</button>&nbsp;
        <button class="btn primary_btn" ref="detailsButtons"
          @click="reserve">{{ $t('packagesArroundMArkerDetailActionsReserve') }}</button>
      </p>
  </div>
</template>

<script>
import http from '@/config/httpInterceptor';
export default {
  props: {
    package_: Object,
    mapVue: Object,
    modal: Object,
  },
  methods: {
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
      var message = "OnMyDirection:" + stringDeparture + ";" + stringArrival;
      this.mapVue.$refs.map.contentWindow.postMessage(message, "*");
    },
    async reserve() {
      const userLanguage = navigator.languages && navigator.languages.length ? navigator.languages[0] : navigator.language || 'fr-FR';
      const url = this.$i18n.t('rootURL') + this.$i18n.t('reservePackageUrl') + "packageID=" + this.package_.id + "&deliveryPersonID=" + this.$store.state.connectedUser.id+"&locale="+userLanguage;
      return new Promise((resolve, reject) => {
        http.put(url)
        .then(response => {
          this.mapVue.$refs.map.contentWindow.postMessage("RefreshPackagesList", "*");
          resolve(response.data);
        })
        .catch(error => {
          console.log("Unable to process your request at this time. Please try again later.", error);
          reject(error);
        });
      });
    },
    details(){
      this.$store.commit('updatePackage', this.package_);
      this.modal.openModal();
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
    departureAddress() {
      var destinationAddressS = '';
      this.package_.addresses.forEach(address => {
        if(address.type === 'DEPARTURE'){
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
};
</script>

<style>
.marker-details {
  justify-content: space-between;
  padding: 10px;
  margin-bottom: 10px;
  border: 1px solid #ccc;
  border-radius: 5px;
  background: linear-gradient(0.25turn, #ffffff37, #ffffffcb, #ffffff, #ffffffdb), no-repeat url('../assets/box 2.png') left -100px bottom 50%;
  background-size: contain;
  opacity: 0.8;
  /*background: rgba(255, 255, 255, 0.6);*/
}
.marker-details p {
  margin: 5px 0;
  font-size: 12px;
}
</style>
