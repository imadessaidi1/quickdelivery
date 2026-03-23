import { getRequiredUserDocuments, getRequiredVehicleDocuments, requiresVehicleDetails, resolveDeliveryMode } from './deliveryMode';

function getDocumentEntry(documents, key) {
  if (!documents) {
    return null;
  }
  return documents[key] || null;
}

function hasValue(value) {
  return value !== null && value !== undefined && `${value}`.trim() !== '';
}

function hasAddress(user) {
  if (hasValue(user?.addressAuto)) {
    return true;
  }

  const primaryAddress = Array.isArray(user?.personalAddress) ? user.personalAddress[0] : null;
  return hasValue(primaryAddress?.line1) && hasValue(primaryAddress?.zipCode) && hasValue(primaryAddress?.town) && hasValue(primaryAddress?.country);
}

function hasPayment(user) {
  const creditCard = user?.paymentModes?.CREDIT_CARD || {};
  const iban = user?.paymentModes?.IBAN || {};
  const cardConfigured = hasValue(creditCard.cardNumber) && hasValue(creditCard.expiryDate) && hasValue(creditCard.cvv);
  const ibanConfigured = hasValue(iban.iban) && hasValue(iban.bic);
  return cardConfigured || ibanConfigured;
}

function getRejectedKeys(keys, documents) {
  return keys.filter((key) => getDocumentEntry(documents, key)?.documentStatus === 'REJECTED');
}

function areDocumentsSubmitted(keys, documents) {
  return keys.every((key) => !!getDocumentEntry(documents, key));
}

export function buildCourierReadiness(user, vehicle = null, userDocuments = null, vehicleDocuments = null) {
  if (user?.type !== 'DELIVERY_PERSON') {
    return null;
  }

  const normalizedUserDocuments = userDocuments || user?.document || {};
  const normalizedVehicle = vehicle || (Array.isArray(user?.vehicles) ? user.vehicles[0] : null) || {};
  const normalizedVehicleDocuments = vehicleDocuments || normalizedVehicle?.vehicleDocuments || user?.document || {};
  const requiredUserDocuments = getRequiredUserDocuments(user);
  const requiredVehicleDocuments = getRequiredVehicleDocuments(user);
  const vehicleRequired = requiresVehicleDetails(user);

  const profileCompleted = hasValue(user?.firstName)
    && hasValue(user?.lastName)
    && hasValue(user?.emailAddress)
    && hasValue(user?.phone)
    && hasValue(user?.birthDate)
    && hasValue(user?.sex);
  const deliveryModeSelected = hasValue(resolveDeliveryMode(user));
  const emailValidated = user?.emailAddressValidation === true;
  const addressCompleted = hasAddress(user);
  const identityDocumentsSubmitted = areDocumentsSubmitted(requiredUserDocuments, normalizedUserDocuments);
  const vehicleInformationCompleted = !vehicleRequired || (
    hasValue(normalizedVehicle?.registrationNumber)
    && hasValue(normalizedVehicle?.brand)
    && hasValue(normalizedVehicle?.model)
    && hasValue(normalizedVehicle?.energyType)
  );
  const vehicleDocumentsSubmitted = !vehicleRequired || areDocumentsSubmitted(requiredVehicleDocuments, normalizedVehicleDocuments);
  const paymentConfigured = hasPayment(user);
  const rejectedUserDocuments = getRejectedKeys(requiredUserDocuments, normalizedUserDocuments);
  const rejectedVehicleDocuments = getRejectedKeys(requiredVehicleDocuments, normalizedVehicleDocuments);
  const rejectedDocuments = [...rejectedUserDocuments, ...rejectedVehicleDocuments];
  const accountApproved = user?.activeAccount === true;

  const steps = [
    { key: 'profile', labelKey: 'courierReadinessProfile', done: profileCompleted },
    { key: 'deliveryMode', labelKey: 'courierReadinessDeliveryMode', done: deliveryModeSelected },
    { key: 'email', labelKey: 'courierReadinessEmail', done: emailValidated },
    { key: 'address', labelKey: 'courierReadinessAddress', done: addressCompleted },
    { key: 'identity', labelKey: 'courierReadinessIdentityDocs', done: identityDocumentsSubmitted },
    { key: 'payment', labelKey: 'courierReadinessPayment', done: paymentConfigured },
    ...(vehicleRequired ? [{ key: 'vehicle', labelKey: 'courierReadinessVehicleInfo', done: vehicleInformationCompleted }] : []),
    ...(vehicleRequired ? [{ key: 'vehicleDocs', labelKey: 'courierReadinessVehicleDocs', done: vehicleDocumentsSubmitted }] : []),
    { key: 'approval', labelKey: 'courierReadinessApproval', done: accountApproved },
  ];

  return {
    steps,
    completedSteps: steps.filter((step) => step.done).length,
    totalSteps: steps.length,
    rejectedDocuments,
    readyForActivation: steps.every((step) => step.done) && rejectedDocuments.length === 0,
  };
}

