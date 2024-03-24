<template>
    <div class="d-flex">
        <div class="summary_component" style="width: 30%;">
            <PackageSummary/>
        </div>
        <div class="google-map" style="width: 70%;">
            <div id="map">
                <iframe
                        ref="map"
                        width="100%"
                        height="100%"
                        :src="googleMapPath"
                        style="border: 0;"
                @load="onLoadIframe"
                name="map"
                ></iframe>
            </div>
        </div>
    </div>
</template>

<script>
import PackageSummary from '../components/PackageDetails.vue';
import http from '@/config/httpInterceptor';
export default {
  components: {
      PackageSummary,
  },
  data() {
      return {
        googleMapPath: process.env.BASE_URL + 'google-maps-package-tracking.html',
      };
  },
  mounted() {
    http.get(this.$i18n.t('rootURL') + this.$i18n.t('getPackage')+this.$route.params.packageReference)
      .then(response => {
        this.$store.commit('updatePackage', response.data);
    }).catch(() => {
      console.log("unable to process your request this time. please try again latter.");
    });
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
