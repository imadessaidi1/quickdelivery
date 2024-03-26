<template>
    <div class="d_flex">
        <div class="tracking_summary_component" style="width: 25%;">
            <div>
              <PackageSummary/>
            </div>
        </div>
        <div class="google-map" style="width: 75%;">
          <div class="roadInfo" v-show="routeInfo">
              <p><strong>Distance:</strong> 25Km{{ routeInfo.distance.text }}</p>
              <p><strong>Duration:</strong> 25min{{ routeInfo.duration.text }}</p>
          </div>
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
  props: {
    routeInfo: Object,
  },
  mounted() {
    http.get(this.$i18n.t('rootURL') + this.$i18n.t('getPackage')+this.$route.params.packageReference)
      .then(response => {
        this.$store.commit('updatePackage', response.data);
    }).catch(() => {
      console.log("unable to process your request this time. please try again latter.");
    });
    window.onmessage = (e) => {
        if (typeof e.data === 'string' && e.data.includes('RouteInfo;')) {
            //const routeInfo = JSON.parse(e.data.split(';')[1]);
        }
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
.d_flex{
  display: flex;
}
.tracking_summary_component{
  overflow: scroll;
}
.roadInfo {
  position: absolute;
  top: 15px;
  right: 50%;
  transform: translateX(50%);
  display: flex;
  justify-content: space-between;
  width: max-content;
  margin: 0 auto;
  background-color: #00d3d3d5;
  border-radius: 10px;
  padding: 0 20px;
}
.roadInfo p:first-child{
  margin-right: 20px;
}
#map{
  height: 100%;
}
.google-map {
  width: 100%;
  height: 75vh;
  position: relative;
}
</style>
