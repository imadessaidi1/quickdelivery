<template>
  <div class="marker-list">
    <h2>{{$t('packagesArround')}}</h2>
    <ul>
      <li v-for="marker in makersList" :key="marker.addressString">
        <h2 :id="marker.addressString">{{displayAddress(marker.addressString)}} {{$t('packagesArroundDistanceFromYou')}}</h2>

        <div v-for="(package_,index) in marker.groupedPackagesList" :key="marker.addressString+index">
          <MarkerDetails ref="marker.addressString+index" :package_="package_" />
        </div>
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
        if (Array.isArray(e.data)) {
                const rawData = e.data;
                this.packagesList = JSON.parse(JSON.stringify(rawData));
        }
        if (typeof e.data === 'string' && e.data.includes('SelectedPackage:')) {
           const packageID = e.data.split(':')[1];
           const markerDetailComponent = this.$refs.markerDetail;
           //const markerDetailToSelect = markerDetailComponent.filter(markerDetail => markerDetail.package_.id+'' === packageID);
           markerDetailComponent.forEach(markerDetail => {
            if(markerDetail.package_.id+'' === packageID)
             if (markerDetail && markerDetail.setFocusOnReserveButton) {
                markerDetail.setFocusOnReserveButton();
             }
           });
        }
       };
   },
   methods: {
    displayAddress(addressString) {
      const addressArray = addressString.split(',');
      return addressArray[2];
    },
  },
};
</script>

<style scoped>
.marker-list{
  padding: 0 20px;
  height: 75vh;
}
.marker-list h2{
  margin-left: 10px;
}
.marker-list ul{
  width: 100%;
  flex: 1;
  max-height: 60vh;
  overflow-y: auto;
}
ul{
 list-style: none;
 padding: 0;
}

</style>
