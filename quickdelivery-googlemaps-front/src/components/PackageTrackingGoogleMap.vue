<template>
    <div class="d_flex">
        <div class="tracking_summary_component">
          <h2>{{$t('packageSummaryAction')}}</h2>
          <PackageSummary/>
        </div>
        <div class="google-map">
          <div class="roadInfo" v-show="routeInfo">
              <p><strong>Distance:</strong> {{ routeInfo.distance.text }}</p>
              <p><strong>Duration:</strong> {{ routeInfo.duration.text }}</p>
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
          <div class="btnInfo">
            <button class="btn primary_btn" type="button">{{ $t('packagesArroundMArkerDetailActionsDetails') }}</button>
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
  height: 94vh !important;
}
.tracking_summary_component{
  width: 25%;
  height: 89vh;
  overflow: scroll;
  box-shadow: rgba(0, 0, 0, 0.45) 20px 0px 30px -34px;
  background-color: #eeeeee;
  z-index: 1;
}
.tracking_summary_component h2{
  margin-left: 20px;
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
  background-color: #10b3ffd5;
  border-radius: 10px;
  padding: 0 20px;
  box-shadow: rgba(0, 0, 0, 0.24) 0px 3px 8px;
}
.roadInfo p:first-child{
  margin-right: 20px;
}
#map{
  height: 89vh;
}
.google-map {
  width: 75%;
  position: relative;
}
.btnInfo{
  display: none;
  position: absolute;
  bottom: 60px;
  right: 50%;
  transform: translateX(50%);
}
@media screen and (max-width: 1100px){
  .tracking_summary_component{
    display: none;
  }
  .google-map{
    width: 100%;
  }
  .btnInfo{
  display: block;
}
}
</style>
