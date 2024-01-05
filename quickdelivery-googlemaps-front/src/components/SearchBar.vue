<template>
  <div class="search-bar">
     <a @click="toggleMenu" class="material-symbols-outlined " style="display: none;">menu</a>
     <div class="menu vertical-menu">
      <ul>
        <router-link to="/"><li class="material-symbols-outlined">home</li></router-link>
        <router-link to="/myPackages"><li class="material-symbols-outlined">deployed_code_account</li></router-link>
        <router-link to="/createPackage"><li class="material-symbols-outlined">box_add</li></router-link>
      </ul>
     </div>
    <transition name="fade">
          <div v-if="isActiveMenu" class="menu horizontal-menu">
            <ul>
              <router-link to="/"><li>{{$t('menuHome')}}</li></router-link>
              <router-link to="/myPackages"><li>{{$t('menuMyPackages')}}</li></router-link>
              <router-link to="/createPackage"><li>{{$t('menuNewPackage')}}</li></router-link>
            </ul>
          </div>
        </transition>
        <transition name="fade">
          <div v-if="isActiveLoginMenu" class="menu login-menu">
            <ul>
              <li>{{$t('menuUserLogin')}}</li>
              <li>{{$t('menuUserSignin')}}</li>
              <li>{{$t('menuUuserAccount')}}</li>
            </ul>
          </div>
        </transition>

    <!-- Barre de recherche -->
    <input type="text" id="searchInput" placeholder="Rechercher...">

    <!-- Bouton de connexion -->
    <div>
      <ul>
        <li></li>
        <li></li>
        <li></li>
      </ul>
    </div>
    <a class="material-symbols-outlined" @click="loginMenu">person</a>
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
  methods: {
    toggleMenu() {
      this.isActiveMenu = !this.isActiveMenu;
    },
    loginMenu() {
      this.isActiveLoginMenu = !this.isActiveLoginMenu;
    },
  },
};

</script>

<style>

.search-bar {
  display: flex;
  min-height: 4%;
  box-sizing: border-box;
  justify-content: space-between;
  align-items: center;
  padding: 10px 40px;
  margin: 0;
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
  list-style-type: none;
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
  border: solid 2px rgba(120, 183, 255, 0);
  border-radius: 6px;
  transition: all 300ms;
}
#searchInput:hover{
  border: solid 2px #0086df;
}

.primary_btn:hover{
  background-color: #0086df;
  color: #fff;
}
/* Transition pour l'effet de fondu */
.fade-enter-active, .fade-leave-active {
  transition: opacity 300ms;
}

.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
.material-symbols-outlined {
    font-variation-settings:
    'FILL' 0,
    'wght' 400,
    'GRAD' 0,
    'opsz' 24
  }
</style>
