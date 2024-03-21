<template>
    <div class="item-image">
      <!--<img :src="getImageSrc()" alt="Image" class="item-image" />-->
      <img src="../assets/box.png" alt="Image" />
    </div>
    <div class="item-details">
        <h3 v-if="package_.reference">{{$t('packageReference')}} : {{ package_.reference }}</h3>
        <!--<p>{{$t('packageHeight')}}:
            {{package_.height}}</p>
        <p>{{$t('packageWidth')}}:
            {{package_.width}} </p>
        <p>{{$t('packageDepth')}}:
            {{package_.depth}}</p>
        <p>{{$t('packageWeight')}}:
            {{package_.weight}}</p>-->
        <p><strong>{{$t('packagePrice')}} : </strong>{{package_.deliveryPrice}}&nbsp;{{ $t('currency') }}</p>
        <p><strong>{{$t('packageDeparture')}} : </strong>
        {{departureAddress.firstName}} {{departureAddress.lastName}} {{departureAddress.line1}} {{departureAddress.zipCode}} {{departureAddress.town}} {{departureAddress.country}}</p>
        <!--<p>{{departureAddress.phone}}</p>-->
        <p><strong>{{$t('packageDestination')}} : </strong>
        {{arrivalAddress.firstName}} {{arrivalAddress.lastName}} {{arrivalAddress.line1}} {{arrivalAddress.zipCode}} {{arrivalAddress.town}} {{arrivalAddress.country}}</p>
        <!--<p>{{arrivalAddress.phone}}</p>
        <p>{{$t('packageDistanceToDestination')}}:
            {{package_.distanceToDestination}}</p>-->
    </div>
    <div class="item-buttons">
      <button class="btn primary_btn"
        @click="details">{{ $t('packagesArroundMArkerDetailActionsDetails') }}</button>
    </div>
</template>
<script>

export default{
    props: {
    package_: Object,
    modal: Object,
  },
  computed: {
    departureAddress() {
      return this.departureAddress_();
    },
    arrivalAddress() {
      return this.destinationAddress_();
    },
  },
    methods: {
       getImageSrc() {
          let imgSrc = '';
          if (this.package_.files && this.package_.files[0]) {
            imgSrc = `data:image/png;base64,${this.package_.files[0].data}`;
          }
          return imgSrc;
        },
        destinationAddress_() {
         let selectedAddress;
          this.package_.addresses.forEach(address => {
            if(address.type === 'ARRIVAL'){
              selectedAddress = address;
            }
          });
          return selectedAddress;
        },
        departureAddress_() {
          let selectedAddress;
          this.package_.addresses.forEach(address => {
            if(address.type === 'DEPARTURE'){
              selectedAddress = address;
            }
          });
          return selectedAddress;
        },
        details(){
          this.$store.commit('updatePackage', this.package_);
          this.modal.openModal();
        },
    }
}
</script>
<style>
.item-details{
  height: 100%;
  padding: 5px 10px;
}
.item-image{
  width: 100%;
}
.item-image img{
  display: block;
  max-width: 100%;
  max-height: 250px;
  margin-left: auto;
  margin-right: auto;
}
.item-details p {
  font-size: 13px;
}
</style>