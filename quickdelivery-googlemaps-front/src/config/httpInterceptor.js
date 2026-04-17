import axios from 'axios';
import store from './store';
import { getAccessToken, hasValidAccessToken, redirectToLogin, wasRecentAuthSuccess } from './auth';
import { attachAbortController, releaseAbortController } from './requestControl';
import { resolveBackendErrorMessage } from './backendErrorMessages';

let authRedirectInProgress = false;
function triggerLoginRedirect() {
  if (authRedirectInProgress) {
    return;
  }
  if (wasRecentAuthSuccess()) {
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
    config = attachAbortController(config);
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
    releaseAbortController(error?.config);
    if (axios.isCancel(error) || error?.code === 'ERR_CANCELED') {
      if (!error?.config?.silent) {
        store.commit('endLoading');
      }
      return Promise.reject(error);
    }
    if (!error?.config?.silent) {
      store.commit('endLoading');
    }
    const status = error?.response?.status;
    const requestUrl = error?.config?.url || '';
    const skipAuth = !!error?.config?.skipAuth;
    const silent = !!error?.config?.silent;
    const isTokenRequest = requestUrl.includes('/protocol/openid-connect/token');
    if (!skipAuth && !isTokenRequest && status === 401) {
      triggerLoginRedirect();
    }
    if (!silent) {
      store.commit('updateShowMessage', true);
      store.commit('updateRequestSuccess', false);
      store.commit('updateRequestMessage', resolveBackendErrorMessage(error));
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
    releaseAbortController(response?.config);
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
    releaseAbortController(error?.config);
    if (axios.isCancel(error) || error?.code === 'ERR_CANCELED') {
      if (!error?.config?.silent) {
        store.commit('endLoading');
      }
      return Promise.reject(error);
    }
    if (!error?.config?.silent) {
      store.commit('endLoading');
      store.commit('updateShowMessage', true);
      store.commit('updateRequestSuccess', false);
      store.commit('updateRequestMessage', resolveBackendErrorMessage(error));
      setTimeout(() => {
          store.commit('updateShowMessage', false);
      }, 9000);
    }
    return Promise.reject(error);
  }
);

export default instance;
