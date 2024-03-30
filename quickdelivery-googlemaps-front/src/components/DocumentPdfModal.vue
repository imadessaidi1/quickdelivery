<template>
  <div v-if="isOpen" class="modal">
  <div class="modal-content">
    <iframe :src="byteArrayPDF" width="600px" height="600px"></iframe>
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
  justify-content: center;
  align-items: center;
}

.centered-iframe {
  width: 100%; /* Pour occuper toute la largeur disponible */
  height: 100%; /* Pour occuper toute la hauteur disponible */
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
