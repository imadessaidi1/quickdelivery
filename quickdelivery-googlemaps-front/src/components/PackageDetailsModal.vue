<!-- Modal.vue -->
<template>
  <div v-if="isOpen" class="modal">
    <div class="modal-content">
      <PackageSummary />
      <br/>
      <p>
        <button class="primary_btn" ref="closeModalButtons"
        @click="closeModal">{{ $t('packagesArroundMArkerDetailActionCloseModal') }}</button>&nbsp;
      <button class="primary_btn" ref="detailsButtons"
      @click="reserve">{{ $t('packagesArroundMArkerDetailActionsReserve') }}</button>
</p>
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
    };
  },
  methods: {
    openModal() {
      this.isOpen = true;
    },
    closeModal() {
      this.isOpen = false;
    },
    reserve() {
      const url = this.$i18n.t('rootURL') + this.$i18n.t('reservePackageUrl') + "packageID=" + this.$store.state.package_.id + "&deliveryPersonID=" + this.$store.state.connectedUser.id;
      console.log(url);
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
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
}

.modal-content {
  background: white;
  padding: 20px;
}

.close-btn {
  /* Styles pour le bouton de fermeture (position absolue en haut à droite, couleur, curseur, etc.) */
  position: absolute;
  top: 10px;
  right: 10px;
  font-size: 20px;
  cursor: pointer;
  color: #555;
}
</style>
