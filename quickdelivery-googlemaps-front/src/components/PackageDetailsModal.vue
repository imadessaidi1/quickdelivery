<!-- Modal.vue -->
<template>
  <div v-if="isOpen" class="modal">
    <div class="modal-content">
      <PackageSummary />
      <br/>
        <button class="close-btn" ref="closeModalButtons"
        @click="closeModal"><span class="material-symbols-outlined">cancel</span></button>
    </div>
  </div>
</template>

<script>
//import SummarizedPackageDetail from './SummarizedPackageDetail.vue';
import PackageSummary from '../components/PackageDetails.vue';
import http from '@/config/httpInterceptor';

export default {
  components: {
      //SummarizedPackageDetail,
      PackageSummary,
    },
  data() {
    return {
      isOpen: false,
      emptyPackage: {
        id: null,
        version: null,
        creationDate: null,
        height: 0,
        width: 0,
        depth: 0,
        weight: 0,
        pictureURL: "",
        status: "",
        deliveryPrice: null,
        senderID: null,
        packageReservations: [],
        addresses: [{
        firstName: "",
        lastName: "",
        line1: "",
        line2: "",
        town: "",
        zipCode: "",
        country: "",
        floor:0,
        dateTime: null,
        email: "",
        phone: "",
        type: "DEPARTURE",
        latitude: 0,
        longitude: 0,
      },
      {
        firstName: "",
        lastName: "",
        line1: "",
        line2: "",
        town: "",
        zipCode: "",
        country: "",
        floor:0,
        dateTime: null,
        email: "",
        phone: "",
        type: "ARRIVAL",
        latitude: 0,
        longitude: 0,
      }],
      lastPositionLatitude: null,
      lastPositionLongitude: null
    }
    };
  },
  methods: {
    openModal() {
      this.isOpen = true;
    },
    closeModal() {
      this.$store.commit('updatePackage', this.emptyPackage);
      this.isOpen = false;
    },
    reserve() {
      const url = this.$i18n.t('rootURL') + this.$i18n.t('reservePackageUrl') + "packageID=" + this.$store.state.package_.id + "&deliveryPersonID=" + this.$store.state.connectedUser.id;
      return http.put(url)
        .then(response => {
          if(response.status == '200'){
            this.$parent.$refs.mapVue.$refs.map.contentWindow.postMessage("RefreshPackagesList", "*");
            this.isOpen = false;
          }
          return response.data;
        }).catch(() => {
          console.log("unable to process your request this time. please try again latter.");
        });
    },
  },
};
</script>

<style scoped>
/* Styles CSS pour votre modal */
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
  .modal {
    align-items: baseline;
    overflow: scroll;
  }
  .modal-content{
    margin: 60px 0;
  }
}
@media screen and (max-width: 600px){
  .modal-content{
    width: 85%;
  }
}
</style>
