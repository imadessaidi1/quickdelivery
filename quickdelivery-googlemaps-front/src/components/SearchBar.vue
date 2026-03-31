<template>
  <div class="search-bar">
     <a @click="toggleMenu" class="material-symbols-outlined burger_menu" ref="handleClickOutsideBurgerMenu">menu</a>
     <div class="menu vertical-menu">
      <ul>
        <span v-if="canSeeHome" class="infobull" :data-tooltip="$t('menuTooltipHome')"><router-link @click="toggleMenu(0)" :to="homeRoute"><li class="material-symbols-outlined">home</li></router-link></span>
        <span v-if="canSeeMap" class="infobull" :data-tooltip="$t('menuTooltipMap')"><router-link @click="toggleMenu(1)" to="/app"><li class="material-symbols-outlined">map</li></router-link></span>
        <span v-if="canSeeMyPackages" class="infobull" :data-tooltip="$t('menuTooltipMyPackages')"><router-link @click="toggleMenu(2)" to="/myPackages"><li class="material-symbols-outlined">deployed_code_account</li></router-link></span>
        <span v-if="canSeeCreatePackage" class="infobull" :data-tooltip="$t('menuTooltipNewPackage')"><router-link @click="toggleMenu(2)" to="/createPackage"><li class="material-symbols-outlined">box_add</li></router-link></span>
      </ul>
     </div>
    <transition name="fade">
      <div v-if="isActiveMenu" class="menu horizontal-menu">
        <ul>
          <router-link v-if="canSeeHome" @click="toggleMenu(0)" :to="homeRoute"><li>{{$t('menuHome')}}</li></router-link>
          <router-link v-if="canSeeMap" @click="toggleMenu(1)" to="/app"><li>{{$t('menuMap')}}</li></router-link>
          <router-link v-if="canSeeMyPackages" @click="toggleMenu(2)" to="/myPackages"><li>{{$t('menuMyPackages')}}</li></router-link>
          <router-link v-if="canSeeCreatePackage" @click="toggleMenu(2)" to="/createPackage"><li>{{$t('menuNewPackage')}}</li></router-link>
        </ul>
      </div>
    </transition>
    <transition name="fade">
      <div v-if="isActiveLoginMenu" class="menu login-menu">
        <ul>
            <router-link v-if="isAdmin" @click="loginMenu" to="/userSignInPage"><li>{{$t('menuUserSignin')}}</li></router-link>
            <router-link v-if="isAdmin" @click="loginMenu" to="/usersAccountValidation"><li>{{$t('menuUusersAccountValidation')}}</li></router-link>
            <router-link @click="loginMenu" to="/notifications">
              <li class="menu-item-with-badge">
                <span>{{$t('menuNotifications')}}</span>
                <span v-if="unreadNotificationsCount" class="menu-badge">{{ unreadNotificationsCount }}</span>
              </li>
            </router-link>
            <li v-if="canInstallPwa" @click="installPwa">{{$t('menuInstallApp')}}</li>
            <router-link @click="loginMenu" to="/userAccount"><li>{{$t('menuUuserAccount')}}</li></router-link>
            <li @click="logoutUser">{{$t('menuUserLogout')}}</li>
        </ul>
      </div>
    </transition>

    <div
      v-if="isAdmin"
      class="admin-menu-wrapper"
      ref="handleClickOutsideAdminMenu"
    >
      <span class="infobull admin-trigger" :data-tooltip="$t('menuTooltipAdminCenter')">
        <a class="account-link admin-link" @click="adminMenu">
          <span class="material-symbols-outlined">shield_person</span>
          <span class="account-name">{{$t('menuAdminCenter')}}</span>
        </a>
      </span>
      <transition name="fade">
        <div v-if="isActiveAdminMenu" class="menu login-menu admin-menu">
          <ul>
            <router-link @click="adminMenu" to="/dashboard/metrics"><li>{{$t('menuAdminMetrics')}}</li></router-link>
            <router-link @click="adminMenu" to="/dashboard/finance"><li>{{$t('menuAdminFinance')}}</li></router-link>
          </ul>
        </div>
      </transition>
    </div>

    <span class="infobull account-trigger" :data-tooltip="$t('menuTooltipAccount')" ref="handleClickOutsideUserMenu">
      <a class="account-link" @click="loginMenu">
        <span class="material-symbols-outlined">person</span>
        <span v-if="connectedUserFullName" class="account-name">{{ connectedUserFullName }}</span>
        <span v-if="unreadNotificationsCount" class="account-badge">{{ unreadNotificationsCount }}</span>
      </a>
    </span>
  </div>
</template>

<script>
import { getCurrentUserRoles, logout, resolveLandingPathForCurrentUser } from '@/config/auth';

export default {
  data() {
    return {
      isActiveMenu: false,
      isActiveLoginMenu: false,
      isActiveAdminMenu: false,
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
      return this.isAdmin || this.isClient || this.isLivreur;
    },
    canSeeMap() {
      return this.isAdmin || this.isLivreur;
    },
    canSeeCreatePackage() {
      return this.isAdmin || this.isClient;
    },
    canSeeMyPackages() {
      return this.isAdmin || this.isClient || this.isLivreur;
    },
    connectedUserFullName() {
      const firstName = this.$store.state.connectedUser?.firstName || '';
      const lastName = this.$store.state.connectedUser?.lastName || '';
      return `${firstName} ${lastName}`.trim();
    },
    unreadNotificationsCount() {
      return this.$store.getters.unreadNotificationsCount || 0;
    },
    canInstallPwa() {
      return this.$store.state.canInstallPwa;
    },
    homeRoute() {
      return resolveLandingPathForCurrentUser();
    },
  },
  mounted() {
    window.addEventListener('click', this.handleClickOutsideUserMenu);
    window.addEventListener('click', this.handleClickOutsideBurgerMenu);
    window.addEventListener('click', this.handleClickOutsideAdminMenu);
  },
  beforeUnmount() {
    window.removeEventListener('click', this.handleClickOutsideUserMenu);
    window.removeEventListener('click', this.handleClickOutsideBurgerMenu);
    window.removeEventListener('click', this.handleClickOutsideAdminMenu);
  },
  methods: {
    toggleMenu() {
      this.isActiveMenu = !this.isActiveMenu;
      this.isActiveLoginMenu = false;
      this.isActiveAdminMenu = false;
      this.$store.commit('updateLocation', 'other');
    },
    loginMenu() {
      this.isActiveLoginMenu = !this.isActiveLoginMenu;
      this.isActiveAdminMenu = false;
      this.isActiveMenu = false;
      this.$store.commit('updateLocation', 'other');
    },
    adminMenu() {
      this.isActiveAdminMenu = !this.isActiveAdminMenu;
      this.isActiveLoginMenu = false;
      this.isActiveMenu = false;
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
    handleClickOutsideAdminMenu(event) {
      if (this.$refs.handleClickOutsideAdminMenu && !this.$refs.handleClickOutsideAdminMenu.contains(event.target)) {
        this.isActiveAdminMenu = false;
      }
    },
    logoutUser() {
      this.isActiveLoginMenu = false;
      this.isActiveAdminMenu = false;
      logout();
    },
    installPwa() {
      this.isActiveLoginMenu = false;
      this.isActiveAdminMenu = false;
      window.dispatchEvent(new CustomEvent('qd-install-pwa'));
    },
  },
};
</script>

<style>
.search-bar {
  display: flex;
  min-height: 62px;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  justify-content: flex-start;
  align-items: center;
  padding: 0 28px;
  background: #f8fafc !important;
  border-bottom: 1px solid #e7ebf2;
  position: relative;
  z-index: 6;
  gap: 12px;
}
.material-symbols-outlined{
  cursor: pointer;
  font-variation-settings:
  'FILL' 0,
  'wght' 400,
  'GRAD' 0,
  'opsz' 24
}
.vertical-menu ul {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.vertical-menu {
  margin-right: auto;
}
.infobull {
  position: relative;
  display: inline-flex;
  align-items: center;
}
.infobull::after {
  content: attr(data-tooltip);
  position: absolute;
  left: 50%;
  top: calc(100% + 10px);
  transform: translateX(-50%);
  padding: 6px 10px;
  border-radius: 8px;
  background: #0f172a;
  color: #fff;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.2s ease;
  z-index: 20;
}
.infobull::before {
  content: '';
  position: absolute;
  left: 50%;
  top: calc(100% + 4px);
  transform: translateX(-50%);
  border-width: 6px;
  border-style: solid;
  border-color: transparent transparent #0f172a transparent;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.2s ease;
  z-index: 19;
}
.infobull:hover::after,
.infobull:hover::before {
  opacity: 1;
}
.account-trigger {
  display: inline-flex;
  align-items: center;
  min-width: 0;
}
.admin-trigger {
  display: inline-flex;
  align-items: center;
  min-width: 0;
}
.admin-menu-wrapper {
  position: relative;
  display: inline-flex;
  align-items: center;
  min-width: 0;
}
.account-link {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: inherit;
  text-decoration: none;
  max-width: 100%;
  min-width: 0;
}
.admin-link {
  padding: 8px 12px;
  border-radius: 999px;
  background: #fff3e8;
  border: 1px solid #ffd4ae;
}
.admin-link .material-symbols-outlined,
.admin-link .account-name {
  color: #b45309;
}
.account-name {
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
  max-width: min(32vw, 220px);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.account-badge,
.menu-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  border-radius: 999px;
  padding: 0 6px;
  background: #f97316;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
}
.menu-item-with-badge {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.horizontal-menu,
.login-menu{
  position: absolute;
  background-color: #fff;
  border: 1px solid #dde3ec;
  border-radius: 12px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.12);
  max-width: calc(100vw - 20px);
}
.horizontal-menu {
  top: 100%;
  left: 10px;
}
.login-menu{
  top: 100%;
  right: 10px;
}
.admin-menu {
  top: 100%;
  right: 0;
  left: auto;
}
.menu ul {
  padding: 0;
  margin: 0;
  display: inline-block;
  justify-content: space-around;
  list-style: none;
  max-width: 100%;
}

.menu a {
  display: block;
  color: #0f172a;
  text-decoration: none;
}

.menu li {
  padding: 10px 15px;
  cursor: pointer;
  transition: all 0.3s;
  list-style: none;
}
.menu li:hover {
  background-color: #adadad67;
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
  .horizontal-menu,
  .login-menu {
    width: min(280px, calc(100vw - 20px));
  }
  .admin-menu {
    width: min(280px, calc(100vw - 20px));
    left: auto;
    right: 0;
    top: 100%;
  }
  .burger_menu{
    display: block;
  }
  .vertical-menu{
    display: none;
  }
  .infobull::after,
  .infobull::before {
    display: none;
  }
  .account-name {
    display: none;
  }
}

</style>
