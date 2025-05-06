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
                            <!--MOBILE MARKER STRUCTUR-->
  <div class="mobile-marker-details">
      <div class="details_container">
        <div class="package_image">
          <img src="../assets/box.png" alt="">
        </div>
        <div class="package_info">
          <p><b>{{ $t('packageVolume') }}:</b> {{ package_.width * package_.height * package_.depth }}</p>
          <p><b>{{ $t('packageWeight') }}:</b> {{ package_.weight }}</p>
          <p><b>{{ $t('packagePrice') }}:</b> {{ package_.deliveryPrice }}</p>
        </div>
      </div>
      <!-- Zone inférieure avec des boutons -->
      <div class="marker-details-buttons">
          <button class="icon_btn primary_btn material-symbols-outlined" ref="onMyRoad"
            @click="onMyDirection">timeline</button>
          <button class="icon_btn warning_btn material-symbols-outlined" ref="reserveButtons"
            @click="details">add</button>
          <button class="icon_btn confirm_btn material-symbols-outlined" ref="detailsButtons"
            @click="reserve">home_pin</button>
      </div>
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
      window.top.postMessage("RefreshPackagesList "+this.package_.id, "*");
      //this.mapVue.$refs.map.contentWindow.postMessage("RefreshPackagesList", "*");
      return new Promise((resolve, reject) => {
        http.put(url)
        .then(response => {
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
          destinationAddressS = address.addressAuto;
        }
      });
      return destinationAddressS;
    },
    departureAddress() {
      var destinationAddressS = '';
      this.package_.addresses.forEach(address => {
        if(address.type === 'DEPARTURE'){
          destinationAddressS = address.addressAuto;
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
  /*display: none;*/
  justify-content: space-between;
  padding: 10px;
  margin-bottom: 10px;
  border: 1px solid #ccc;
  border-radius: 5px;
  background: linear-gradient(0.25turn, #ffffff37, #ffffffcb, #ffffff, #ffffffdb), no-repeat url('../assets/box 2.png') left -100px bottom 50%;
  background-size: contain;
  opacity: 0.8;
  cursor: pointer;
  /*background: rgba(255, 255, 255, 0.6);*/
}
.marker-details p {
  margin: 5px 0;
  font-size: 12px;
}
/** MOBILE MARKER STYLE */

.mobile-marker-details {
  display: none;
  width: max-content;
  padding: 10px;
  margin-bottom: 10px;
  border: 1px solid rgb(249, 206, 111);
  border-radius: 17px;
  background: linear-gradient(0.25turn, #ffffff37, #ffffffcb, #ffffff, #ffffffdb);
  background-size: contain;
  box-shadow: 0 8px 30px rgb(0,0,0,0.12);
  opacity: 0.8;
  cursor: pointer;
}
.mobile-marker-details .details_container{
  display: flex;
  justify-content: space-evenly;
  align-items: center;
  width: 240px;
  margin: 0 auto;
}
.mobile-marker-details .details_container .package_info{
  width: 130px;
  padding: 0 0 0 20px;
}
.details_container .package_image{
  width: 75px;
}
.details_container .package_image img{
  width: 100%;
}
.mobile-marker-details .package_info p {
  margin: 5px 0;
  font-size: 13px;
  text-align: left;
}
.mobile-marker-details .marker-details-buttons{
  width: 180px;
  display: flex;
  justify-content: space-evenly;
  margin: 15px auto 0px auto;
}
@media screen and (max-width: 500px){
  .mobile-marker-details{
    display: block;
  }
  .marker-details{
    display: none;
  }
}
</style>
