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

const FRANCE_CITY_POOL = [
  {
    name: 'Paris',
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
    name: 'Lille',
    line1: '1 Boulevard de Turin',
    line2: '',
    town: 'Lille',
    zipCode: '59800',
    country: 'France',
    latitude: 50.6394,
    longitude: 3.0754,
    addressAuto: '1 Boulevard de Turin, 59800 Lille, France',
  },
  {
    name: 'Strasbourg',
    line1: '1 Place de la Gare',
    line2: '',
    town: 'Strasbourg',
    zipCode: '67000',
    country: 'France',
    latitude: 48.5852,
    longitude: 7.734,
    addressAuto: '1 Place de la Gare, 67000 Strasbourg, France',
  },
  {
    name: 'Nantes',
    line1: '27 Boulevard de Stalingrad',
    line2: '',
    town: 'Nantes',
    zipCode: '44000',
    country: 'France',
    latitude: 47.2173,
    longitude: -1.5417,
    addressAuto: '27 Boulevard de Stalingrad, 44000 Nantes, France',
  },
  {
    name: 'Rennes',
    line1: '19 Place de la Gare',
    line2: '',
    town: 'Rennes',
    zipCode: '35000',
    country: 'France',
    latitude: 48.1035,
    longitude: -1.6723,
    addressAuto: '19 Place de la Gare, 35000 Rennes, France',
  },
  {
    name: 'Bordeaux',
    line1: '1 Rue Charles Domercq',
    line2: '',
    town: 'Bordeaux',
    zipCode: '33800',
    country: 'France',
    latitude: 44.8253,
    longitude: -0.5567,
    addressAuto: '1 Rue Charles Domercq, 33800 Bordeaux, France',
  },
  {
    name: 'Toulouse',
    line1: '64 Boulevard Pierre Semard',
    line2: '',
    town: 'Toulouse',
    zipCode: '31500',
    country: 'France',
    latitude: 43.6112,
    longitude: 1.4544,
    addressAuto: '64 Boulevard Pierre Semard, 31500 Toulouse, France',
  },
  {
    name: 'Montpellier',
    line1: 'Place Auguste Gibert',
    line2: '',
    town: 'Montpellier',
    zipCode: '34000',
    country: 'France',
    latitude: 43.6045,
    longitude: 3.8807,
    addressAuto: 'Place Auguste Gibert, 34000 Montpellier, France',
  },
  {
    name: 'Marseille',
    line1: 'Square Narvik',
    line2: '',
    town: 'Marseille',
    zipCode: '13001',
    country: 'France',
    latitude: 43.3028,
    longitude: 5.3802,
    addressAuto: 'Square Narvik, 13001 Marseille, France',
  },
  {
    name: 'Nice',
    line1: '12 Avenue Thiers',
    line2: '',
    town: 'Nice',
    zipCode: '06000',
    country: 'France',
    latitude: 43.7043,
    longitude: 7.2619,
    addressAuto: '12 Avenue Thiers, 06000 Nice, France',
  },
  {
    name: 'Lyon',
    line1: 'Place Charles Beraudier',
    line2: '',
    town: 'Lyon',
    zipCode: '69003',
    country: 'France',
    latitude: 45.7606,
    longitude: 4.8599,
    addressAuto: 'Place Charles Beraudier, 69003 Lyon, France',
  },
  {
    name: 'Grenoble',
    line1: '1 Place de la Gare',
    line2: '',
    town: 'Grenoble',
    zipCode: '38000',
    country: 'France',
    latitude: 45.1918,
    longitude: 5.714,
    addressAuto: '1 Place de la Gare, 38000 Grenoble, France',
  },
  {
    name: 'Clermont-Ferrand',
    line1: '46 Avenue de l Union Sovietique',
    line2: '',
    town: 'Clermont-Ferrand',
    zipCode: '63000',
    country: 'France',
    latitude: 45.7782,
    longitude: 3.1001,
    addressAuto: '46 Avenue de l Union Sovietique, 63000 Clermont-Ferrand, France',
  },
  {
    name: 'Dijon',
    line1: '31 Cour de la Gare',
    line2: '',
    town: 'Dijon',
    zipCode: '21000',
    country: 'France',
    latitude: 47.322,
    longitude: 5.0342,
    addressAuto: '31 Cour de la Gare, 21000 Dijon, France',
  },
  {
    name: 'Brest',
    line1: '8 Place du 19e Regiment d Infanterie',
    line2: '',
    town: 'Brest',
    zipCode: '29200',
    country: 'France',
    latitude: 48.3876,
    longitude: -4.4801,
    addressAuto: '8 Place du 19e Regiment d Infanterie, 29200 Brest, France',
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

function buildFakePackagePicture() {
  return {
    filename: `package-picture-${uniqueSuffix()}.jpg`,
    contentType: 'image/jpeg',
    content: 'fake-jpeg-content-quickdelivery-package-picture',
  };
}

function buildFakePackageInvoice() {
  return {
    filename: `package-invoice-${uniqueSuffix()}.pdf`,
    contentType: 'application/pdf',
    content: `%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\n% quickdelivery perf package invoice ${uniqueSuffix()}\n`,
  };
}

export function buildCreateMultipartFields(locale = 'fr') {
  const packageDTO = buildCreatePayload();
  return {
    packageDTO: JSON.stringify(packageDTO),
    locale,
    files: [
      buildFakePackagePicture(),
      buildFakePackageInvoice(),
    ],
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

export function buildFranceVisibleSeedPayload(cityIndex = 0, variantIndex = 0) {
  const { name: departureName, ...departure } = FRANCE_CITY_POOL[cityIndex % FRANCE_CITY_POOL.length];
  const { name: arrivalName, ...arrival } = FRANCE_CITY_POOL[(cityIndex + variantIndex + 3) % FRANCE_CITY_POOL.length];
  const seed = `VISIBLE-FR-${departureName}-${variantIndex}-${uniqueSuffix()}`;

  return {
    ...buildEstimatePayload({
      seed,
      departure,
      arrival,
      referencePrefix: `PERF-VISIBLE-FR-${departureName.toUpperCase()}`,
    }),
    status: 'PAYMENTPENDING',
  };
}

export function getFranceVisibleCities() {
  return FRANCE_CITY_POOL.slice();
}

const CURATED_VISIBLE_CITY_PLANS = [
  {
    name: 'Limeil-Brevannes',
    departures: [
      {
        line1: '3 Rue Pasteur',
        line2: '',
        town: 'Limeil-Brevannes',
        zipCode: '94450',
        country: 'France',
        latitude: 48.74845,
        longitude: 2.48856,
        addressAuto: '3 Rue Pasteur, 94450 Limeil-Brevannes, France',
      },
      {
        line1: '11 Rue Henri Barbusse',
        line2: '',
        town: 'Limeil-Brevannes',
        zipCode: '94450',
        country: 'France',
        latitude: 48.7449,
        longitude: 2.4921,
        addressAuto: '11 Rue Henri Barbusse, 94450 Limeil-Brevannes, France',
      },
      {
        line1: '18 Avenue de Verdun',
        line2: '',
        town: 'Limeil-Brevannes',
        zipCode: '94450',
        country: 'France',
        latitude: 48.7516,
        longitude: 2.4834,
        addressAuto: '18 Avenue de Verdun, 94450 Limeil-Brevannes, France',
      },
      {
        line1: '6 Rue Claude Bernard',
        line2: '',
        town: 'Limeil-Brevannes',
        zipCode: '94450',
        country: 'France',
        latitude: 48.7541,
        longitude: 2.4958,
        addressAuto: '6 Rue Claude Bernard, 94450 Limeil-Brevannes, France',
      },
      {
        line1: '22 Rue des Herbages',
        line2: '',
        town: 'Limeil-Brevannes',
        zipCode: '94450',
        country: 'France',
        latitude: 48.7468,
        longitude: 2.5004,
        addressAuto: '22 Rue des Herbages, 94450 Limeil-Brevannes, France',
      },
    ],
    arrivals: [
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
        line1: '15 Rue Juliette Savar',
        line2: '',
        town: 'Creteil',
        zipCode: '94000',
        country: 'France',
        latitude: 48.7759,
        longitude: 2.4612,
        addressAuto: '15 Rue Juliette Savar, 94000 Creteil, France',
      },
      {
        line1: '8 Rue Maurice Demenitroux',
        line2: '',
        town: 'Creteil',
        zipCode: '94000',
        country: 'France',
        latitude: 48.7864,
        longitude: 2.4478,
        addressAuto: '8 Rue Maurice Demenitroux, 94000 Creteil, France',
      },
      {
        line1: '21 Rue de la Basse Quinte',
        line2: '',
        town: 'Creteil',
        zipCode: '94000',
        country: 'France',
        latitude: 48.7717,
        longitude: 2.4686,
        addressAuto: '21 Rue de la Basse Quinte, 94000 Creteil, France',
      },
      {
        line1: '4 Allee Parmentier',
        line2: '',
        town: 'Creteil',
        zipCode: '94000',
        country: 'France',
        latitude: 48.7794,
        longitude: 2.4721,
        addressAuto: '4 Allee Parmentier, 94000 Creteil, France',
      },
    ],
  },
  {
    name: 'Choisy-le-Roi',
    departures: [
      {
        line1: '2 Avenue Anatole France',
        line2: '',
        town: 'Choisy-le-Roi',
        zipCode: '94600',
        country: 'France',
        latitude: 48.7644,
        longitude: 2.4095,
        addressAuto: '2 Avenue Anatole France, 94600 Choisy-le-Roi, France',
      },
      {
        line1: '14 Rue Emile Zola',
        line2: '',
        town: 'Choisy-le-Roi',
        zipCode: '94600',
        country: 'France',
        latitude: 48.7683,
        longitude: 2.4141,
        addressAuto: '14 Rue Emile Zola, 94600 Choisy-le-Roi, France',
      },
      {
        line1: '6 Rue de l Insurrection Parisienne',
        line2: '',
        town: 'Choisy-le-Roi',
        zipCode: '94600',
        country: 'France',
        latitude: 48.7611,
        longitude: 2.4178,
        addressAuto: '6 Rue de l Insurrection Parisienne, 94600 Choisy-le-Roi, France',
      },
      {
        line1: '23 Rue de la Poste',
        line2: '',
        town: 'Choisy-le-Roi',
        zipCode: '94600',
        country: 'France',
        latitude: 48.7668,
        longitude: 2.4062,
        addressAuto: '23 Rue de la Poste, 94600 Choisy-le-Roi, France',
      },
      {
        line1: '31 Avenue Victor Hugo',
        line2: '',
        town: 'Choisy-le-Roi',
        zipCode: '94600',
        country: 'France',
        latitude: 48.7597,
        longitude: 2.4114,
        addressAuto: '31 Avenue Victor Hugo, 94600 Choisy-le-Roi, France',
      },
    ],
    arrivals: [
      {
        line1: '5 Rue Jean Mermoz',
        line2: '',
        town: 'Orly',
        zipCode: '94310',
        country: 'France',
        latitude: 48.7478,
        longitude: 2.4012,
        addressAuto: '5 Rue Jean Mermoz, 94310 Orly, France',
      },
      {
        line1: '18 Rue des Hautes Bornes',
        line2: '',
        town: 'Orly',
        zipCode: '94310',
        country: 'France',
        latitude: 48.7441,
        longitude: 2.3957,
        addressAuto: '18 Rue des Hautes Bornes, 94310 Orly, France',
      },
      {
        line1: '7 Avenue Adrien Raynal',
        line2: '',
        town: 'Orly',
        zipCode: '94310',
        country: 'France',
        latitude: 48.7512,
        longitude: 2.4048,
        addressAuto: '7 Avenue Adrien Raynal, 94310 Orly, France',
      },
      {
        line1: '24 Rue Christophe Colomb',
        line2: '',
        town: 'Orly',
        zipCode: '94310',
        country: 'France',
        latitude: 48.7462,
        longitude: 2.3899,
        addressAuto: '24 Rue Christophe Colomb, 94310 Orly, France',
      },
      {
        line1: '11 Rue Louis Bonin',
        line2: '',
        town: 'Orly',
        zipCode: '94310',
        country: 'France',
        latitude: 48.7495,
        longitude: 2.3984,
        addressAuto: '11 Rue Louis Bonin, 94310 Orly, France',
      },
    ],
  },
  {
    name: 'Orly',
    departures: [
      {
        line1: '3 Rue Louis Bonin',
        line2: '',
        town: 'Orly',
        zipCode: '94310',
        country: 'France',
        latitude: 48.7488,
        longitude: 2.3974,
        addressAuto: '3 Rue Louis Bonin, 94310 Orly, France',
      },
      {
        line1: '12 Rue du Commerce',
        line2: '',
        town: 'Orly',
        zipCode: '94310',
        country: 'France',
        latitude: 48.7521,
        longitude: 2.4041,
        addressAuto: '12 Rue du Commerce, 94310 Orly, France',
      },
      {
        line1: '20 Rue Christophe Colomb',
        line2: '',
        town: 'Orly',
        zipCode: '94310',
        country: 'France',
        latitude: 48.7464,
        longitude: 2.3907,
        addressAuto: '20 Rue Christophe Colomb, 94310 Orly, France',
      },
      {
        line1: '9 Rue des Oliviers',
        line2: '',
        town: 'Orly',
        zipCode: '94310',
        country: 'France',
        latitude: 48.7446,
        longitude: 2.4015,
        addressAuto: '9 Rue des Oliviers, 94310 Orly, France',
      },
      {
        line1: '27 Avenue de la Victoire',
        line2: '',
        town: 'Orly',
        zipCode: '94310',
        country: 'France',
        latitude: 48.7504,
        longitude: 2.4093,
        addressAuto: '27 Avenue de la Victoire, 94310 Orly, France',
      },
    ],
    arrivals: [
      {
        line1: '4 Place des Martyrs',
        line2: '',
        town: 'Thiais',
        zipCode: '94320',
        country: 'France',
        latitude: 48.7649,
        longitude: 2.3911,
        addressAuto: '4 Place des Martyrs, 94320 Thiais, France',
      },
      {
        line1: '16 Avenue Rene Panhard',
        line2: '',
        town: 'Thiais',
        zipCode: '94320',
        country: 'France',
        latitude: 48.7616,
        longitude: 2.3854,
        addressAuto: '16 Avenue Rene Panhard, 94320 Thiais, France',
      },
      {
        line1: '9 Rue Maurepas',
        line2: '',
        town: 'Thiais',
        zipCode: '94320',
        country: 'France',
        latitude: 48.7587,
        longitude: 2.3998,
        addressAuto: '9 Rue Maurepas, 94320 Thiais, France',
      },
      {
        line1: '21 Rue de la Resistance',
        line2: '',
        town: 'Thiais',
        zipCode: '94320',
        country: 'France',
        latitude: 48.7662,
        longitude: 2.4017,
        addressAuto: '21 Rue de la Resistance, 94320 Thiais, France',
      },
      {
        line1: '7 Rue de la Saussaie',
        line2: '',
        town: 'Thiais',
        zipCode: '94320',
        country: 'France',
        latitude: 48.7569,
        longitude: 2.3926,
        addressAuto: '7 Rue de la Saussaie, 94320 Thiais, France',
      },
    ],
  },
  {
    name: 'Saint-Denis',
    departures: [
      {
        line1: '1 Place du Caquet',
        line2: '',
        town: 'Saint-Denis',
        zipCode: '93200',
        country: 'France',
        latitude: 48.9363,
        longitude: 2.3575,
        addressAuto: '1 Place du Caquet, 93200 Saint-Denis, France',
      },
      {
        line1: '17 Rue Gabriel Peri',
        line2: '',
        town: 'Saint-Denis',
        zipCode: '93200',
        country: 'France',
        latitude: 48.9348,
        longitude: 2.3541,
        addressAuto: '17 Rue Gabriel Peri, 93200 Saint-Denis, France',
      },
      {
        line1: '9 Rue de la Republique',
        line2: '',
        town: 'Saint-Denis',
        zipCode: '93200',
        country: 'France',
        latitude: 48.9381,
        longitude: 2.3612,
        addressAuto: '9 Rue de la Republique, 93200 Saint-Denis, France',
      },
      {
        line1: '25 Boulevard Marcel Sembat',
        line2: '',
        town: 'Saint-Denis',
        zipCode: '93200',
        country: 'France',
        latitude: 48.9417,
        longitude: 2.3658,
        addressAuto: '25 Boulevard Marcel Sembat, 93200 Saint-Denis, France',
      },
      {
        line1: '6 Rue Auguste Gillot',
        line2: '',
        town: 'Saint-Denis',
        zipCode: '93200',
        country: 'France',
        latitude: 48.9329,
        longitude: 2.3495,
        addressAuto: '6 Rue Auguste Gillot, 93200 Saint-Denis, France',
      },
    ],
    arrivals: [
      {
        line1: '8 Rue de la Montjoie',
        line2: '',
        town: 'Saint-Ouen-sur-Seine',
        zipCode: '93400',
        country: 'France',
        latitude: 48.9124,
        longitude: 2.3407,
        addressAuto: '8 Rue de la Montjoie, 93400 Saint-Ouen-sur-Seine, France',
      },
      {
        line1: '19 Rue Albert Dhalenne',
        line2: '',
        town: 'Saint-Ouen-sur-Seine',
        zipCode: '93400',
        country: 'France',
        latitude: 48.9072,
        longitude: 2.3339,
        addressAuto: '19 Rue Albert Dhalenne, 93400 Saint-Ouen-sur-Seine, France',
      },
      {
        line1: '4 Rue Arago',
        line2: '',
        town: 'Saint-Ouen-sur-Seine',
        zipCode: '93400',
        country: 'France',
        latitude: 48.9046,
        longitude: 2.3485,
        addressAuto: '4 Rue Arago, 93400 Saint-Ouen-sur-Seine, France',
      },
      {
        line1: '22 Rue du Docteur Bauer',
        line2: '',
        town: 'Saint-Ouen-sur-Seine',
        zipCode: '93400',
        country: 'France',
        latitude: 48.9099,
        longitude: 2.3441,
        addressAuto: '22 Rue du Docteur Bauer, 93400 Saint-Ouen-sur-Seine, France',
      },
      {
        line1: '11 Rue Kleber',
        line2: '',
        town: 'Saint-Ouen-sur-Seine',
        zipCode: '93400',
        country: 'France',
        latitude: 48.9018,
        longitude: 2.3374,
        addressAuto: '11 Rue Kleber, 93400 Saint-Ouen-sur-Seine, France',
      },
    ],
  },
  {
    name: 'Saint-Cloud',
    departures: [
      {
        line1: '2 Rue Dailly',
        line2: '',
        town: 'Saint-Cloud',
        zipCode: '92210',
        country: 'France',
        latitude: 48.8468,
        longitude: 2.2198,
        addressAuto: '2 Rue Dailly, 92210 Saint-Cloud, France',
      },
      {
        line1: '15 Rue Royale',
        line2: '',
        town: 'Saint-Cloud',
        zipCode: '92210',
        country: 'France',
        latitude: 48.8451,
        longitude: 2.2257,
        addressAuto: '15 Rue Royale, 92210 Saint-Cloud, France',
      },
      {
        line1: '7 Avenue Bernard Palissy',
        line2: '',
        town: 'Saint-Cloud',
        zipCode: '92210',
        country: 'France',
        latitude: 48.8509,
        longitude: 2.2141,
        addressAuto: '7 Avenue Bernard Palissy, 92210 Saint-Cloud, France',
      },
      {
        line1: '24 Rue du Calvaire',
        line2: '',
        town: 'Saint-Cloud',
        zipCode: '92210',
        country: 'France',
        latitude: 48.8433,
        longitude: 2.2296,
        addressAuto: '24 Rue du Calvaire, 92210 Saint-Cloud, France',
      },
      {
        line1: '10 Avenue de Longchamp',
        line2: '',
        town: 'Saint-Cloud',
        zipCode: '92210',
        country: 'France',
        latitude: 48.8534,
        longitude: 2.2208,
        addressAuto: '10 Avenue de Longchamp, 92210 Saint-Cloud, France',
      },
    ],
    arrivals: [
      {
        line1: '9 Route de la Reine',
        line2: '',
        town: 'Boulogne-Billancourt',
        zipCode: '92100',
        country: 'France',
        latitude: 48.8356,
        longitude: 2.2408,
        addressAuto: '9 Route de la Reine, 92100 Boulogne-Billancourt, France',
      },
      {
        line1: '18 Rue de Silly',
        line2: '',
        town: 'Boulogne-Billancourt',
        zipCode: '92100',
        country: 'France',
        latitude: 48.8334,
        longitude: 2.2473,
        addressAuto: '18 Rue de Silly, 92100 Boulogne-Billancourt, France',
      },
      {
        line1: '4 Avenue Andre Morizet',
        line2: '',
        town: 'Boulogne-Billancourt',
        zipCode: '92100',
        country: 'France',
        latitude: 48.8413,
        longitude: 2.2364,
        addressAuto: '4 Avenue Andre Morizet, 92100 Boulogne-Billancourt, France',
      },
      {
        line1: '27 Rue du Chateau',
        line2: '',
        town: 'Boulogne-Billancourt',
        zipCode: '92100',
        country: 'France',
        latitude: 48.8381,
        longitude: 2.2524,
        addressAuto: '27 Rue du Chateau, 92100 Boulogne-Billancourt, France',
      },
      {
        line1: '6 Rue Escudier',
        line2: '',
        town: 'Boulogne-Billancourt',
        zipCode: '92100',
        country: 'France',
        latitude: 48.8427,
        longitude: 2.2457,
        addressAuto: '6 Rue Escudier, 92100 Boulogne-Billancourt, France',
      },
    ],
  },
  {
    name: 'Boulogne',
    departures: [
      {
        line1: '12 Boulevard Jean Jaures',
        line2: '',
        town: 'Boulogne-Billancourt',
        zipCode: '92100',
        country: 'France',
        latitude: 48.8394,
        longitude: 2.2396,
        addressAuto: '12 Boulevard Jean Jaures, 92100 Boulogne-Billancourt, France',
      },
      {
        line1: '5 Rue Gallieni',
        line2: '',
        town: 'Boulogne-Billancourt',
        zipCode: '92100',
        country: 'France',
        latitude: 48.8352,
        longitude: 2.2441,
        addressAuto: '5 Rue Gallieni, 92100 Boulogne-Billancourt, France',
      },
      {
        line1: '28 Avenue Victor Hugo',
        line2: '',
        town: 'Boulogne-Billancourt',
        zipCode: '92100',
        country: 'France',
        latitude: 48.8421,
        longitude: 2.2512,
        addressAuto: '28 Avenue Victor Hugo, 92100 Boulogne-Billancourt, France',
      },
      {
        line1: '3 Rue Fessart',
        line2: '',
        town: 'Boulogne-Billancourt',
        zipCode: '92100',
        country: 'France',
        latitude: 48.8299,
        longitude: 2.2384,
        addressAuto: '3 Rue Fessart, 92100 Boulogne-Billancourt, France',
      },
      {
        line1: '21 Rue de Billancourt',
        line2: '',
        town: 'Boulogne-Billancourt',
        zipCode: '92100',
        country: 'France',
        latitude: 48.8337,
        longitude: 2.2328,
        addressAuto: '21 Rue de Billancourt, 92100 Boulogne-Billancourt, France',
      },
    ],
    arrivals: [
      {
        line1: '8 Rue des Freres Caudron',
        line2: '',
        town: 'Issy-les-Moulineaux',
        zipCode: '92130',
        country: 'France',
        latitude: 48.8214,
        longitude: 2.2642,
        addressAuto: '8 Rue des Freres Caudron, 92130 Issy-les-Moulineaux, France',
      },
      {
        line1: '15 Rue Ernest Renan',
        line2: '',
        town: 'Issy-les-Moulineaux',
        zipCode: '92130',
        country: 'France',
        latitude: 48.8276,
        longitude: 2.2705,
        addressAuto: '15 Rue Ernest Renan, 92130 Issy-les-Moulineaux, France',
      },
      {
        line1: '2 Rue Rouget de Lisle',
        line2: '',
        town: 'Issy-les-Moulineaux',
        zipCode: '92130',
        country: 'France',
        latitude: 48.8248,
        longitude: 2.2563,
        addressAuto: '2 Rue Rouget de Lisle, 92130 Issy-les-Moulineaux, France',
      },
      {
        line1: '19 Avenue Victor Cresson',
        line2: '',
        town: 'Issy-les-Moulineaux',
        zipCode: '92130',
        country: 'France',
        latitude: 48.8196,
        longitude: 2.2684,
        addressAuto: '19 Avenue Victor Cresson, 92130 Issy-les-Moulineaux, France',
      },
      {
        line1: '6 Rue Kléber',
        line2: '',
        town: 'Issy-les-Moulineaux',
        zipCode: '92130',
        country: 'France',
        latitude: 48.8301,
        longitude: 2.2628,
        addressAuto: '6 Rue Kleber, 92130 Issy-les-Moulineaux, France',
      },
    ],
  },
  {
    name: 'Corbeil-Essonnes',
    departures: [
      {
        line1: '4 Place Galignani',
        line2: '',
        town: 'Corbeil-Essonnes',
        zipCode: '91100',
        country: 'France',
        latitude: 48.6149,
        longitude: 2.4745,
        addressAuto: '4 Place Galignani, 91100 Corbeil-Essonnes, France',
      },
      {
        line1: '12 Rue Feray',
        line2: '',
        town: 'Corbeil-Essonnes',
        zipCode: '91100',
        country: 'France',
        latitude: 48.6123,
        longitude: 2.4788,
        addressAuto: '12 Rue Feray, 91100 Corbeil-Essonnes, France',
      },
      {
        line1: '8 Boulevard Jean Jaures',
        line2: '',
        town: 'Corbeil-Essonnes',
        zipCode: '91100',
        country: 'France',
        latitude: 48.6178,
        longitude: 2.4691,
        addressAuto: '8 Boulevard Jean Jaures, 91100 Corbeil-Essonnes, France',
      },
      {
        line1: '25 Rue Saint-Spire',
        line2: '',
        town: 'Corbeil-Essonnes',
        zipCode: '91100',
        country: 'France',
        latitude: 48.6096,
        longitude: 2.4824,
        addressAuto: '25 Rue Saint-Spire, 91100 Corbeil-Essonnes, France',
      },
      {
        line1: '31 Avenue Darblay',
        line2: '',
        town: 'Corbeil-Essonnes',
        zipCode: '91100',
        country: 'France',
        latitude: 48.6207,
        longitude: 2.4863,
        addressAuto: '31 Avenue Darblay, 91100 Corbeil-Essonnes, France',
      },
    ],
    arrivals: [
      {
        line1: '3 Place des Droits de l Homme',
        line2: '',
        town: 'Evry-Courcouronnes',
        zipCode: '91000',
        country: 'France',
        latitude: 48.6242,
        longitude: 2.4298,
        addressAuto: '3 Place des Droits de l Homme, 91000 Evry-Courcouronnes, France',
      },
      {
        line1: '11 Boulevard de l Europe',
        line2: '',
        town: 'Evry-Courcouronnes',
        zipCode: '91000',
        country: 'France',
        latitude: 48.6298,
        longitude: 2.4421,
        addressAuto: '11 Boulevard de l Europe, 91000 Evry-Courcouronnes, France',
      },
      {
        line1: '6 Rue Montespan',
        line2: '',
        town: 'Evry-Courcouronnes',
        zipCode: '91000',
        country: 'France',
        latitude: 48.6261,
        longitude: 2.4368,
        addressAuto: '6 Rue Montespan, 91000 Evry-Courcouronnes, France',
      },
      {
        line1: '18 Cours Blaise Pascal',
        line2: '',
        town: 'Evry-Courcouronnes',
        zipCode: '91000',
        country: 'France',
        latitude: 48.6334,
        longitude: 2.4312,
        addressAuto: '18 Cours Blaise Pascal, 91000 Evry-Courcouronnes, France',
      },
      {
        line1: '9 Place de l Agora',
        line2: '',
        town: 'Evry-Courcouronnes',
        zipCode: '91000',
        country: 'France',
        latitude: 48.6287,
        longitude: 2.4404,
        addressAuto: '9 Place de l Agora, 91000 Evry-Courcouronnes, France',
      },
    ],
  },
  {
    name: 'Sucy-en-Brie',
    departures: [
      {
        line1: '12 Rue du General Leclerc',
        line2: '',
        town: 'Sucy-en-Brie',
        zipCode: '94370',
        country: 'France',
        latitude: 48.7694,
        longitude: 2.5342,
        addressAuto: '12 Rue du General Leclerc, 94370 Sucy-en-Brie, France',
      },
      {
        line1: '5 Place Sainte-Bernadette',
        line2: '',
        town: 'Sucy-en-Brie',
        zipCode: '94370',
        country: 'France',
        latitude: 48.7721,
        longitude: 2.5298,
        addressAuto: '5 Place Sainte-Bernadette, 94370 Sucy-en-Brie, France',
      },
      {
        line1: '18 Avenue Winston Churchill',
        line2: '',
        town: 'Sucy-en-Brie',
        zipCode: '94370',
        country: 'France',
        latitude: 48.7662,
        longitude: 2.5411,
        addressAuto: '18 Avenue Winston Churchill, 94370 Sucy-en-Brie, France',
      },
      {
        line1: '9 Rue de la Porte',
        line2: '',
        town: 'Sucy-en-Brie',
        zipCode: '94370',
        country: 'France',
        latitude: 48.7744,
        longitude: 2.5373,
        addressAuto: '9 Rue de la Porte, 94370 Sucy-en-Brie, France',
      },
      {
        line1: '27 Rue du Temple',
        line2: '',
        town: 'Sucy-en-Brie',
        zipCode: '94370',
        country: 'France',
        latitude: 48.7681,
        longitude: 2.5266,
        addressAuto: '27 Rue du Temple, 94370 Sucy-en-Brie, France',
      },
    ],
    arrivals: [
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
        line1: '15 Rue Juliette Savar',
        line2: '',
        town: 'Creteil',
        zipCode: '94000',
        country: 'France',
        latitude: 48.7759,
        longitude: 2.4612,
        addressAuto: '15 Rue Juliette Savar, 94000 Creteil, France',
      },
      {
        line1: '8 Rue Maurice Demenitroux',
        line2: '',
        town: 'Creteil',
        zipCode: '94000',
        country: 'France',
        latitude: 48.7864,
        longitude: 2.4478,
        addressAuto: '8 Rue Maurice Demenitroux, 94000 Creteil, France',
      },
      {
        line1: '21 Rue de la Basse Quinte',
        line2: '',
        town: 'Creteil',
        zipCode: '94000',
        country: 'France',
        latitude: 48.7717,
        longitude: 2.4686,
        addressAuto: '21 Rue de la Basse Quinte, 94000 Creteil, France',
      },
      {
        line1: '4 Allee Parmentier',
        line2: '',
        town: 'Creteil',
        zipCode: '94000',
        country: 'France',
        latitude: 48.7794,
        longitude: 2.4721,
        addressAuto: '4 Allee Parmentier, 94000 Creteil, France',
      },
    ],
  },
];

const GENERATED_ADDRESS_OFFSETS = [
  { lat: 0, lng: 0, suffix: 'Central' },
  { lat: 0.012, lng: 0.009, suffix: 'Nord-Est' },
  { lat: -0.011, lng: 0.008, suffix: 'Sud-Est' },
  { lat: 0.009, lng: -0.012, suffix: 'Nord-Ouest' },
  { lat: -0.013, lng: -0.007, suffix: 'Sud-Ouest' },
];

const FRANCE_WIDE_VISIBLE_CLUSTERS = [
  {
    name: 'Paris',
    departure: { line1: '14 Cours de Vincennes', town: 'Paris', zipCode: '75012', country: 'France', latitude: 48.8447, longitude: 2.4084 },
    arrival: { line1: '6 Rue de Londres', town: 'Paris', zipCode: '75009', country: 'France', latitude: 48.8762, longitude: 2.3292 },
  },
  {
    name: 'Lille',
    departure: { line1: '1 Boulevard de Turin', town: 'Lille', zipCode: '59800', country: 'France', latitude: 50.6394, longitude: 3.0754 },
    arrival: { line1: '18 Rue des Canonniers', town: 'Lille', zipCode: '59800', country: 'France', latitude: 50.6367, longitude: 3.0716 },
  },
  {
    name: 'Strasbourg',
    departure: { line1: '1 Place de la Gare', town: 'Strasbourg', zipCode: '67000', country: 'France', latitude: 48.5852, longitude: 7.7340 },
    arrival: { line1: '12 Rue du Faubourg de Saverne', town: 'Strasbourg', zipCode: '67000', country: 'France', latitude: 48.5878, longitude: 7.7413 },
  },
  {
    name: 'Nantes',
    departure: { line1: '27 Boulevard de Stalingrad', town: 'Nantes', zipCode: '44000', country: 'France', latitude: 47.2173, longitude: -1.5417 },
    arrival: { line1: '5 Rue Foure', town: 'Nantes', zipCode: '44000', country: 'France', latitude: 47.2144, longitude: -1.5488 },
  },
  {
    name: 'Rennes',
    departure: { line1: '19 Place de la Gare', town: 'Rennes', zipCode: '35000', country: 'France', latitude: 48.1035, longitude: -1.6723 },
    arrival: { line1: '8 Boulevard Solferino', town: 'Rennes', zipCode: '35000', country: 'France', latitude: 48.1056, longitude: -1.6782 },
  },
  {
    name: 'Bordeaux',
    departure: { line1: '1 Rue Charles Domercq', town: 'Bordeaux', zipCode: '33800', country: 'France', latitude: 44.8253, longitude: -0.5567 },
    arrival: { line1: '14 Cours de la Marne', town: 'Bordeaux', zipCode: '33800', country: 'France', latitude: 44.8304, longitude: -0.5631 },
  },
  {
    name: 'Toulouse',
    departure: { line1: '64 Boulevard Pierre Semard', town: 'Toulouse', zipCode: '31500', country: 'France', latitude: 43.6112, longitude: 1.4544 },
    arrival: { line1: '9 Allee Jean Jaures', town: 'Toulouse', zipCode: '31000', country: 'France', latitude: 43.6083, longitude: 1.4493 },
  },
  {
    name: 'Montpellier',
    departure: { line1: 'Place Auguste Gibert', town: 'Montpellier', zipCode: '34000', country: 'France', latitude: 43.6045, longitude: 3.8807 },
    arrival: { line1: '11 Rue Jules Ferry', town: 'Montpellier', zipCode: '34000', country: 'France', latitude: 43.6072, longitude: 3.8736 },
  },
  {
    name: 'Marseille',
    departure: { line1: 'Square Narvik', town: 'Marseille', zipCode: '13001', country: 'France', latitude: 43.3028, longitude: 5.3802 },
    arrival: { line1: '22 Boulevard d Athenes', town: 'Marseille', zipCode: '13001', country: 'France', latitude: 43.3009, longitude: 5.3761 },
  },
  {
    name: 'Nice',
    departure: { line1: '12 Avenue Thiers', town: 'Nice', zipCode: '06000', country: 'France', latitude: 43.7043, longitude: 7.2619 },
    arrival: { line1: '7 Avenue Jean Medecin', town: 'Nice', zipCode: '06000', country: 'France', latitude: 43.7011, longitude: 7.2684 },
  },
  {
    name: 'Lyon',
    departure: { line1: 'Place Charles Beraudier', town: 'Lyon', zipCode: '69003', country: 'France', latitude: 45.7606, longitude: 4.8599 },
    arrival: { line1: '18 Rue Servient', town: 'Lyon', zipCode: '69003', country: 'France', latitude: 45.7618, longitude: 4.8527 },
  },
  {
    name: 'Grenoble',
    departure: { line1: '1 Place de la Gare', town: 'Grenoble', zipCode: '38000', country: 'France', latitude: 45.1918, longitude: 5.7140 },
    arrival: { line1: '10 Avenue Alsace Lorraine', town: 'Grenoble', zipCode: '38000', country: 'France', latitude: 45.1889, longitude: 5.7202 },
  },
  {
    name: 'Clermont-Ferrand',
    departure: { line1: '46 Avenue de l Union Sovietique', town: 'Clermont-Ferrand', zipCode: '63000', country: 'France', latitude: 45.7782, longitude: 3.1001 },
    arrival: { line1: '9 Boulevard Lafayette', town: 'Clermont-Ferrand', zipCode: '63000', country: 'France', latitude: 45.7751, longitude: 3.0877 },
  },
  {
    name: 'Dijon',
    departure: { line1: '31 Cour de la Gare', town: 'Dijon', zipCode: '21000', country: 'France', latitude: 47.3220, longitude: 5.0342 },
    arrival: { line1: '6 Avenue Marechal Foch', town: 'Dijon', zipCode: '21000', country: 'France', latitude: 47.3241, longitude: 5.0289 },
  },
  {
    name: 'Brest',
    departure: { line1: '8 Place du 19e Regiment d Infanterie', town: 'Brest', zipCode: '29200', country: 'France', latitude: 48.3876, longitude: -4.4801 },
    arrival: { line1: '15 Rue Jean Jaures', town: 'Brest', zipCode: '29200', country: 'France', latitude: 48.3902, longitude: -4.4867 },
  },
];

function buildOffsetAddress(baseAddress, offset, index, roleLabel) {
  return {
    line1: `${baseAddress.line1} ${roleLabel} ${offset.suffix}`,
    line2: '',
    town: baseAddress.town,
    zipCode: baseAddress.zipCode,
    country: baseAddress.country,
    latitude: Number((baseAddress.latitude + offset.lat).toFixed(6)),
    longitude: Number((baseAddress.longitude + offset.lng).toFixed(6)),
    addressAuto: `${baseAddress.line1} ${roleLabel} ${offset.suffix}, ${baseAddress.zipCode} ${baseAddress.town}, ${baseAddress.country}`,
  };
}

function buildGeneratedVisibleCityPlan(cluster) {
  return {
    name: cluster.name,
    departures: GENERATED_ADDRESS_OFFSETS.map((offset, index) => buildOffsetAddress(cluster.departure, offset, index, 'Depart')),
    arrivals: GENERATED_ADDRESS_OFFSETS.map((offset, index) => buildOffsetAddress(cluster.arrival, offset, index, 'Arrivee')),
  };
}

const ALL_VISIBLE_CITY_PLANS = [
  ...CURATED_VISIBLE_CITY_PLANS,
  ...FRANCE_WIDE_VISIBLE_CLUSTERS.map(buildGeneratedVisibleCityPlan),
];

function cloneAddress(address) {
  return {
    ...address,
  };
}

function buildVisibleSeedPackagePayload(departure, arrival, referencePrefix, seed) {
  return {
    ...buildEstimatePayload({
      seed,
      departure: cloneAddress(departure),
      arrival: cloneAddress(arrival),
      referencePrefix,
    }),
    status: 'PAYMENTPENDING',
  };
}

export function getCuratedVisibleSeedCities() {
  return ALL_VISIBLE_CITY_PLANS.map(({ name }) => ({ name }));
}

export function buildCuratedVisibleSeedPlan(cityIndex = 0) {
  const cityPlan = ALL_VISIBLE_CITY_PLANS[cityIndex % ALL_VISIBLE_CITY_PLANS.length];
  const anchorSeed = `VISIBLE-CURATED-${cityPlan.name}-ANCHOR-${uniqueSuffix()}`;
  const anchorPayload = buildVisibleSeedPackagePayload(
    cityPlan.departures[0],
    cityPlan.arrivals[0],
    `PERF-VISIBLE-${cityPlan.name.toUpperCase().replace(/[^A-Z0-9]/g, '-')}`,
    anchorSeed,
  );

  const nearbyPayloads = cityPlan.departures.slice(1, 5).map((departure, index) => {
    const arrival = cityPlan.arrivals[index + 1];
    const seed = `VISIBLE-CURATED-${cityPlan.name}-${index + 1}-${uniqueSuffix()}`;
    return buildVisibleSeedPackagePayload(
      departure,
      arrival,
      `PERF-VISIBLE-${cityPlan.name.toUpperCase().replace(/[^A-Z0-9]/g, '-')}`,
      seed,
    );
  });

  return {
    city: cityPlan.name,
    anchorPayload,
    nearbyPayloads,
  };
}
