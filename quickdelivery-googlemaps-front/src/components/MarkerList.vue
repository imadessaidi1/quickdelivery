<template>
  <div class="marker-list">
    <h2>{{$t('packagesArround')}}</h2>
    <ul>
      <li v-for="marker in makersList" :key="marker.addressString">
        <h2 :id="marker.addressString">{{displayAddress(marker.addressString)}}</h2>
        <div v-for="package_ in marker.groupedPackagesList" :key="package_.id">
          <MarkerDetails ref="markerDetail" :package_="package_" />
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
      makersList: [],
    };
  },
   mounted() {
       window.onmessage = (e) => {
        if (typeof e.data === 'string' && e.data.includes('SelectedPackage:')) {
           window.location.hash = `#${e.data}`;
           const packageID = e.data.split(':')[1];
           const markerDetailComponent = this.$refs.markerDetail;
           //const markerDetailToSelect = markerDetailComponent.filter(markerDetail => markerDetail.package_.id+'' === packageID);
           markerDetailComponent.forEach(markerDetail => {
            if(markerDetail.package_.id+'' === packageID)
             if (markerDetail && markerDetail.setFocusOnReserveButton) {
                markerDetail.setFocusOnReserveButton();
             }
           });
        }else{
           Object.entries(e.data).forEach(([coordinates, packagesArray]) => {
              if(typeof coordinates === 'string' && Array.isArray(packagesArray)){
                this.makersList.push({addressString: coordinates, groupedPackagesList: packagesArray});
              }
            });
        }
       };
   },
   methods: {
    displayAddress(addressString) {
      console.log(addressString);
      const addressArray = addressString.split(',');
      return addressArray[2];
    },
  },
};
</script>

<style scoped>
.marker-list ul{
  width: 100%;
  flex: 1;
  padding: 10px;
  max-height: 530px;
  overflow-y: auto;
}
ul{
 list-style: none;
 padding: 0;
}
</style>
