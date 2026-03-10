<template>
  <div v-if="isOpen" class="pdf_modal">
  <div class="modal-content">
    <iframe class="centered-iframe" :src="byteArrayPDF"></iframe>
    <button class="close-btn" ref="closeModalButtons"
    @click="closeModal"><span class="material-symbols-outlined size-24">cancel</span></button>
  </div>
  </div>
</template>

<script>
export default {
  name: "PDFJSViewer",
  data() {
    return {
      isOpen: false,
    }
  },
  props: {
    byteArrayPDF: String
  },
  methods: {
    openModal() {
      this.isOpen = true;
    },
    closeModal() {
      this.isOpen = false;
    },
  },
}
</script>
<style scoped>
/* Styles CSS pour votre modal */
.pdf_modal {
  position: fixed;
  top: 0;
  left: 0;
  z-index: 1200;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.25);
  display: flex;
  justify-content: center;
  align-items: center;
}

.pdf_modal .modal-content {
  width: min(760px, 92vw);
  height: min(700px, 88vh);
  padding: 22px 14px 14px 14px;
  border-radius: 10px;
  position: relative;
  z-index: 1201;
  /* From https://css.glass */
  background: rgba(255, 255, 255, 0.5);
  box-shadow: 0 4px 30px rgba(0, 0, 0, 0.1);
  justify-content: center;
  align-items: center;
}

.pdf_modal .centered-iframe {
  width: 100%; /* Pour occuper toute la largeur disponible */
  height: 100%; /* Pour occuper toute la hauteur disponible */
}

.pdf_modal .close-btn {
  /* Styles pour le bouton de fermeture (position absolue en haut à droite, couleur, curseur, etc.) */
  position: absolute;
  top: 5px;
  right: 2px;
  z-index: 1202;
  cursor: pointer;
  color: #555;
  border: none;
  background: none;
  transition: all .3s;
}
.pdf_modal .close-btn:hover{
  color: #000000;
}
@media screen and (max-width: 1100px){
  .modal {
    align-items: baseline;
    overflow: scroll;
  }
  .pdf_modal .modal-content{
    margin: 60px 0;
  }
}
@media screen and (max-width: 600px){
  .pdf_modal .modal-content{
    width: 85%;
  }
}
</style>
