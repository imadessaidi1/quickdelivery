<template>
  <div class="search-bar">
    <!-- Bouton du menu -->
    <button @click="toggleMenu">☰</button>
    <transition name="fade">
          <div v-if="isActiveMenu" class="horizontal-menu">
            <ul>
              <li>{{$t('menuMyPackages')}}</li>
              <li><a @click="openModal">{{$t('menuNewPackage')}}</a></li>
              <li>CGU</li>
              <li>Politique des Cookies</li>
              <li>Nous Contacter</li>
            </ul>
          </div>
        </transition>

    <!-- Barre de recherche -->
    <input type="text" id="searchInput" placeholder="Rechercher...">

    <!-- Bouton de connexion -->
    <button @click="login">Connexion</button>
  </div>
  <AppModal ref="AppModal" />
</template>

<script>
import AppModal from "@/components/CreatePackageModal.vue";
export default {
  components: {
    AppModal,
  },
  data() {
      return {
        isActiveMenu: false,
      };
    },
  methods: {
    toggleMenu() {
      this.isActiveMenu = !this.isActiveMenu;
    },
    login() {
      // Logique de connexion
      console.log('User logged in');
    },
    openModal() {
      this.$refs.AppModal.openModal();
    },
  },
};
</script>

<style>
.search-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
  background-color: #f0f0f0;
  position: relative;
}

/* Styles pour le menu horizontal */
.horizontal-menu {
  position: absolute;
  top: 100%; /* Ajustez la valeur en fonction de la hauteur de la barre de recherche */
  left: 0;
  width: 100%;
  background-color: #fff; /* Ajoutez une couleur de fond selon vos besoins */
  border: 1px solid #ccc; /* Ajoutez une bordure si nécessaire */
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1); /* Ajoutez une ombre si nécessaire */
}

.horizontal-menu ul {
  list-style-type: none;
  padding: 0;
  margin: 0;
  display: flex;
  justify-content: space-around; /* Ajustez l'alignement horizontal selon vos besoins */
}

.horizontal-menu li {
  padding: 10px;
  cursor: pointer;
  /* Ajoutez d'autres styles de texte, bordure, etc. selon vos besoins */
}

/* Transition pour l'effet de fondu */
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.5s;
}

.fade-enter, .fade-leave-to {
  opacity: 0;
}
</style>
