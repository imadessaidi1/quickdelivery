import { futureIso, uniqueSuffix } from './utils.js';

const LIMEIL_CENTER = {
  line1: '3 Rue Pasteur',
  line2: '',
  town: 'Limeil-Brevannes',
  zipCode: '94450',
  country: 'France',
  latitude: 48.74845,
  longitude: 2.48856,
  addressAuto: '3 Rue Pasteur, 94450 Limeil-Brevannes, France',
};

const DEFAULT_DEPARTURE = {
  firstName: 'Perf',
  lastName: 'Sender',
  floor: 1,
  hasElevator: true,
  dateTime: futureIso(90),
  email: 'perf.sender@example.com',
  phone: '0600000001',
  ...LIMEIL_CENTER,
};

const ARRIVAL_POOL = [
  {
    line1: '2 Avenue du General de Gaulle',
    line2: '',
    town: 'Creteil',
    zipCode: '94000',
    country: 'France',
    latitude: 48.7812,
    longitude: 2.4545,
    addressAuto: '2 Avenue du General de Gaulle, 94000 Creteil, France',
  },
  {
    line1: '14 Cours de Vincennes',
    line2: '',
    town: 'Paris',
    zipCode: '75012',
    country: 'France',
    latitude: 48.8447,
    longitude: 2.4084,
    addressAuto: '14 Cours de Vincennes, 75012 Paris, France',
  },
  {
    line1: '1 Parvis de la Defense',
    line2: '',
    town: 'Courbevoie',
    zipCode: '92400',
    country: 'France',
    latitude: 48.8919,
    longitude: 2.2387,
    addressAuto: '1 Parvis de la Defense, 92400 Courbevoie, France',
  },
];

const VISIBLE_RING_DEPARTURES = {
  10: {
    ...LIMEIL_CENTER,
  },
  20: {
    line1: '14 Cours de Vincennes',
    line2: '',
    town: 'Paris',
    zipCode: '75012',
    country: 'France',
    latitude: 48.8447,
    longitude: 2.4084,
    addressAuto: '14 Cours de Vincennes, 75012 Paris, France',
  },
  30: {
    line1: '1 Parvis de la Defense',
    line2: '',
    town: 'Courbevoie',
    zipCode: '92400',
    country: 'France',
    latitude: 48.8919,
    longitude: 2.2387,
    addressAuto: '1 Parvis de la Defense, 92400 Courbevoie, France',
  },
};

function buildAddress(type, overrides = {}) {
  if (type === 'DEPARTURE') {
    return {
      ...DEFAULT_DEPARTURE,
      type,
      ...overrides,
    };
  }

  return {
    firstName: 'Perf',
    lastName: 'Receiver',
    floor: 3,
    hasElevator: false,
    dateTime: futureIso(150),
    email: 'perf.receiver@example.com',
    phone: '0600000002',
    type,
    ...ARRIVAL_POOL[0],
    ...overrides,
  };
}

function seededChoice(seed, values) {
  if (!Array.isArray(values) || values.length === 0) {
    return null;
  }
  const raw = `${seed ?? uniqueSuffix()}`;
  let hash = 0;
  for (let index = 0; index < raw.length; index += 1) {
    hash = ((hash << 5) - hash) + raw.charCodeAt(index);
    hash |= 0;
  }
  return values[Math.abs(hash) % values.length];
}

export function buildEstimatePayload(options = {}) {
  const seed = options.seed || uniqueSuffix();
  const arrival = options.arrival || seededChoice(seed, ARRIVAL_POOL);
  const departure = options.departure || DEFAULT_DEPARTURE;
  const referencePrefix = options.referencePrefix || 'PERF';

  return {
    reference: `${referencePrefix}-${seed}`,
    height: 20,
    width: 20,
    depth: 20,
    weight: 2,
    deliverySpeed: 'STANDARD',
    insuranceSelected: true,
    declaredValue: 50,
    guestMode: true,
    addresses: [
      buildAddress('DEPARTURE', departure),
      buildAddress('ARRIVAL', arrival),
    ],
  };
}

export function buildCreatePayload() {
  return {
    ...buildEstimatePayload(),
    status: 'PAYMENTPENDING',
  };
}

export function buildCreateFormData(locale = 'fr') {
  const packageDTO = buildCreatePayload();
  return {
    packageDTO: JSON.stringify(packageDTO),
    locale,
  };
}

export function buildVisibleSeedPayload(radiusKm, index = 0) {
  const departure = VISIBLE_RING_DEPARTURES[radiusKm] || DEFAULT_DEPARTURE;
  let arrival = ARRIVAL_POOL[index % ARRIVAL_POOL.length];
  if (arrival.addressAuto === departure.addressAuto) {
    arrival = ARRIVAL_POOL[(index + 1) % ARRIVAL_POOL.length];
  }

  const seed = `VISIBLE-${radiusKm}-${index}-${uniqueSuffix()}`;
  return {
    ...buildEstimatePayload({
      seed,
      departure,
      arrival,
      referencePrefix: `PERF-VISIBLE-${radiusKm}KM`,
    }),
    status: 'PAYMENTPENDING',
  };
}
