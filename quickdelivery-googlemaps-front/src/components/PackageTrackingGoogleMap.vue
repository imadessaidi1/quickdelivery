<template>
    <div class="d_flex">
        <div class="tracking_summary_component">
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
            <button @click="details" class="btn primary_btn">{{ $t('packagesArroundMArkerDetailActionsDetails') }}</button>
          </div>
        </div>
    </div>
    <div v-if="showModal" class="modal">
        <div class="modal-content">
          <h2>{{$t('packageSummaryAction')}}</h2>
          <PackageSummary/>
          <button class="close-btn" @click="closeDetails"><span class="material-symbols-outlined size-24">cancel</span></button>
        </div>
    </div>
</template>

<script>
import PackageSummary from '../components/VerticalPackageDetails.vue';
import http from '@/config/httpInterceptor';
export default {
  components: {
      PackageSummary,
  },
  data() {
      return {
        googleMapPath: process.env.BASE_URL + 'google-maps-package-tracking.html',
        showModal: false,
      };
  },
  props: {
    routeInfo: Object,
  },
  mounted() {
    http.get(this.$i18n.t('rootURL') + this.$i18n.t('getPackage')+this.$parent.packageReference)
      .then(response => {
        this.$store.commit('updatePackage', response.data);
    }).catch(() => {
      console.log("unable to process your request this time. please try again latter.");
    });
  },
  methods: {
    onLoadIframe() {
      this.$refs.map.contentWindow.postMessage("PackageReference:" + this.$parent.packageReference, "*");
    },
    details(){
      this.showModal = true;
    },
    closeDetails(){
      this.showModal = false;
    }
  },
  watch: {
    '$store.state.packagesLastPosition': {
      deep: true,
      handler(newVal) {
        const newPosition = newVal[this.$parent.packageReference];
        this.$refs.map.contentWindow.postMessage("PackageNewPosition;" + JSON.stringify(newPosition), "*");
      }
    }
  }
};
</script>

<style scoped>
.d_flex{
  display: flex;
  height: 100% !important;
}
.tracking_summary_component{
  width: 25%;
  height: 100%;
  overflow: scroll;
  box-shadow: rgba(0, 0, 0, 0.45) 20px 0px 30px -34px;
  background-color: #F9F7F7;
  z-index: 1;
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
  height: 100%;
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
.modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.25);
  display: flex;
  justify-content: center;
  align-items: center;
}

.modal-content {
  width: 70%;
  padding: 0 20px 20px 20px;
  border-radius: 10px;
  position: relative;
  /* From https://css.glass */
  background: rgba(255, 255, 255, 0.5);
  border-radius: 16px;
  box-shadow: 0 4px 30px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(6.6px);
  -webkit-backdrop-filter: blur(6.6px);
}

.close-btn {
  /* Styles pour le bouton de fermeture (position absolue en haut à droite, couleur, curseur, etc.) */
  position: absolute;
  top: 10px;
  right: 10px;
  cursor: pointer;
  color: #555;
  border: none;
  background: none;
  transition: all .3s;
}
.close-btn:hover{
  color: #000000;
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
