// store.js
import { createStore } from 'vuex';

const MAX_NOTIFICATIONS = 100;

function notificationStorageKey(userId) {
  return `quickdelivery.notifications.${userId}`;
}

function loadNotificationsForUser(userId) {
  if (!userId || typeof window === 'undefined') {
    return [];
  }

  try {
    const rawValue = window.localStorage.getItem(notificationStorageKey(userId));
    const parsedValue = rawValue ? JSON.parse(rawValue) : [];
    return Array.isArray(parsedValue) ? parsedValue : [];
  } catch (error) {
    console.warn('Unable to load notifications from storage:', error);
    return [];
  }
}

function saveNotificationsForUser(userId, notifications) {
  if (!userId || typeof window === 'undefined') {
    return;
  }

  try {
    window.localStorage.setItem(notificationStorageKey(userId), JSON.stringify(notifications));
  } catch (error) {
    console.warn('Unable to save notifications to storage:', error);
  }
}

export default createStore({
  state: {
      isLoading: false,
      showMessage: false,
      requestSuccess: false,
      requestMessage: '',
      notifications: [],
      package_: {
        id: null,
        version: null,
        creationDate: null,
        reference: '',
        height: 0,
        width: 0,
        depth: 0,
        weight: 0,
        pictureURL: '',
        status: '',
        deliveryPrice: null,
        senderID: null,
        packageReservations: [],
        addresses: [{
        firstName: '',
        lastName: '',
        line1: '',
        line2: '',
        town: '',
        zipCode: '',
        country: '',
        floor: 0,
        dateTime: null,
        email: '',
        phone: '',
        type: 'DEPARTURE',
        addressAuto: '',
        latitude: 0,
        longitude: 0,
      },
      {
        firstName: '',
        lastName: '',
        line1: '',
        line2: '',
        town: '',
        zipCode: '',
        country: '',
        floor: 0,
        dateTime: null,
        email: '',
        phone: '',
        type: 'ARRIVAL',
        addressAuto: '',
        latitude: 0,
        longitude: 0,
      }],
      documentS: null,
      lastPositionLatitude: null,
      lastPositionLongitude: null,
    },
    documentS: [],
    connectedUser: {
      id: null,
      firstName: '',
      lastName: '',
      email: '',
      type: '',
      roles: [],
      loaded: false,
    },
    user: {
                id: null,
                version: null,
                type: 'DELIVERY_PERSON',
                firstName: '',
                lastName: '',
                age: null,
                birthDate: null,
                sex: '',
                emailAddress: '',
                emailAddressValidation: false,
                phone: '',
                phoneValidation: false,
                activeAccount: false,
                password: '',
                passwordConfirmation: '',
                emailAddressConfirmation: '',
                phoneConfirmation: '',
                addressAuto: '',
                personalAddress: [
                  {
                    id: null,
                    version: null,
                    firstName: '',
                    lastName: '',
                    line1: '',
                    line2: '',
                    town: '',
                    zipCode: '',
                    country: '',
                    floor: 0,
                    dateTime: null,
                    type: 'RESIDENCE',
                    latitude: 0,
                    longitude: 0,
                    email: '',
                    phone: '',
                  },
                ],
                documents: [],
                paymentModes: {
                    CREDIT_CARD: {
                                     holderNam: '',
                                     cardNumber: '',
                                     expiryDate: '',
                                     cvv: '',
                                    },
                    IBAN: {
                             iban: '',
                             bic: '',
                            },
                },
              },
              vehicle: {
                  registrationNumber: '',
                  brand: '',
                  model: '',
                  energyType: '',
                  vehicleDocuments: null,
              },
              userDocuments: [],
              vehicleDocuments: [],
              userRIB: {},
              packagesLastPosition: {},
              location: 'mapPage',
              mapSearchRadius: 10000,
  },
  mutations: {
      updatePackage(state, updatedPackage) {
        state.package_ = updatedPackage;
      },
      updatePackageLastPosition(state, payload) {
        if (!payload) {
          return;
        }

        if (payload.packageReference) {
          state.packagesLastPosition = {
            ...state.packagesLastPosition,
            [payload.packageReference]: payload.position,
          };
          return;
        }

        state.packagesLastPosition = payload;
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
      updateUser(state, updatedUser) {
        state.user = updatedUser;
      },
      updateVehicle(state, updatedVehicle) {
        state.vehicle = updatedVehicle;
      },
      updateUserDocuments(state, updatedUserDocuments) {
        state.userDocuments = updatedUserDocuments;
      },
      updateVehicleDocuments(state, updatedVehicleDocuments) {
        state.vehicleDocuments = updatedVehicleDocuments;
      },
      updateConnectedUser(state, connectedUser) {
        state.connectedUser = connectedUser;
        state.notifications = loadNotificationsForUser(connectedUser?.id);
      },
      resetConnectedUser(state) {
        state.connectedUser = {
          id: null,
          firstName: '',
          lastName: '',
          email: '',
          type: '',
          roles: [],
          loaded: false,
        };
        state.notifications = [];
      },
      updateLocation(state, updateLocation) {
        state.location = updateLocation;
      },
      updateMapSearchRadius(state, radius) {
        state.mapSearchRadius = radius;
      },
      pushNotification(state, notification) {
        const formattedNotification = {
          id: notification.id || `${Date.now()}-${Math.random().toString(36).slice(2, 10)}`,
          type: notification.type || 'GENERIC_NOTIFICATION',
          from: notification.from ?? null,
          to: notification.to ?? null,
          message: notification.message || '',
          url: notification.url || '',
          receivedAt: notification.receivedAt || new Date().toISOString(),
          read: false,
        };

        state.notifications = [formattedNotification, ...state.notifications].slice(0, MAX_NOTIFICATIONS);
        saveNotificationsForUser(state.connectedUser?.id, state.notifications);
      },
      markNotificationRead(state, notificationId) {
        state.notifications = state.notifications.map((notification) => (
          notification.id === notificationId ? { ...notification, read: true } : notification
        ));
        saveNotificationsForUser(state.connectedUser?.id, state.notifications);
      },
      markAllNotificationsRead(state) {
        state.notifications = state.notifications.map((notification) => ({
          ...notification,
          read: true,
        }));
        saveNotificationsForUser(state.connectedUser?.id, state.notifications);
      },
      clearNotifications(state) {
        state.notifications = [];
        saveNotificationsForUser(state.connectedUser?.id, state.notifications);
      },
    },
  actions: {},
  getters: {
    unreadNotificationsCount(state) {
      return state.notifications.filter((notification) => !notification.read).length;
    },
  },
});
