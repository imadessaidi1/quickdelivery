const DELIVERY_MODE_RULES = {
  HEAVY: {
    label: 'deliveryModeHeavy',
    userDocuments: ['ID', 'PICTURE', 'DRIVER_LICENCE', 'USER_COMPANY_EXTRACT', 'USER_COMPANY_INSURANCE'],
    vehicleDocuments: ['GRAY_CARD', 'INSURANCE'],
    requiresVehicleInfo: true,
    maxConcurrentPackages: 999,
    searchRadius: 100000, // 100km
    allowedPackageSizes: ['SMALL', 'MEDIUM', 'LARGE', 'EXTRA_LARGE'],
  },
  TRUCK: {
    label: 'deliveryModeTruck',
    userDocuments: ['ID', 'PICTURE', 'DRIVER_LICENCE', 'USER_COMPANY_EXTRACT', 'USER_COMPANY_INSURANCE'],
    vehicleDocuments: ['GRAY_CARD', 'INSURANCE'],
    requiresVehicleInfo: true,
    maxConcurrentPackages: 150,
    searchRadius: 50000, // 50km
    allowedPackageSizes: ['SMALL', 'MEDIUM', 'LARGE', 'EXTRA_LARGE'],
  },
  VAN: {
    label: 'deliveryModeVan',
    userDocuments: ['ID', 'PICTURE', 'DRIVER_LICENCE', 'USER_COMPANY_EXTRACT', 'USER_COMPANY_INSURANCE'],
    vehicleDocuments: ['GRAY_CARD', 'INSURANCE'],
    requiresVehicleInfo: true,
    maxConcurrentPackages: 60,
    searchRadius: 30000, // 30km
    allowedPackageSizes: ['SMALL', 'MEDIUM', 'LARGE', 'EXTRA_LARGE'],
  },
  CAR: {
    label: 'deliveryModeCar',
    userDocuments: ['ID', 'PICTURE', 'DRIVER_LICENCE', 'USER_COMPANY_EXTRACT', 'USER_COMPANY_INSURANCE'],
    vehicleDocuments: ['GRAY_CARD', 'INSURANCE'],
    requiresVehicleInfo: true,
    maxConcurrentPackages: 25,
    searchRadius: 15000, // 15km
    allowedPackageSizes: ['SMALL', 'MEDIUM', 'LARGE'],
  },
  SCOOTER: {
    label: 'deliveryModeScooter',
    userDocuments: ['ID', 'PICTURE', 'DRIVER_LICENCE', 'USER_COMPANY_EXTRACT', 'USER_COMPANY_INSURANCE'],
    vehicleDocuments: ['GRAY_CARD', 'INSURANCE'],
    requiresVehicleInfo: true,
    maxConcurrentPackages: 12,
    searchRadius: 8000, // 8km
    allowedPackageSizes: ['SMALL', 'MEDIUM'],
  },
  BIKE: {
    label: 'deliveryModeBike',
    userDocuments: ['ID', 'PICTURE', 'USER_COMPANY_EXTRACT', 'USER_COMPANY_INSURANCE'],
    vehicleDocuments: [],
    requiresVehicleInfo: false,
    maxConcurrentPackages: 6,
    searchRadius: 3000, // 3km
    allowedPackageSizes: ['SMALL'],
  },
  ON_FOOT: {
    label: 'deliveryModeOnFoot',
    userDocuments: ['ID', 'PICTURE', 'USER_COMPANY_EXTRACT', 'USER_COMPANY_INSURANCE'],
    vehicleDocuments: [],
    requiresVehicleInfo: false,
    maxConcurrentPackages: 3,
    searchRadius: 1000, // 1km
    allowedPackageSizes: ['SMALL'],
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
  const mode = resolveDeliveryMode(user);
  return DELIVERY_MODE_RULES[mode] || DELIVERY_MODE_RULES[DEFAULT_DELIVERY_MODE];
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

export function getDeliveryModeCapacity(user) {
  return getDeliveryModeRules(user).maxConcurrentPackages;
}

export function getDeliveryModeSearchRadius(user) {
  return getDeliveryModeRules(user).searchRadius;
}

export function isPackageSizeAllowed(user, size) {
  const allowedSizes = getDeliveryModeRules(user).allowedPackageSizes || [];
  return allowedSizes.includes(size);
}

