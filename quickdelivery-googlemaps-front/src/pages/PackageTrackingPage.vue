<template>
    <PackageTrackingGoogleMap ref="mapTrackingVue"  :routeInfo="routeInfo"/>
</template>
<script>
import PackageTrackingGoogleMap from '../components/PackageTrackingGoogleMap.vue';

export default{
  components: {
    PackageTrackingGoogleMap,
  },
  props: {
    packageReference: String,
  },
  data() {
    return {
        routeInfo: {
            distance: {
                text: '',
                value: ''
            },
            duration: {
                text: '',
                value: ''
            }
        },
    };
  },
  mounted() {
    window.onmessage = (e) => {
        if (typeof e.data === 'string' && e.data === 'EndLoading') {
            this.$store.commit('updateLoaderStatus', false);
        }else if (typeof e.data === 'string' && e.data.includes('RouteInfo;')) {
            this.routeInfo = JSON.parse(e.data.split(';')[1]);
        }
       };

  },
}
</script>
