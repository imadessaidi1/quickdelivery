// store.js
import { createStore } from 'vuex';

export default createStore({
  state: {
      isLoading: false,
      showMessage: false,
      requestSuccess: false,
      requestMessage: '',
      package_: {
        id: null,
        version: null,
        creationDate: null,
        height: 0,
        width: 0,
        depth: 0,
        weight: 0,
        pictureURL: "",
        status: "",
        deliveryPrice: null,
        senderID: null,
        packageReservations: [],
        addresses: [{
        firstName: "",
        lastName: "",
        line1: "",
        line2: "",
        town: "",
        zipCode: "",
        country: "",
        floor:0,
        dateTime: null,
        email: "",
        phone: "",
        type: "DEPARTURE",
        latitude: 0,
        longitude: 0,
      },
      {
        firstName: "",
        lastName: "",
        line1: "",
        line2: "",
        town: "",
        zipCode: "",
        country: "",
        floor:0,
        dateTime: null,
        email: "",
        phone: "",
        type: "ARRIVAL",
        latitude: 0,
        longitude: 0,
      }],
      lastPositionLatitude: null,
      lastPositionLongitude: null
    },
    documentS: [],
    connectedUser: {
      firstName: 'Imad',
      lastName: 'ESSAIDI',
      email: 'im.essaidi@gmail.com',
      id: 904,
    },
  },
  mutations: {
      updatePackage(state, updatedPackage) {
        state.package_ = updatedPackage;
      },
      updateDocuments(state, updatedDocuments) {
        state.documentS = updatedDocuments;
      },
      updatePackageDepartureAddress(state, updatedAddress) {
        state.package_.addresses[0] = updatedAddress;
      },
      updatePackageArrivalAddress(state, updatedAddress) {
        state.package_.addresses[1] = updatedAddress;
      },
      updateLoaderStatus(state, isLoading_) {
        state.isLoading = isLoading_;
      },
      updateShowMessage(state, showMessage_) {
        state.showMessage = showMessage_;
      },
      updateRequestSuccess(state, requestSuccess_) {
        state.requestSuccess = requestSuccess_;
      },
      updateRequestMessage(state, requestMessage_) {
        state.requestMessage = requestMessage_;
      },
    },
  actions: {
    // Vous pouvez définir des actions si vous avez besoin d'effectuer des opérations asynchrones
  },
  getters: {
    // Vous pouvez définir des getters pour récupérer des données calculées à partir de l'état
  },
});
