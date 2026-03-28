import { futureIso, uniqueSuffix } from './utils.js';

function buildAddress(type, overrides = {}) {
  if (type === 'DEPARTURE') {
    return {
      firstName: 'Perf',
      lastName: 'Sender',
      line1: '10 Rue de Paris',
      line2: '',
      town: 'Paris',
      zipCode: '75011',
      country: 'France',
      floor: 1,
      hasElevator: true,
      dateTime: futureIso(90),
      email: 'perf.sender@example.com',
      phone: '0600000001',
      type,
      latitude: 48.8591,
      longitude: 2.3785,
      addressAuto: '10 Rue de Paris, 75011 Paris, France',
      ...overrides,
    };
  }

  return {
    firstName: 'Perf',
    lastName: 'Receiver',
    line1: '25 Avenue de la Republique',
    line2: '',
    town: 'Paris',
    zipCode: '75011',
    country: 'France',
    floor: 3,
    hasElevator: false,
    dateTime: futureIso(150),
    email: 'perf.receiver@example.com',
    phone: '0600000002',
    type,
    latitude: 48.8673,
    longitude: 2.3691,
    addressAuto: '25 Avenue de la Republique, 75011 Paris, France',
    ...overrides,
  };
}

export function buildEstimatePayload() {
  return {
    reference: `PERF-${uniqueSuffix()}`,
    height: 20,
    width: 20,
    depth: 20,
    weight: 2,
    deliverySpeed: 'STANDARD',
    insuranceSelected: true,
    declaredValue: 50,
    guestMode: true,
    addresses: [
      buildAddress('DEPARTURE'),
      buildAddress('ARRIVAL'),
    ],
  };
}

export function buildCreateFormData(locale = 'fr') {
  const packageDTO = buildEstimatePayload();
  return {
    packageDTO: JSON.stringify(packageDTO),
    locale,
  };
}
