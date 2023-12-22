<template>
  <div class="marker-list">
    <h2>{{$t('packagesArround')}}</h2>
    <ul>
      <li v-for="package_ in packagesList" :key="package_.id">
        <MarkerDetails ref="markerDetail" :package_="package_" />
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
