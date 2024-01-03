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
      makersList: [],
    };
  },
   mounted() {
       window.onmessage = (e) => {
        if (typeof e.data === 'string' && e.data.includes('SelectedPackage:')) {
            const packageID = e.data.split(':')[1];
            this.makersList.forEach(marker => {
              marker.groupedPackagesList.forEach(package_ => {
                if (package_.id === packageID) {
                  // Trouver la référence au composant MarkerDetails
                  const markerDetailComponent = this.$refs[package_.id];

                  // Vérifier si la référence existe et appeler la méthode setFocusOnReserveButton
                  if (markerDetailComponent && markerDetailComponent.setFocusOnReserveButton) {
                    markerDetailComponent.setFocusOnReserveButton();
                  }
                }
              });
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
