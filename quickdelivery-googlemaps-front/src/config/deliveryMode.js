const DELIVERY_MODE_RULES = {
  CAR: {
    userDocuments: ['ID', 'PICTURE', 'DRIVER_LICENCE', 'USER_COMPANY_EXTRACT', 'USER_COMPANY_INSURANCE'],
    vehicleDocuments: ['GRAY_CARD', 'INSURANCE'],
    requiresVehicleInfo: true,
  },
  SCOOTER: {
    userDocuments: ['ID', 'PICTURE', 'DRIVER_LICENCE', 'USER_COMPANY_EXTRACT', 'USER_COMPANY_INSURANCE'],
    vehicleDocuments: ['GRAY_CARD', 'INSURANCE'],
    requiresVehicleInfo: true,
  },
  BIKE: {
    userDocuments: ['ID', 'PICTURE', 'USER_COMPANY_EXTRACT', 'USER_COMPANY_INSURANCE'],
    vehicleDocuments: [],
    requiresVehicleInfo: false,
  },
  ON_FOOT: {
    userDocuments: ['ID', 'PICTURE', 'USER_COMPANY_EXTRACT', 'USER_COMPANY_INSURANCE'],
    vehicleDocuments: [],
    requiresVehicleInfo: false,
  },
};

export const DEFAULT_DELIVERY_MODE = 'CAR';

export function resolveDeliveryMode(user) {
  if (user?.type !== 'DELIVERY_PERSON') {
    return '';
  }
  return user?.deliveryMode || DEFAULT_DELIVERY_MODE;
}

export function getDeliveryModeRules(user) {
  return DELIVERY_MODE_RULES[resolveDeliveryMode(user)] || DELIVERY_MODE_RULES[DEFAULT_DELIVERY_MODE];
}

export function getRequiredUserDocuments(user) {
  return getDeliveryModeRules(user).userDocuments;
}

export function getRequiredVehicleDocuments(user) {
  return getDeliveryModeRules(user).vehicleDocuments;
}

export function requiresVehicleDetails(user) {
  return getDeliveryModeRules(user).requiresVehicleInfo;
}

