<template>
  <div class="search-bar">
     <a @click="toggleMenu" class="material-symbols-outlined burger_menu" ref="handleClickOutsideBurgerMenu">menu</a>
     <div class="menu vertical-menu">
      <ul>
        <span v-if="canSeeHome" class="infobull" :data-tooltip="$t('menuTooltipHome')"><router-link @click="toggleMenu(0)" to="/app"><li class="material-symbols-outlined">home</li></router-link></span>
        <span v-if="canSeeMyPackages" class="infobull" :data-tooltip="$t('menuTooltipMyPackages')"><router-link @click="toggleMenu(1)" to="/myPackages"><li class="material-symbols-outlined">deployed_code_account</li></router-link></span>
        <span v-if="canSeeCreatePackage" class="infobull" :data-tooltip="$t('menuTooltipNewPackage')"><router-link @click="toggleMenu(2)" to="/createPackage"><li class="material-symbols-outlined">box_add</li></router-link></span>
      </ul>
     </div>
    <transition name="fade">
      <div v-if="isActiveMenu" class="menu horizontal-menu">
        <ul>
          <router-link v-if="canSeeHome" @click="toggleMenu(0)" to="/app"><li>{{$t('menuHome')}}</li></router-link>
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
        <input type="text" id="searchInput" :placeholder="$t('mapSearchPlaceholder')"><button class="btn primary_btn">{{ $t('actionSearch') }}</button>
    </div>
    <div v-show="showAddressSearch" class="search-zone">
        <div class="address-zone">
          <AddressAutocomplete id="address" ref="addressAutoComplete"/>
        </div>
        <button class="btn primary_btn" @click="searchInMap">{{ $t('actionSearch') }}</button>
    </div>

    <span class="infobull account-trigger" :data-tooltip="$t('menuTooltipAccount')" ref="handleClickOutsideUserMenu">
      <a class="account-link" @click="loginMenu">
        <span class="material-symbols-outlined">person</span>
        <span v-if="connectedUserFullName" class="account-name">{{ connectedUserFullName }}</span>
      </a>
    </span>
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
      return this.$route.path === '/app';
    },
    showTextSearch() {
      return this.$route.path === '/myPackages';
    },
    connectedUserFullName() {
      const firstName = this.$store.state.connectedUser?.firstName || '';
      const lastName = this.$store.state.connectedUser?.lastName || '';
      return `${firstName} ${lastName}`.trim();
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
  min-height: 62px;
  box-sizing: border-box;
  justify-content: space-between;
  align-items: center;
  padding: 0 28px;
  background: #f8fafc !important;
  border-bottom: 1px solid #e7ebf2;
  position: relative;
  z-index: 6;
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
.account-trigger {
  display: inline-flex;
  align-items: center;
}
.account-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: inherit;
  text-decoration: none;
}
.account-name {
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
  white-space: nowrap;
}
.horizontal-menu,
.login-menu{
  position: absolute;
  background-color: #fff;
  border: 1px solid #dde3ec;
  border-radius: 12px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.12);
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
  height: 40px;
  border: 1px solid #e2e8f0;
  padding: 0 15px;
  margin: 0;
  border-radius: 12px;
  background: #ffffff;
  transition: all 300ms;
}
#searchInput:hover{
  border-color: #b9c6de;
}
.search-zone button {
  height: 40px;
  white-space: nowrap;
  flex-shrink: 0;
  min-width: 96px;
  border-radius: 12px;
  border: none;
  background: #020617;
  color: #ffffff;
  font-weight: 600;
}
.search-zone button:hover {
  background: #0f172a;
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
  height: 40px;
  border: 1px solid #e2e8f0;
  padding: 0 15px;
  margin: 0;
  border-radius: 12px;
  background: #ffffff;
  transition: all 300ms;
}
.address-zone :deep(input:hover),
.address-zone :deep(.autocomplete-input:hover),
.address-zone :deep(input:focus),
.address-zone :deep(.autocomplete-input:focus) {
  border-color: #b9c6de;
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
    padding: 0 10px;
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
    height: 35px;
  }
  .search-zone button {
    min-width: 74px;
    padding: 0 10px;
  }
  .account-name {
    display: none;
  }
}

</style>
