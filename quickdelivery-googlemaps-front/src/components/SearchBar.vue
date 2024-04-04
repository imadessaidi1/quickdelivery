<template>
  <div class="search-bar">
     <a @click="toggleMenu" class="material-symbols-outlined burger_menu" ref="handleClickOutsideBurgerMenu">menu</a>
     <div class="menu vertical-menu">
      <ul>
        <span class="infobull" data-tooltip='Home page'><router-link to="/"><li class="material-symbols-outlined">home</li></router-link></span>
        <span class="infobull" data-tooltip='My packages'><router-link to="/myPackages"><li class="material-symbols-outlined" data-tooltip="My packages">deployed_code_account</li></router-link></span>
        <span class="infobull" data-tooltip='New package'><router-link to="/createPackage"><li class="material-symbols-outlined" data-tooltip="new package">box_add</li></router-link></span>
      </ul>
     </div>
    <transition name="fade">
      <div v-if="isActiveMenu" class="menu horizontal-menu">
        <ul>
          <router-link @click="toggleMenu" to="/"><li>{{$t('menuHome')}}</li></router-link>
          <router-link @click="toggleMenu" to="/myPackages"><li>{{$t('menuMyPackages')}}</li></router-link>
          <router-link @click="toggleMenu" to="/createPackage"><li>{{$t('menuNewPackage')}}</li></router-link>
        </ul>
      </div>
    </transition>
    <transition name="fade">
      <div v-if="isActiveLoginMenu" class="menu login-menu">
        <ul>
            <router-link @click="loginMenu" to="/userSignInPage"><li>{{$t('menuUserSignin')}}</li></router-link>
            <router-link @click="loginMenu" to="/packageTracking?packageReference=PACKFR202403170003271731677"><li>{{$t('menuUserLogin')}}</li></router-link>
            <router-link @click="loginMenu" to="/usersAccountValidation"><li>{{$t('menuUusersAccountValidation')}}</li></router-link>
            <router-link @click="loginMenu" to="/usersAccountValidation"><li>{{$t('menuUuserAccount')}}</li></router-link>
        </ul>
      </div>
    </transition>

    <!-- Barre de recherche -->
    <input type="text" id="searchInput" placeholder="Rechercher...">

    <!-- Bouton de connexion -->
    <span class="infobull" data-tooltip='Account' ref="handleClickOutsideUserMenu"><a class="material-symbols-outlined" @click="loginMenu">person</a></span>
  </div>
</template>

<script>
export default {
  data() {
      return {
        isActiveMenu: false,
        isActiveLoginMenu: false,
      };
    },
  mounted() {
  // Ajouter un écouteur d'événements sur la fenêtre
    window.addEventListener('click', this.handleClickOutsideUserMenu);
    window.addEventListener('click', this.handleClickOutsideBurgerMenu);
  }, 
  beforeUnmount() {
    // Supprimer l'écouteur d'événements lors de la destruction du composant
    window.removeEventListener('click', this.handleClickOutsideUserMenu);
    window.removeEventListener('click', this.handleClickOutsideBurgerMenu);
  },
  methods: {
    toggleMenu() {
      this.isActiveMenu = !this.isActiveMenu;
    },
    loginMenu() {
      this.isActiveLoginMenu = !this.isActiveLoginMenu;
    },
    handleClickOutsideUserMenu(event) {
      // Vérifier si le clic provient de l'élément à masquer ou de ses enfants
      if(this.$refs.handleClickOutsideUserMenu && !this.$refs.handleClickOutsideUserMenu.contains(event.target)){
        // Cacher l'élément si le clic ne provient pas de l'élément à masquer
        this.isActiveLoginMenu = false;
      }
    },
    handleClickOutsideBurgerMenu(event) {
      // Vérifier si le clic provient de l'élément à masquer ou de ses enfants
      if(this.$refs.handleClickOutsideBurgerMenu && !this.$refs.handleClickOutsideBurgerMenu.contains(event.target)){
        // Cacher l'élément si le clic ne provient pas de l'élément à masquer
        this.isActiveMenu = false;
      }
    },
  },
};
</script>

<style>

.search-bar {
  display: flex;
  min-height: 55px;
  box-sizing: border-box;
  justify-content: space-between;
  align-items: center;
  padding: 0px 40px;
  background-color: #e8e8e8;
  position: relative;
  z-index: 2;
}
.material-symbols-outlined{
  cursor: pointer;
  font-variation-settings:
  'FILL' 0,
  'wght' 400,
  'GRAD' 0,
  'opsz' 24
}
/* Styles pour le menu horizontal */
.horizontal-menu,
.login-menu{
  position: absolute;
  background-color: #fff; /* Ajoutez une couleur de fond selon vos besoins */
  border: 1px solid #ccc; /* Ajoutez une bordure si nécessaire */
  border-radius: 10px;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.5); /* Ajoutez une ombre si nécessaire */
}
.horizontal-menu {
  top: 100%; /* Ajustez la valeur en fonction de la hauteur de la barre de recherche */
  left: 10px;
}
.login-menu{
  top: 100%; /* Ajustez la valeur en fonction de la hauteur de la barre de recherche */
  right: 10px;
}
.menu ul {
  padding: 0;
  margin: 0;
  display: inline-block;
  justify-content: space-around; /* Ajustez l'alignement horizontal selon vos besoins */
}

.menu li {
  padding: 10px 15px;
  cursor: pointer;
  transition: all 0.3s;
  /* Ajoutez d'autres styles de texte, bordure, etc. selon vos besoins */
}
.menu li:hover {
  background-color: #adadad67;
}
#searchInput {
  width: 45%;
  height: 35px;
  border: none;
  padding: 0 15px;
  margin: 0;
  border: solid 2px rgba(120, 183, 255, 0);
  border-radius: 6px;
  transition: all 300ms;
}
#searchInput:hover{
  border: solid 2px #0086df;
}
/* Transition pour l'effet de fondu */
.fade-enter-active, .fade-leave-active {
  transition: opacity 300ms;
}

.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
.burger_menu{
  display: none;
}

@media screen and (max-width: 580px) {
  .burger_menu{
    display: block;
  }
  .vertical-menu{
    display: none;
  }
}

</style>
