<template>
    <PackageTrackingGoogleMap ref="mapTrackingVue" :style="{ width: '100%', height: '87%' }" :routeInfo="routeInfo"/>
</template>
<script>
import PackageTrackingGoogleMap from '../components/PackageTrackingGoogleMap.vue';

export default{
  components: {
    PackageTrackingGoogleMap,
  },
  data() {
    return {
        packageReference: '',
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
    this.packageReference = this.$route.params.packageReference;
    window.onmessage = (e) => {
        if (typeof e.data === 'string' && e.data === 'EndLoading') {
            this.$store.commit('updateLoaderStatus', false);
        }else if (typeof e.data === 'string' && e.data.includes('RouteInfo;')) {
            this.routeInfo = JSON.parse(e.data.split(';')[1]);
        }
       };

  },
  methods: {
    send(){
    this.$refs.mapTrackingVue.$refs.map.contentWindow.postMessage("PackageReference:" + this.packageReference, "*");
    },
  },
}
</script>
