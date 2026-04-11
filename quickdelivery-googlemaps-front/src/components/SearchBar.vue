<template>
  <div class="search-bar">
    <div class="burger-menu-shell" ref="handleClickOutsideBurgerMenu">
     <a @click="toggleMenu" class="material-symbols-outlined burger_menu">menu</a>
     <div class="menu vertical-menu">
      <ul>
        <span v-if="canSeeHome" class="infobull" :data-tooltip="$t('menuTooltipHome')"><router-link :to="homeRoute"><li class="material-symbols-outlined">home</li></router-link></span>
        <span v-if="canSeeMap" class="infobull" :data-tooltip="$t('menuTooltipMap')"><router-link to="/app"><li class="material-symbols-outlined">map</li></router-link></span>
        <span v-if="canSeeActiveRoute" class="infobull" :data-tooltip="$t('menuTooltipMyRoute')"><router-link to="/myRoute"><li class="material-symbols-outlined">route</li></router-link></span>
        <span v-if="canSeeMyPackages" class="infobull" :data-tooltip="$t('menuTooltipMyPackages')"><router-link to="/myPackages"><li class="material-symbols-outlined">deployed_code_account</li></router-link></span>
        <span v-if="canSeeCreatePackage" class="infobull" :data-tooltip="$t('menuTooltipNewPackage')"><router-link to="/createPackage"><li class="material-symbols-outlined">box_add</li></router-link></span>
      </ul>
     </div>
      <transition name="fade">
      <div v-if="isActiveMenu" class="menu horizontal-menu" :class="{ 'mobile-drawer': isMobileLayout }">
        <ul>
          <router-link v-if="canSeeHome" @click="closeAllMenus" :to="homeRoute"><li>{{$t('menuHome')}}</li></router-link>
          <router-link v-if="canSeeMap" @click="closeAllMenus" to="/app"><li>{{$t('menuMap')}}</li></router-link>
          <router-link v-if="canSeeActiveRoute" @click="closeAllMenus" to="/myRoute"><li>{{$t('menuMyRoute')}}</li></router-link>
          <router-link v-if="canSeeMyPackages" @click="closeAllMenus" to="/myPackages"><li>{{$t('menuMyPackages')}}</li></router-link>
          <router-link v-if="canSeeCreatePackage" @click="closeAllMenus" to="/createPackage"><li>{{$t('menuNewPackage')}}</li></router-link>
        </ul>
      </div>
      </transition>
    </div>
    <transition name="fade">
      <div v-if="isActiveLoginMenu" class="menu login-menu">
        <ul>
            <router-link v-if="isAdmin" @click="closeAllMenus" to="/dashboard/metrics"><li>{{$t('menuAdminMetrics')}}</li></router-link>
            <router-link v-if="isAdmin" @click="closeAllMenus" to="/dashboard/finance"><li>{{$t('menuAdminFinance')}}</li></router-link>
            <router-link v-if="isAdmin" @click="closeAllMenus" to="/dashboard/courier-penalties"><li>{{$t('menuAdminPenalties')}}</li></router-link>
            <router-link v-if="isAdmin" @click="closeAllMenus" to="/userSignInPage"><li>{{$t('menuUserSignin')}}</li></router-link>
            <router-link v-if="isAdmin" @click="closeAllMenus" to="/usersAccountValidation"><li>{{$t('menuUusersAccountValidation')}}</li></router-link>
            <router-link @click="closeAllMenus" to="/notifications">
              <li class="menu-item-with-badge">
                <span>{{$t('menuNotifications')}}</span>
                <span v-if="unreadNotificationsCount" class="menu-badge">{{ unreadNotificationsCount }}</span>
              </li>
            </router-link>
            <li v-if="canInstallPwa" @click="installPwa">{{$t('menuInstallApp')}}</li>
            <router-link @click="closeAllMenus" to="/userAccount"><li>{{$t('menuUuserAccount')}}</li></router-link>
            <li @click="logoutUser">{{$t('menuUserLogout')}}</li>
        </ul>
      </div>
    </transition>

    <div class="search-bar-actions">
      <span class="infobull account-trigger" :data-tooltip="$t('menuTooltipAccount')" ref="handleClickOutsideUserMenu">
        <a class="account-link" @click="loginMenu">
          <span class="material-symbols-outlined">person</span>
          <span v-if="connectedUserFullName" class="account-name">{{ connectedUserFullName }}</span>
          <span v-if="unreadNotificationsCount" class="account-badge">{{ unreadNotificationsCount }}</span>
        </a>
      </span>
    </div>
  </div>
</template>

<script>
import http from '@/config/httpInterceptor';
import { isMobileCapacitorRuntime } from '@/config/network';
import { getCurrentUserRoles, logout, resolveLandingPathForCurrentUser } from '@/config/auth';

const MOBILE_DEVICE_STORAGE_KEY = 'quickdelivery.mobileDeviceId';

export default {
  data() {
    return {
      isActiveMenu: false,
      isActiveLoginMenu: false,
      isActiveAdminMenu: false,
      viewportWidth: window.innerWidth,
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
    canSeeActiveRoute() {
      return this.isLivreur || this.isAdmin;
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
    hasAuthenticatedMenu() {
      return this.isAdmin || this.isClient || this.isLivreur;
    },
    isMobileLayout() {
      return this.viewportWidth <= 580;
    },
    homeRoute() {
      return resolveLandingPathForCurrentUser();
    },
  },
  mounted() {
    window.addEventListener('click', this.handleClickOutsideUserMenu);
    window.addEventListener('click', this.handleClickOutsideBurgerMenu);
    window.addEventListener('resize', this.handleResize);
  },
  beforeUnmount() {
    window.removeEventListener('click', this.handleClickOutsideUserMenu);
    window.removeEventListener('click', this.handleClickOutsideBurgerMenu);
    window.removeEventListener('resize', this.handleResize);
  },
  methods: {
    handleResize() {
      this.viewportWidth = window.innerWidth;
      if (!this.isMobileLayout) {
        return;
      }
      this.isActiveLoginMenu = false;
    },
    closeAllMenus() {
      this.isActiveMenu = false;
      this.isActiveLoginMenu = false;
    },
    toggleMenu() {
      this.isActiveMenu = !this.isActiveMenu;
      this.isActiveLoginMenu = false;
      this.$store.commit('updateLocation', 'other');
    },
    loginMenu() {
      this.isActiveLoginMenu = !this.isActiveLoginMenu;
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
    logoutUser() {
      this.closeAllMenus();
      const connectedUserId = this.$store.state.connectedUser?.id;
      const deviceId = typeof window === 'undefined' ? '' : (window.localStorage.getItem(MOBILE_DEVICE_STORAGE_KEY) || '');
      if (isMobileCapacitorRuntime() && connectedUserId && deviceId) {
        http.delete(
          `${this.$i18n.t('rootURL')}devices/unregister?userId=${encodeURIComponent(connectedUserId)}&deviceId=${encodeURIComponent(deviceId)}`,
          { silent: true },
        ).catch((error) => {
          console.warn('Unable to unregister mobile device during logout:', error);
        });
      }
      this.$store.commit('resetConnectedUser');
      logout();
    },
    installPwa() {
      this.closeAllMenus();
      window.dispatchEvent(new CustomEvent('qd-install-pwa'));
    },
    installPwaFromBurger() {
      this.closeAllMenus();
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
  padding: max(10px, env(safe-area-inset-top, 0px)) 28px 0;
  background: var(--qd-bg) !important;
  border-bottom: 1px solid var(--qd-border);
  position: relative;
  z-index: 6;
  gap: 12px;
}
.burger-menu-shell {
  display: flex;
  align-items: center;
  min-width: 0;
}
.material-symbols-outlined {
  cursor: pointer;
  font-variation-settings:
  'FILL' 0,
  'wght' 400,
  'GRAD' 0,
  'opsz' 24;
}
.vertical-menu ul {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  list-style: none;
  padding: 0;
  margin: 0;
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
  background: var(--qd-text);
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
  border-color: transparent transparent var(--qd-text) transparent;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.2s ease;
  z-index: 19;
}
.infobull:hover::after,
.infobull:hover::before {
  opacity: 1;
}
.account-trigger, 
.admin-trigger, 
.admin-menu-wrapper {
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
  background: var(--qd-primary-soft);
  border: 1px solid var(--qd-primary);
}
.admin-link .material-symbols-outlined,
.admin-link .account-name {
  color: var(--qd-primary-dark);
}
.account-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--qd-text);
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
  background: var(--qd-primary);
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
.login-menu {
  position: absolute;
  background-color: var(--qd-surface-strong);
  border: 1px solid var(--qd-border);
  border-radius: 12px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.12);
  max-width: calc(100vw - 20px);
  z-index: 10;
}
.horizontal-menu {
  top: 100%;
  left: 10px;
}
.login-menu {
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
  list-style: none;
}
.menu a {
  display: block;
  color: var(--qd-text);
  text-decoration: none;
}
.menu li {
  padding: 10px 15px;
  cursor: pointer;
  transition: all 0.3s;
}
.menu li:hover {
  background-color: var(--qd-primary-soft);
}
.fade-enter-active, .fade-leave-active {
  transition: opacity 300ms;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
.burger_menu {
  display: none;
}
.search-bar-actions {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}
@media screen and (max-width: 580px) {
  .search-bar {
    padding: max(4px, env(safe-area-inset-top, 0px)) 10px 0;
    gap: 4px;
    min-height: calc(31px + env(safe-area-inset-top, 0px));
    align-items: flex-end;
  }
  .horizontal-menu,
  .login-menu {
    width: min(280px, calc(100vw - 20px));
  }
  .horizontal-menu.mobile-drawer {
    position: fixed;
    top: calc(env(safe-area-inset-top, 0px) + 12px);
    left: max(10px, env(safe-area-inset-left, 0px) + 10px);
    width: min(320px, calc(100vw - 20px - env(safe-area-inset-left, 0px) - env(safe-area-inset-right, 0px)));
    max-height: calc(100dvh - env(safe-area-inset-top, 0px) - 24px);
    overflow-y: auto;
    z-index: 15;
  }
  .burger_menu {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 30px;
    height: 30px;
    border-radius: 8px;
    background: var(--qd-bg-alt);
    border: 1px solid var(--qd-border);
    color: var(--qd-text);
    font-size: 20px;
  }
  .vertical-menu {
    display: none;
  }
  .account-name {
    display: none;
  }
}
</style>
