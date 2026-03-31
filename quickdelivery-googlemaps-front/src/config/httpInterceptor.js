import axios from 'axios';
import store from './store';
import { getAccessToken, hasValidAccessToken, redirectToLogin } from './auth';

let authRedirectInProgress = false;
function triggerLoginRedirect() {
  if (authRedirectInProgress) {
    return;
  }
  authRedirectInProgress = true;
  redirectToLogin().finally(() => {
    setTimeout(() => {
      authRedirectInProgress = false;
    }, 2000);
  });
}


const instance = axios.create();

instance.interceptors.request.use(
  function(config) {
    if (!config.silent) {
      store.commit('beginLoading');
    }
    if (!config.skipAuth && hasValidAccessToken()) {
      const token = getAccessToken();
      config.headers = config.headers || {};
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  function(error) {
    if (!error?.config?.silent) {
      store.commit('endLoading');
    }
    const status = error?.response?.status;
    const requestUrl = error?.config?.url || '';
    const skipAuth = !!error?.config?.skipAuth;
    const silent = !!error?.config?.silent;
    const isTokenRequest = requestUrl.includes('/protocol/openid-connect/token');
    const isGatewayBusinessCall = requestUrl.includes('/users/v1/') || requestUrl.includes('/packages/v1/');
    if (!skipAuth && !isTokenRequest && (status === 401 || status === 403)) {
      triggerLoginRedirect();
    } else if (!skipAuth && !isTokenRequest && !status && isGatewayBusinessCall) {
      // Browser-side CORS/network failures on protected calls should also force auth flow.
      triggerLoginRedirect();
    }
    if (!silent) {
      store.commit('updateShowMessage', true);
      store.commit('updateRequestSuccess', false);
      store.commit('updateRequestMessage', 'error');
      setTimeout(() => {
          store.commit('updateShowMessage', false);
      }, 9000);
    }
    return Promise.reject(error);
  }
);

// Ajouter un intercepteur pour les réponses
instance.interceptors.response.use(
  function(response) {
    if (!response.config.silent) {
      store.commit('endLoading');
    }
    if (!response.config.silent && (response.config.method === 'post' || response.config.method === 'put')) {
        store.commit('updateShowMessage', true);
        store.commit('updateRequestSuccess', true);
        store.commit('updateRequestMessage', 'success');
          setTimeout(() => {
            store.commit('updateShowMessage', false);
          }, 9000);
      }
    return response;
  },
  function(error) {
    if (!error?.config?.silent) {
      store.commit('endLoading');
      store.commit('updateShowMessage', true);
      store.commit('updateRequestSuccess', false);
      store.commit('updateRequestMessage', 'error');
      setTimeout(() => {
          store.commit('updateShowMessage', false);
      }, 9000);
    }
    return Promise.reject(error);
  }
);

export default instance;
