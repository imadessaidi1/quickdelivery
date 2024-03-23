<template>
  <div class="google-map">
    <div id="map">
    <iframe
            ref="map"
            width="100%"
          height="100%"
          :src="googleMapPath"
          style="border:0;"
          @load="onLoadIframe"
          name="map"
        ></iframe>
    </div>
  </div>
</template>

<script>
export default {
  data() {
      return {
        googleMapPath: process.env.BASE_URL + 'google-maps-package-tracking.html',
      };
  },
  methods: {
    onLoadIframe() {
      this.$refs.map.contentWindow.postMessage("PackageReference:" + this.$route.params.packageReference, "*");
    }
  },
  watch: {
    '$store.state.packagesLastPosition': {
      deep: true,
      handler(newVal) {
        const newPosition = newVal[this.$route.params.packageReference];
        this.$refs.map.contentWindow.postMessage("PackageNewPosition;" + JSON.stringify(newPosition), "*");
      }
    }
  }
};
</script>

<style scoped>
#map{
  height: 100%;
}
.google-map {
  width: 100%;
  height: 75vh;
  flex: 1;
}
</style>
