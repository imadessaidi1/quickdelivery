import store from './store';
import http from './httpInterceptor';
import { getCurrentUserIdentity, getCurrentUserRoles, hasValidAccessToken } from './auth';
import i18n from './i18n';

let hydrateConnectedUserPromise = null;

function buildFallbackConnectedUser(identity, roles) {
  return {
    id: null,
    firstName: '',
    lastName: '',
    email: identity?.email || '',
    emailAddress: identity?.email || '',
    type: '',
    sex: '',
    birthDate: null,
    phone: '',
    addressAuto: '',
    deliveryMode: '',
    primaryVehicleType: '',
    personalAddress: [],
    vehicles: [],
    documents: [],
    document: {},
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

  const currentConnectedUser = store.state.connectedUser;
  if (currentConnectedUser?.loaded && currentConnectedUser?.email === email) {
    return currentConnectedUser;
  }

  if (hydrateConnectedUserPromise) {
    return hydrateConnectedUserPromise;
  }

  hydrateConnectedUserPromise = (async () => {
    try {
      const response = await http.get(`${i18n.global.t('userRootURL')}${i18n.global.t('getUserByEmail')}${encodeURIComponent(email)}`);
      const user = response?.data;

      const connectedUser = {
        id: user?.id ?? null,
        firstName: user?.firstName || '',
        lastName: user?.lastName || '',
        email: user?.emailAddress || email,
        emailAddress: user?.emailAddress || email,
        type: user?.type || '',
        sex: user?.sex || '',
        birthDate: user?.birthDate || null,
        phone: user?.phone || '',
        addressAuto: user?.addressAuto || '',
        deliveryMode: user?.deliveryMode || '',
        primaryVehicleType: Array.isArray(user?.vehicles) ? (user.vehicles.find((vehicle) => vehicle?.type)?.type || '') : '',
        personalAddress: Array.isArray(user?.personalAddress) ? user.personalAddress : [],
        vehicles: Array.isArray(user?.vehicles) ? user.vehicles : [],
        documents: Array.isArray(user?.documents) ? user.documents : [],
        document: user?.document || {},
        roles,
        loaded: true,
      };

      store.commit('updateConnectedUser', connectedUser);
      return connectedUser;
    } catch (error) {
      const fallbackUser = buildFallbackConnectedUser(identity, roles);
      store.commit('updateConnectedUser', fallbackUser);
      return fallbackUser;
    } finally {
      hydrateConnectedUserPromise = null;
    }
  })();

  return hydrateConnectedUserPromise;
}
