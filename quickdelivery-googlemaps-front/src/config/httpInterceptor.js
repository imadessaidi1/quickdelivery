import { CapacitorHttp } from '@capacitor/core';  // Importer Capacitor HTTP
import store from './store';

// Créer une instance pour centraliser les appels API
const instance = {
  // Méthode GET pour effectuer des requêtes GET
  async get(url, config = {}) {
    store.commit('updateLoaderStatus', true);  // Afficher le loader
    try {
      const response = await CapacitorHttp.get({
        url,
        headers: {
          'Accept': 'application/json',
          ...config.headers,  // Ajouter des headers supplémentaires si nécessaire
        },
        sslPinning: false,  // Accepter les certificats SSL auto-signés
      });
      store.commit('updateLoaderStatus', false);  // Cacher le loader
      return response;
    } catch (error) {
      store.commit('updateLoaderStatus', false);  // Cacher le loader
      store.commit('updateShowMessage', true);  // Afficher le message d'erreur
      store.commit('updateRequestSuccess', false);
      store.commit('updateRequestMessage', 'error');
      setTimeout(() => {
        store.commit('updateShowMessage', false);  // Cacher le message d'erreur après 9 secondes
      }, 9000);
      return Promise.reject(error);
    }
  },

  // Méthode POST pour effectuer des requêtes POST
  async postRequest(url, data, config = {}) {
    store.commit('updateLoaderStatus', true);  // Afficher le loader
    try {
      const response = await CapacitorHttp.post({
        url,
        data,
        headers: {
          'Accept': 'application/json',
          ...config.headers,  // Ajouter des headers supplémentaires si nécessaire
        },
        sslPinning: false,  // Accepter les certificats SSL auto-signés
      });
      store.commit('updateLoaderStatus', false);  // Cacher le loader
      return response;
    } catch (error) {
      store.commit('updateLoaderStatus', false);  // Cacher le loader
      store.commit('updateShowMessage', true);  // Afficher le message d'erreur
      store.commit('updateRequestSuccess', false);
      store.commit('updateRequestMessage', 'error');
      setTimeout(() => {
        store.commit('updateShowMessage', false);  // Cacher le message d'erreur après 9 secondes
      }, 9000);
      return Promise.reject(error);
    }
  }
};

export default instance;  // S'assurer que l'instance est correctement exportée
