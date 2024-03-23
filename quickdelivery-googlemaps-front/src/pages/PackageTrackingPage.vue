<template>
    <button @click="send">send</button>
    <PackageTrackingGoogleMap ref="mapTrackingVue" :style="{ width: '100%', height: '87%' }"/>
</template>
<script>
import PackageTrackingGoogleMap from '../components/PackageTrackingGoogleMap.vue';

export default{
  computed: {

  },
  components: {
    PackageTrackingGoogleMap,
  },
  data() {
    return {
        packageReference: '',
    };
  },
  mounted() {
    this.packageReference = this.$route.params.packageReference;
    window.onmessage = (e) => {
        if (typeof e.data === 'string' && e.data === 'EndLoading') {
            this.$store.commit('updateLoaderStatus', false);
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
