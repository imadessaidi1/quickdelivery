import axios from 'axios';
import store from './store';


const instance = axios.create();

instance.interceptors.request.use(
  function(config) {
    store.commit('updateLoaderStatus', true);
    return config;
  },
  function(error) {
    store.commit('updateLoaderStatus', false);
    store.commit('updateShowMessage', true);
    store.commit('updateRequestSuccess', false);
    store.commit('updateRequestMessage', 'error');
    setTimeout(() => {
        store.commit('updateShowMessage', false);
    }, 5000);
    return Promise.reject(error);
  }
);

// Ajouter un intercepteur pour les réponses
instance.interceptors.response.use(
  function(response) {
    store.commit('updateLoaderStatus', false);
    if (response.config.method === 'post' || response.config.method === 'put') {
        store.commit('updateShowMessage', true);
        store.commit('updateRequestSuccess', true);
        store.commit('updateRequestMessage', 'success');
          setTimeout(() => {
            store.commit('updateShowMessage', false);
          }, 5000);
      }
    return response;
  },
  function(error) {
    store.commit('updateLoaderStatus', false);
    store.commit('updateShowMessage', true);
    store.commit('updateRequestSuccess', false);
    store.commit('updateRequestMessage', 'error');
    setTimeout(() => {
        store.commit('updateShowMessage', false);
    }, 5000);
    return Promise.reject(error);
  }
);

export default instance;
