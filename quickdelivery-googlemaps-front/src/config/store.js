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

function normalizeNotification(notification) {
  return {
    id: notification.id || notification.notificationId || `${Date.now()}-${Math.random().toString(36).slice(2, 10)}`,
    type: notification.type || notification.eventType || 'GENERIC_NOTIFICATION',
    from: notification.from ?? null,
    to: notification.to ?? null,
    title: notification.title || '',
    message: notification.message || notification.body || '',
    url: notification.url || notification.targetUrl || '',
    payloadJson: notification.payloadJson || '',
    receivedAt: notification.receivedAt || notification.createdAt || new Date().toISOString(),
    read: Boolean(notification.read),
  };
}

function sortNotificationsByDate(notifications) {
  return [...notifications].sort((left, right) => {
    const leftTime = new Date(left.receivedAt || 0).getTime();
    const rightTime = new Date(right.receivedAt || 0).getTime();
    return rightTime - leftTime;
  });
}

export default createStore({
  state: {
      isLoading: false,
      loadingRequestsCount: 0,
      showMessage: false,
      requestSuccess: false,
      requestMessage: '',
      canInstallPwa: false,
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
        deliverySpeed: 'STANDARD',
        insuranceSelected: false,
        declaredValue: null,
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
        hasElevator: null,
        dateTime: null,
        email: '',
        phone: '',
        type: 'DEPARTURE',
        addressAuto: '',
        latitude: null,
        longitude: null,
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
        hasElevator: null,
        dateTime: null,
        email: '',
        phone: '',
        type: 'ARRIVAL',
        addressAuto: '',
        latitude: null,
        longitude: null,
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
      emailAddress: '',
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
                deliveryMode: 'CAR',
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
                    latitude: null,
                    longitude: null,
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
              packagesArround: [],
              location: 'mapPage',
              mapSearchRadius: 10000,
              isUserWithOngoingDelivery: false,
              activeDeliveryRoute: null,
              reservationAvailability: {
                  canReserve: true,
                  activeRouteBlocking: false,
                  capacityReached: false,
                  activeReservations: 0,
                  maxReservations: 0,
                  reason: 'available',
              },
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
      beginLoading(state) {
        state.loadingRequestsCount += 1;
        state.isLoading = state.loadingRequestsCount > 0;
      },
      endLoading(state) {
        state.loadingRequestsCount = Math.max(0, state.loadingRequestsCount - 1);
        state.isLoading = state.loadingRequestsCount > 0;
      },
      resetLoading(state) {
        state.loadingRequestsCount = 0;
        state.isLoading = false;
      },
      updateLoaderStatus(state, isLoading_) {
        if (isLoading_) {
          state.loadingRequestsCount += 1;
        } else {
          state.loadingRequestsCount = Math.max(0, state.loadingRequestsCount - 1);
        }
        state.isLoading = state.loadingRequestsCount > 0;
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
      updateCanInstallPwa(state, canInstallPwa) {
        state.canInstallPwa = !!canInstallPwa;
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
      setNotifications(state, notifications) {
        state.notifications = sortNotificationsByDate((notifications || []).map(normalizeNotification)).slice(0, MAX_NOTIFICATIONS);
        saveNotificationsForUser(state.connectedUser?.id, state.notifications);
      },
      resetConnectedUser(state) {
        state.connectedUser = {
          id: null,
          firstName: '',
          lastName: '',
          email: '',
          emailAddress: '',
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
          roles: [],
          loaded: false,
        };
        state.notifications = [];
        state.isUserWithOngoingDelivery = false;
        state.activeDeliveryRoute = null;
        state.reservationAvailability = {
          canReserve: true,
          activeRouteBlocking: false,
          capacityReached: false,
          activeReservations: 0,
          maxReservations: 0,
          reason: 'available',
        };
      },
      updateLocation(state, updateLocation) {
        state.location = updateLocation;
      },
      updateMapSearchRadius(state, radius) {
        state.mapSearchRadius = radius;
      },
      updatePackagesArround(state, packages) {
        state.packagesArround = Array.isArray(packages) ? packages : [];
      },
      setOngoingDeliveryState(state, payload) {
        state.isUserWithOngoingDelivery = Boolean(payload?.isUserWithOngoingDelivery);
        state.activeDeliveryRoute = payload?.activeDeliveryRoute || null;
      },
      setActiveDeliveryRoute(state, activeDeliveryRoute) {
        state.activeDeliveryRoute = activeDeliveryRoute || null;
      },
      setReservationAvailability(state, reservationAvailability) {
        state.reservationAvailability = {
          canReserve: reservationAvailability?.canReserve !== false,
          activeRouteBlocking: Boolean(reservationAvailability?.activeRouteBlocking),
          capacityReached: Boolean(reservationAvailability?.capacityReached),
          activeReservations: Number(reservationAvailability?.activeReservations || 0),
          maxReservations: Number(reservationAvailability?.maxReservations || 0),
          reason: reservationAvailability?.reason || 'available',
        };
      },
      patchPackageSoftLock(state, payload) {
        if (!payload?.packageId) {
          return;
        }
        state.packagesArround = (state.packagesArround || []).map((pkg) => (
          pkg.id === payload.packageId
            ? { ...pkg, isSoftLockedBy: payload.lockedBy || null, softLockExpiresAt: payload.softLockExpiresAt || null }
            : pkg
        ));
      },
      pushNotification(state, notification) {
        const formattedNotification = normalizeNotification(notification);
        const existingIndex = state.notifications.findIndex((item) => `${item.id}` === `${formattedNotification.id}`);
        if (existingIndex >= 0) {
          state.notifications.splice(existingIndex, 1, {
            ...state.notifications[existingIndex],
            ...formattedNotification,
          });
        } else {
          state.notifications = [formattedNotification, ...state.notifications];
        }
        state.notifications = sortNotificationsByDate(state.notifications).slice(0, MAX_NOTIFICATIONS);
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
