import store from './store';
import http from './httpInterceptor';
import { getCurrentUserIdentity, getCurrentUserRoles, hasValidAccessToken } from './auth';
import i18n from './i18n';

function buildFallbackConnectedUser(identity, roles) {
  return {
    id: null,
    firstName: '',
    lastName: '',
    email: identity?.email || '',
    type: '',
    roles,
    loaded: true,
  };
}

export async function hydrateConnectedUser() {
  if (!hasValidAccessToken()) {
    store.commit('resetConnectedUser');
    return null;
  }

  const identity = getCurrentUserIdentity();
  const roles = getCurrentUserRoles();
  const email = identity?.email;

  if (!email) {
    const fallbackUser = buildFallbackConnectedUser(identity, roles);
    store.commit('updateConnectedUser', fallbackUser);
    return fallbackUser;
  }

  try {
    const response = await http.get(`${i18n.global.t('userRootURL')}${i18n.global.t('getUserByEmail')}${encodeURIComponent(email)}`);
    const user = response?.data;

    const connectedUser = {
      id: user?.id ?? null,
      firstName: user?.firstName || '',
      lastName: user?.lastName || '',
      email: user?.emailAddress || email,
      type: user?.type || '',
      roles,
      loaded: true,
    };

    store.commit('updateConnectedUser', connectedUser);
    return connectedUser;
  } catch (error) {
    const fallbackUser = buildFallbackConnectedUser(identity, roles);
    store.commit('updateConnectedUser', fallbackUser);
    return fallbackUser;
  }
}
