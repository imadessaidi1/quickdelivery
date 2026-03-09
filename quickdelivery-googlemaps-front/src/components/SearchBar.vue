<template>
  <div class="search-bar">
     <a @click="toggleMenu" class="material-symbols-outlined burger_menu" ref="handleClickOutsideBurgerMenu">menu</a>
     <div class="menu vertical-menu">
      <ul>
        <span v-if="canSeeHome" class="infobull" data-tooltip='Home page'><router-link @click="toggleMenu(0)" to="/"><li class="material-symbols-outlined">home</li></router-link></span>
        <span v-if="canSeeMyPackages" class="infobull" data-tooltip='My packages'><router-link @click="toggleMenu(1)" to="/myPackages"><li class="material-symbols-outlined" data-tooltip="My packages">deployed_code_account</li></router-link></span>
        <span v-if="canSeeCreatePackage" class="infobull" data-tooltip='New package'><router-link @click="toggleMenu(2)" to="/createPackage"><li class="material-symbols-outlined" data-tooltip="new package">box_add</li></router-link></span>
      </ul>
     </div>
    <transition name="fade">
      <div v-if="isActiveMenu" class="menu horizontal-menu">
        <ul>
          <router-link v-if="canSeeHome" @click="toggleMenu(0)" to="/"><li>{{$t('menuHome')}}</li></router-link>
          <router-link v-if="canSeeMyPackages" @click="toggleMenu(1)" to="/myPackages"><li>{{$t('menuMyPackages')}}</li></router-link>
          <router-link v-if="canSeeCreatePackage" @click="toggleMenu(2)" to="/createPackage"><li>{{$t('menuNewPackage')}}</li></router-link>
        </ul>
      </div>
    </transition>
    <transition name="fade">
      <div v-if="isActiveLoginMenu" class="menu login-menu">
        <ul>
            <router-link v-if="isAdmin" @click="loginMenu" to="/userSignInPage"><li>{{$t('menuUserSignin')}}</li></router-link>
            <router-link v-if="isAdmin" @click="loginMenu" to="/packageTracking?packageReference=PACKFR202403170003271731677"><li>{{$t('menuUserLogin')}}</li></router-link>
            <router-link v-if="isAdmin" @click="loginMenu" to="/usersAccountValidation"><li>{{$t('menuUusersAccountValidation')}}</li></router-link>
            <router-link @click="loginMenu" to="/userAccount"><li>{{$t('menuUuserAccount')}}</li></router-link>
            <li @click="logoutUser">{{$t('menuUserLogout')}}</li>
        </ul>
      </div>
    </transition>

    <div v-show="showTextSearch" class="search-zone">
        <input type="text" id="searchInput" placeholder="Rechercher..."><button class="btn primary_btn">Search</button>
    </div>
    <div v-show="showAddressSearch" class="search-zone">
        <div class="address-zone">
          <AddressAutocomplete id="address" ref="addressAutoComplete"/>
        </div>
        <button class="btn primary_btn" @click="searchInMap">Search</button>
    </div>

    <span class="infobull" data-tooltip='Account' ref="handleClickOutsideUserMenu"><a class="material-symbols-outlined" @click="loginMenu">person</a></span>
  </div>
</template>

<script>
import AddressAutocomplete from './AddressAutocomplete.vue';
import { getCurrentUserRoles, logout } from '@/config/auth';

export default {
  components: {
    AddressAutocomplete,
  },
  data() {
    return {
      isActiveMenu: false,
      isActiveLoginMenu: false,
    };
  },
  computed: {
    location() {
      return this.$store.state.location;
    },
    userRoles() {
      return getCurrentUserRoles();
    },
    isAdmin() {
      return this.userRoles.includes('ROLE_ADMIN');
    },
    isLivreur() {
      return this.userRoles.includes('ROLE_LIVREUR');
    },
    isClient() {
      return this.userRoles.includes('ROLE_CLIENT') || this.userRoles.includes('ROLE_CLIENT_PRO');
    },
    canSeeHome() {
      return this.isAdmin || this.isLivreur;
    },
    canSeeCreatePackage() {
      return this.isAdmin || this.isClient;
    },
    canSeeMyPackages() {
      return this.isAdmin || this.isClient || this.isLivreur;
    },
    showAddressSearch() {
      return this.$route.path === '/';
    },
    showTextSearch() {
      return this.$route.path === '/myPackages';
    },
  },
  mounted() {
    window.addEventListener('click', this.handleClickOutsideUserMenu);
    window.addEventListener('click', this.handleClickOutsideBurgerMenu);
  },
  beforeUnmount() {
    window.removeEventListener('click', this.handleClickOutsideUserMenu);
    window.removeEventListener('click', this.handleClickOutsideBurgerMenu);
  },
  methods: {
    toggleMenu(index) {
      this.isActiveMenu = !this.isActiveMenu;
      if (index === 0) {
        this.$store.commit('updateLocation', 'mapPage');
      } else {
        this.$store.commit('updateLocation', 'other');
      }
    },
    loginMenu() {
      this.isActiveLoginMenu = !this.isActiveLoginMenu;
      this.$store.commit('updateLocation', 'other');
    },
    handleClickOutsideUserMenu(event) {
      if (this.$refs.handleClickOutsideUserMenu && !this.$refs.handleClickOutsideUserMenu.contains(event.target)) {
        this.isActiveLoginMenu = false;
      }
    },
    handleClickOutsideBurgerMenu(event) {
      if (this.$refs.handleClickOutsideBurgerMenu && !this.$refs.handleClickOutsideBurgerMenu.contains(event.target)) {
        this.isActiveMenu = false;
      }
    },
    searchInMap() {
      const rawAddress = this.$refs.addressAutoComplete?.address;
      if (!rawAddress || typeof rawAddress !== 'string') {
        return;
      }
      const address = rawAddress.split(',');
      if (address.length < 3) {
        return;
      }
      const line1 = address[0].trim();
      const zipCode = address[1].trim().split(' ')[0].trim();
      const index = address[1].trim().indexOf(' ');
      const town = address[1].substring(index + 1).trim();
      const country = address[2].trim();

      window.dispatchEvent(new CustomEvent('qd-search-around-address', {
        detail: {
          line1,
          zipCode,
          town,
          country,
          rawAddress,
        },
      }));
    },
    logoutUser() {
      this.isActiveLoginMenu = false;
      logout();
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
  background-color: rgba(219, 226, 239, 0.52) !important;
  backdrop-filter: blur(8px) saturate(120%);
  -webkit-backdrop-filter: blur(8px) saturate(120%);
  position: relative;
  z-index: 2;
}
.search-zone {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-width: 0;
  margin: 0 14px;
  flex-wrap: nowrap;
}
.material-symbols-outlined{
  cursor: pointer;
  font-variation-settings:
  'FILL' 0,
  'wght' 400,
  'GRAD' 0,
  'opsz' 24
}
.horizontal-menu,
.login-menu{
  position: absolute;
  background-color: #fff;
  border: 1px solid #ccc;
  border-radius: 10px;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.5);
}
.horizontal-menu {
  top: 100%;
  left: 10px;
}
.login-menu{
  top: 100%;
  right: 10px;
}
.menu ul {
  padding: 0;
  margin: 0;
  display: inline-block;
  justify-content: space-around;
}

.menu li {
  padding: 10px 15px;
  cursor: pointer;
  transition: all 0.3s;
}
.menu li:hover {
  background-color: #adadad67;
}
#searchInput {
  width: 100%;
  max-width: 560px;
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
.search-zone button {
  height: 35px;
  white-space: nowrap;
  flex-shrink: 0;
  min-width: 88px;
}
.search-zone #address {
  width: 100%;
  max-width: 560px;
}
.address-zone {
  flex: 1 1 auto;
  min-width: 0;
  width: 100%;
  max-width: 560px;
}
.address-zone :deep(input),
.address-zone :deep(.autocomplete-input) {
  width: 100%;
  height: 35px;
  border: none;
  padding: 0 15px;
  margin: 0;
  border: solid 2px rgba(120, 183, 255, 0);
  border-radius: 6px;
  transition: all 300ms;
}
.address-zone :deep(input:hover),
.address-zone :deep(.autocomplete-input:hover),
.address-zone :deep(input:focus),
.address-zone :deep(.autocomplete-input:focus) {
  border: solid 2px #0086df;
}
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
  .search-bar {
    padding: 0 12px;
    gap: 6px;
  }
  .search-zone {
    margin: 0 6px;
    gap: 6px;
    min-width: 0;
  }
  .burger_menu{
    display: block;
  }
  .vertical-menu{
    display: none;
  }
  #searchInput,
  .address-zone :deep(input),
  .address-zone :deep(.autocomplete-input),
  .search-zone button {
    height: 33px;
  }
  .search-zone button {
    min-width: 74px;
    padding: 0 10px;
  }
}

</style>
