import { uniqueSuffix } from './utils.js';

export function buildCustomerUserPayload() {
  const suffix = uniqueSuffix().replace(/[^a-zA-Z0-9]/g, '').toLowerCase();
  const email = `perf.customer.${suffix}@example.com`;
  const phone = `+336${suffix.slice(-8).padStart(8, '0')}`;

  return {
    type: 'CUSTOMER',
    firstName: 'Perf',
    lastName: 'Customer',
    birthDate: '1990-01-01T00:00:00.000Z',
    sex: 'MAL',
    emailAddress: email,
    emailAddressConfirmation: email,
    phone,
    phoneConfirmation: phone,
    activeAccount: false,
    password: 'PerfPass123@',
    passwordConfirmation: 'PerfPass123@',
    deliveryMode: null,
    personalAddress: [],
    paymentModes: {
      CREDIT_CARD: {
        holderNam: 'Perf Customer',
        cardNumber: '4974018720329404',
        expiryDate: '07/2028',
        cvv: '793',
      },
      IBAN: {
        iban: '',
        bic: '',
      },
    },
    document: {},
  };
}

export function buildEmptyVehiclePayload() {
  return {};
}

export function buildCourierUserPayload() {
  const suffix = uniqueSuffix().replace(/[^a-zA-Z0-9]/g, '').toLowerCase();
  const email = `perf.courier.${suffix}@example.com`;
  const phone = `+336${suffix.slice(-8).padStart(8, '0')}`;

  return {
    type: 'DELIVERY_PERSON',
    firstName: 'Perf',
    lastName: 'Courier',
    birthDate: '1990-01-01T00:00:00.000Z',
    sex: 'MAL',
    emailAddress: email,
    emailAddressConfirmation: email,
    phone,
    phoneConfirmation: phone,
    activeAccount: false,
    emailAddressValidation: false,
    password: 'PerfPass123@',
    passwordConfirmation: 'PerfPass123@',
    deliveryMode: 'CAR',
    addressAuto: '10 avenue de la Republique, 75011 Paris, France',
    personalAddress: [],
    paymentModes: {
      CREDIT_CARD: {
        holderNam: 'Perf Courier',
        cardNumber: '4974018720329404',
        expiryDate: '07/2028',
        cvv: '793',
      },
      IBAN: {
        iban: '',
        bic: '',
      },
    },
    document: {},
  };
}

export function buildCourierVehiclePayload() {
  const suffix = uniqueSuffix().replace(/[^a-zA-Z0-9]/g, '').toUpperCase();
  return {
    registrationNumber: `AB-${suffix.slice(-3)}-CD`,
    brand: 'Peugeot',
    model: '208',
    energyType: 'ESSENCE',
  };
}
