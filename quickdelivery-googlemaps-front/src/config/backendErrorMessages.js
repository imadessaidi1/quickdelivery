import i18n from './i18n';

const EXACT_MESSAGE_KEYS = {
  'Account already validated': 'onboardingAlreadyValidated',
  'Active delivery route not found': 'backendErrorActiveDeliveryRouteNotFound',
  'Active reservation not found': 'backendErrorActiveReservationNotFound',
  'An active delivery route already blocks new reservations': 'backendErrorActiveRouteBlocksReservations',
  'Captcha is temporarily unavailable': 'backendErrorCaptchaUnavailable',
  'Captcha token is required': 'backendErrorCaptchaTokenRequired',
  'Captcha verification failed': 'backendErrorCaptchaVerificationFailed',
  'Captcha verification rejected': 'backendErrorCaptchaRejected',
  'Declared value is required when insurance is selected': 'backendErrorDeclaredValueRequired',
  'Declared value must be greater than zero when insurance is selected.': 'backendErrorDeclaredValuePositive',
  'Delivery address must be different from pickup address': 'backendErrorDeliveryAddressDifferent',
  'Delivery mode is required.': 'backendErrorDeliveryModeRequired',
  'Delivery user not found': 'backendErrorDeliveryUserNotFound',
  'Distance must be greater than zero.': 'backendErrorDistancePositive',
  'Document file not found': 'backendErrorDocumentFileNotFound',
  'Document not found': 'backendErrorDocumentNotFound',
  'Draft onboarding is only available for delivery persons': 'backendErrorDraftOnboardingDeliveryOnly',
  'Email is required': 'backendErrorEmailRequired',
  'Email is required for pickup': 'backendErrorPickupEmailRequired',
  'Invalid delivery OTP': 'backendErrorInvalidDeliveryOtp',
  'Invalid guest access token': 'backendErrorInvalidGuestAccess',
  'Invalid pickup OTP': 'backendErrorInvalidPickupOtp',
  'Invalid update token': 'backendErrorInvalidUpdateToken',
  'Keycloak admin API temporarily unavailable': 'backendErrorKeycloakUnavailable',
  'Keycloak user created but could not be resolved': 'backendErrorKeycloakUserResolutionFailed',
  'Missing device registration data': 'backendErrorDeviceRegistrationMissing',
  'Notification not found': 'backendErrorNotificationNotFound',
  'Onboarding is locked': 'onboardingLocked',
  'Onboarding not found': 'backendErrorOnboardingNotFound',
  'Package already reserved by this delivery person': 'backendErrorPackageAlreadyReservedByCourier',
  'Package category is required.': 'backendErrorPackageCategoryRequired',
  'Package is not available for reservation': 'backendErrorPackageNotAvailableForReservation',
  'Package is not waiting for payment': 'backendErrorPackagePaymentState',
  'Package is temporarily consulted by another courier': 'backendErrorPackageSoftLocked',
  'Package not found': 'backendErrorPackageNotFound',
  'Package payload is required': 'backendErrorPackagePayloadRequired',
  'Package payload is required.': 'backendErrorPackagePayloadRequired',
  'Package reference is required': 'backendErrorPackageReferenceRequired',
  'Package size category is required': 'backendErrorPackageSizeCategoryRequired',
  'Password is required for identity provisioning': 'backendErrorIdentityPasswordRequired',
  'Pickup and delivery addresses are required': 'backendErrorPickupDeliveryAddressesRequired',
  'Public update is not available for this account': 'backendErrorPublicUpdateUnavailable',
  'Recipient not found': 'backendErrorRecipientNotFound',
  'Reservation not found': 'backendErrorReservationNotFound',
  'Sender not found': 'backendErrorSenderNotFound',
  'Unable to resolve route distance': 'backendErrorRouteDistanceUnavailable',
  'Unable to retrieve Keycloak admin access token': 'backendErrorKeycloakTokenUnavailable',
  'Unsupported captcha provider': 'backendErrorCaptchaUnsupported',
  'Unsupported package document type': 'backendErrorUnsupportedPackageDocumentType',
  'User already exists': 'backendErrorUserAlreadyExists',
  'User email is required for identity provisioning': 'backendErrorIdentityEmailRequired',
  'User not found': 'backendErrorUserNotFound',
  'User payload is required': 'backendErrorUserPayloadRequired',
  'Weight must be greater than zero.': 'backendErrorWeightPositive',
  errorActiveRouteCancellationNotAllowed: 'errorActiveRouteCancellationNotAllowed',
  errorActiveRouteStartNotAllowed: 'errorActiveRouteStartNotAllowed',
  errorCourierSuspended: 'errorCourierSuspended',
  errorDeliveryPersonLocationRequired: 'errorDeliveryPersonLocationRequired',
  errorDeliveryPersonNotAtStop: 'errorDeliveryPersonNotAtStop',
  errorPackageNotCompatibleWithVehicle: 'errorPackageNotCompatibleWithVehicle',
  errorPackageReservationCapacityReached: 'errorPackageReservationCapacityReached',
  errorReservationCancellationNotAllowed: 'errorReservationCancellationNotAllowed',
  errorRouteStopLocationMissing: 'errorRouteStopLocationMissing',
  'package.error.notFound': 'backendErrorPackageNotFound',
};

const PREFIX_MESSAGE_KEYS = [
  ['Contact name is required for ', 'backendErrorContactNameRequired'],
  ['Elevator selection is required for ', 'backendErrorElevatorSelectionRequired'],
  ['Keycloak role not found: ', 'backendErrorKeycloakRoleNotFound'],
  ['Package not found for reference: ', 'backendErrorPackageNotFound'],
  ['Phone number is required for ', 'backendErrorPhoneRequired'],
  ['Unknown delivery mode: ', 'backendErrorUnknownDeliveryMode'],
  ['Unknown package category: ', 'backendErrorUnknownPackageCategory'],
  ['Unsupported user type: ', 'backendErrorUnsupportedUserType'],
];

const HTTP_STATUS_KEYS = {
  400: 'backendErrorBadRequest',
  401: 'backendErrorUnauthorized',
  403: 'backendErrorForbidden',
  404: 'backendErrorNotFound',
  409: 'backendErrorConflict',
  423: 'backendErrorLocked',
  500: 'backendErrorServer',
  502: 'backendErrorServiceUnavailable',
  503: 'backendErrorServiceUnavailable',
};

const LOCAL_MESSAGES = {
  en: {
    backendErrorBadRequest: 'The request is incomplete or invalid.',
    backendErrorUnauthorized: 'Your session has expired. Please sign in again.',
    backendErrorForbidden: 'You are not allowed to perform this action.',
    backendErrorNotFound: 'The requested resource could not be found.',
    backendErrorConflict: 'This action cannot be completed in the current state.',
    backendErrorLocked: 'This file is locked and cannot be modified.',
    backendErrorServer: 'The service encountered an error. Please try again later.',
    backendErrorServiceUnavailable: 'The service is temporarily unavailable. Please try again later.',
    backendErrorUserPayloadRequired: 'User information is required.',
    backendErrorUserNotFound: 'User not found.',
    backendErrorEmailRequired: 'Email address is required.',
    backendErrorUserAlreadyExists: 'An account already exists for this email address.',
    backendErrorInvalidUpdateToken: 'This update link is invalid or has expired.',
    backendErrorPublicUpdateUnavailable: 'Public update is not available for this account.',
    backendErrorDraftOnboardingDeliveryOnly: 'Draft onboarding is available only for delivery person accounts.',
    backendErrorOnboardingNotFound: 'Onboarding file not found.',
    backendErrorDocumentNotFound: 'Document not found.',
    backendErrorDocumentFileNotFound: 'Document file not found.',
    backendErrorIdentityEmailRequired: 'Email address is required to create the identity account.',
    backendErrorIdentityPasswordRequired: 'Password is required to create the identity account.',
    backendErrorKeycloakTokenUnavailable: 'Identity service authentication is temporarily unavailable.',
    backendErrorKeycloakUnavailable: 'Identity service is temporarily unavailable.',
    backendErrorKeycloakUserResolutionFailed: 'The identity account was created but could not be loaded.',
    backendErrorKeycloakRoleNotFound: 'The requested identity role is not configured.',
    backendErrorUnsupportedUserType: 'This user type is not supported.',
    backendErrorPackagePayloadRequired: 'Package information is required.',
    backendErrorPackageNotFound: 'Package not found.',
    backendErrorPackageReferenceRequired: 'Package reference is required.',
    backendErrorInvalidGuestAccess: 'This guest access link is invalid or has expired.',
    backendErrorPackagePaymentState: 'This package is not waiting for payment.',
    backendErrorActiveReservationNotFound: 'Active reservation not found.',
    backendErrorRouteDistanceUnavailable: 'Unable to calculate the route distance right now.',
    backendErrorPickupDeliveryAddressesRequired: 'Pickup and delivery addresses are required.',
    backendErrorPackageSizeCategoryRequired: 'Package size is required.',
    backendErrorContactNameRequired: 'Contact first and last name are required.',
    backendErrorPhoneRequired: 'Phone number is required.',
    backendErrorPickupEmailRequired: 'Pickup email address is required.',
    backendErrorDeclaredValueRequired: 'Declared value is required when insurance is selected.',
    backendErrorDeclaredValuePositive: 'Declared value must be greater than zero when insurance is selected.',
    backendErrorElevatorSelectionRequired: 'Elevator availability must be specified for this address.',
    backendErrorDeliveryAddressDifferent: 'Delivery address must be different from pickup address.',
    backendErrorSenderNotFound: 'Sender account not found.',
    backendErrorUnsupportedPackageDocumentType: 'This package document type is not supported.',
    backendErrorActiveDeliveryRouteNotFound: 'Active delivery route not found.',
    backendErrorReservationNotFound: 'Reservation not found.',
    backendErrorDeliveryUserNotFound: 'Delivery person account not found.',
    backendErrorActiveRouteBlocksReservations: 'An active route already blocks new reservations.',
    backendErrorPackageNotAvailableForReservation: 'This package is no longer available for reservation.',
    backendErrorPackageAlreadyReservedByCourier: 'You have already reserved this package.',
    backendErrorPackageSoftLocked: 'Another courier is currently consulting this package.',
    backendErrorInvalidPickupOtp: 'The pickup code is invalid.',
    backendErrorInvalidDeliveryOtp: 'The delivery code is invalid.',
    backendErrorDeliveryPersonLocationRequired: 'Your current location is required to validate this step.',
    backendErrorRouteStopLocationMissing: 'The stop location is missing for this route.',
    backendErrorDeliveryPersonNotAtStop: 'You must be near the stop to validate this step.',
    backendErrorCourierSuspended: 'This courier account is temporarily suspended.',
    backendErrorReservationCapacityReached: 'Your reservation capacity is reached.',
    backendErrorNotificationRecipientNotFound: 'Notification recipient not found.',
    backendErrorNotificationNotFound: 'Notification not found.',
    backendErrorDeviceRegistrationMissing: 'Device registration data is missing.',
    backendErrorCaptchaUnsupported: 'Security verification is not configured correctly.',
    backendErrorCaptchaUnavailable: 'Security verification is temporarily unavailable.',
    backendErrorCaptchaTokenRequired: 'Please complete the security verification before continuing.',
    backendErrorCaptchaVerificationFailed: 'Security verification failed. Please try again.',
    backendErrorCaptchaRejected: 'Security verification was rejected. Please try again.',
    backendErrorPackageCategoryRequired: 'Package category is required.',
    backendErrorWeightPositive: 'Package weight must be greater than zero.',
    backendErrorDeliveryModeRequired: 'Delivery mode is required.',
    backendErrorDistancePositive: 'Route distance must be greater than zero.',
    backendErrorUnknownPackageCategory: 'This package category is not supported.',
    backendErrorUnknownDeliveryMode: 'This delivery mode is not supported.',
    backendErrorRecipientNotFound: 'Recipient not found.',
    errorActiveRouteStartNotAllowed: 'This route cannot be started yet.',
    errorReservationCancellationNotAllowed: 'This reservation can no longer be cancelled.',
    errorActiveRouteCancellationNotAllowed: 'This active route can no longer be cancelled.',
    errorDeliveryPersonLocationRequired: 'Your current location is required to validate this step.',
    errorRouteStopLocationMissing: 'The stop location is missing for this route.',
    errorDeliveryPersonNotAtStop: 'You must be near the stop to validate this step.',
    errorCourierSuspended: 'Your courier account is temporarily suspended.',
    errorPackageReservationCapacityReached: 'Your reservation capacity is reached.',
  },
  fr: {
    backendErrorBadRequest: 'La demande est incomplete ou invalide.',
    backendErrorUnauthorized: 'Votre session a expire. Veuillez vous reconnecter.',
    backendErrorForbidden: 'Vous n avez pas l autorisation d effectuer cette action.',
    backendErrorNotFound: 'La ressource demandee est introuvable.',
    backendErrorConflict: 'Cette action est impossible dans l etat actuel.',
    backendErrorLocked: 'Ce dossier est verrouille et ne peut pas etre modifie.',
    backendErrorServer: 'Le service a rencontre une erreur. Veuillez reessayer plus tard.',
    backendErrorServiceUnavailable: 'Le service est temporairement indisponible. Veuillez reessayer plus tard.',
    backendErrorUserPayloadRequired: 'Les informations utilisateur sont obligatoires.',
    backendErrorUserNotFound: 'Utilisateur introuvable.',
    backendErrorEmailRequired: 'L adresse email est obligatoire.',
    backendErrorUserAlreadyExists: 'Un compte existe deja avec cette adresse email.',
    backendErrorInvalidUpdateToken: 'Ce lien de mise a jour est invalide ou expire.',
    backendErrorPublicUpdateUnavailable: 'La mise a jour publique n est pas disponible pour ce compte.',
    backendErrorDraftOnboardingDeliveryOnly: 'Le brouillon d inscription est reserve aux comptes livreurs.',
    backendErrorOnboardingNotFound: 'Dossier d inscription introuvable.',
    backendErrorDocumentNotFound: 'Document introuvable.',
    backendErrorDocumentFileNotFound: 'Fichier du document introuvable.',
    backendErrorIdentityEmailRequired: 'L adresse email est obligatoire pour creer le compte d identite.',
    backendErrorIdentityPasswordRequired: 'Le mot de passe est obligatoire pour creer le compte d identite.',
    backendErrorKeycloakTokenUnavailable: 'L authentification du service d identite est temporairement indisponible.',
    backendErrorKeycloakUnavailable: 'Le service d identite est temporairement indisponible.',
    backendErrorKeycloakUserResolutionFailed: 'Le compte d identite a ete cree mais ne peut pas etre charge.',
    backendErrorKeycloakRoleNotFound: 'Le role d identite demande n est pas configure.',
    backendErrorUnsupportedUserType: 'Ce type d utilisateur n est pas supporte.',
    backendErrorPackagePayloadRequired: 'Les informations du colis sont obligatoires.',
    backendErrorPackageNotFound: 'Colis introuvable.',
    backendErrorPackageReferenceRequired: 'La reference du colis est obligatoire.',
    backendErrorInvalidGuestAccess: 'Ce lien d acces invite est invalide ou expire.',
    backendErrorPackagePaymentState: 'Ce colis n est pas en attente de paiement.',
    backendErrorActiveReservationNotFound: 'Reservation active introuvable.',
    backendErrorRouteDistanceUnavailable: 'Impossible de calculer la distance du trajet pour le moment.',
    backendErrorPickupDeliveryAddressesRequired: 'Les adresses de collecte et de livraison sont obligatoires.',
    backendErrorPackageSizeCategoryRequired: 'La taille du colis est obligatoire.',
    backendErrorContactNameRequired: 'Le prenom et le nom du contact sont obligatoires.',
    backendErrorPhoneRequired: 'Le numero de telephone est obligatoire.',
    backendErrorPickupEmailRequired: 'L email du contact de collecte est obligatoire.',
    backendErrorDeclaredValueRequired: 'La valeur declaree est obligatoire si l assurance est selectionnee.',
    backendErrorDeclaredValuePositive: 'La valeur declaree doit etre superieure a zero si l assurance est selectionnee.',
    backendErrorElevatorSelectionRequired: 'La presence d un ascenseur doit etre indiquee pour cette adresse.',
    backendErrorDeliveryAddressDifferent: 'L adresse de livraison doit etre differente de l adresse de collecte.',
    backendErrorSenderNotFound: 'Compte expediteur introuvable.',
    backendErrorUnsupportedPackageDocumentType: 'Ce type de document colis n est pas supporte.',
    backendErrorActiveDeliveryRouteNotFound: 'Tournee active introuvable.',
    backendErrorReservationNotFound: 'Reservation introuvable.',
    backendErrorDeliveryUserNotFound: 'Compte livreur introuvable.',
    backendErrorActiveRouteBlocksReservations: 'Une tournee active bloque deja les nouvelles reservations.',
    backendErrorPackageNotAvailableForReservation: 'Ce colis n est plus disponible a la reservation.',
    backendErrorPackageAlreadyReservedByCourier: 'Vous avez deja reserve ce colis.',
    backendErrorPackageSoftLocked: 'Un autre livreur consulte actuellement ce colis.',
    backendErrorInvalidPickupOtp: 'Le code de collecte est invalide.',
    backendErrorInvalidDeliveryOtp: 'Le code de livraison est invalide.',
    backendErrorDeliveryPersonLocationRequired: 'Votre position actuelle est obligatoire pour valider cette etape.',
    backendErrorRouteStopLocationMissing: 'La position de l arret est manquante pour cette tournee.',
    backendErrorDeliveryPersonNotAtStop: 'Vous devez etre proche de l arret pour valider cette etape.',
    backendErrorCourierSuspended: 'Ce compte livreur est temporairement suspendu.',
    backendErrorReservationCapacityReached: 'Votre capacite de reservation est atteinte.',
    backendErrorNotificationRecipientNotFound: 'Destinataire de notification introuvable.',
    backendErrorNotificationNotFound: 'Notification introuvable.',
    backendErrorDeviceRegistrationMissing: 'Les informations d enregistrement de l appareil sont manquantes.',
    backendErrorCaptchaUnsupported: 'La verification de securite n est pas correctement configuree.',
    backendErrorCaptchaUnavailable: 'La verification de securite est temporairement indisponible.',
    backendErrorCaptchaTokenRequired: 'Veuillez completer la verification de securite avant de continuer.',
    backendErrorCaptchaVerificationFailed: 'La verification de securite a echoue. Veuillez reessayer.',
    backendErrorCaptchaRejected: 'La verification de securite a ete refusee. Veuillez reessayer.',
    backendErrorPackageCategoryRequired: 'La categorie du colis est obligatoire.',
    backendErrorWeightPositive: 'Le poids du colis doit etre superieur a zero.',
    backendErrorDeliveryModeRequired: 'Le mode de livraison est obligatoire.',
    backendErrorDistancePositive: 'La distance du trajet doit etre superieure a zero.',
    backendErrorUnknownPackageCategory: 'Cette categorie de colis n est pas supportee.',
    backendErrorUnknownDeliveryMode: 'Ce mode de livraison n est pas supporte.',
    backendErrorRecipientNotFound: 'Destinataire introuvable.',
    errorActiveRouteStartNotAllowed: 'Cette tournee ne peut pas encore etre demarree.',
    errorReservationCancellationNotAllowed: 'Cette reservation ne peut plus etre annulee.',
    errorActiveRouteCancellationNotAllowed: 'Cette tournee active ne peut plus etre annulee.',
    errorDeliveryPersonLocationRequired: 'Votre position actuelle est obligatoire pour valider cette etape.',
    errorRouteStopLocationMissing: 'La position de l arret est manquante pour cette tournee.',
    errorDeliveryPersonNotAtStop: 'Vous devez etre proche de l arret pour valider cette etape.',
    errorCourierSuspended: 'Votre compte livreur est temporairement suspendu.',
    errorPackageReservationCapacityReached: 'Votre capacite de reservation est atteinte.',
  },
};

function translate(key) {
  if (!key) {
    return '';
  }
  if (i18n.global.te(key)) {
    return i18n.global.t(key);
  }
  const globalLocale = i18n.global.locale;
  const localeValue = typeof globalLocale === 'string' ? globalLocale : globalLocale?.value;
  const locale = String(localeValue || 'en').split('-')[0];
  return LOCAL_MESSAGES[locale]?.[key] || LOCAL_MESSAGES.en[key] || '';
}

function normalizeMessage(value) {
  if (value == null) {
    return '';
  }
  const raw = String(value).trim();
  if (!raw) {
    return '';
  }
  const quotedReason = raw.match(/^[1-5]\d{2}\s+[A-Z_]+\s+"(.+)"$/);
  if (quotedReason) {
    return quotedReason[1].trim();
  }
  return raw;
}

function extractResponseCandidates(data) {
  if (!data) {
    return [];
  }
  if (typeof data === 'string') {
    return [data];
  }
  if (Array.isArray(data)) {
    return data.flatMap(extractResponseCandidates);
  }

  const candidates = [
    data.code,
    data.message,
    data.detail,
    data.reason,
    data.error_description,
  ];

  if (Array.isArray(data.errors)) {
    data.errors.forEach((item) => {
      if (typeof item === 'string') {
        candidates.push(item);
      } else if (item) {
        candidates.push(item.defaultMessage, item.message, item.code);
      }
    });
  }

  return candidates.filter(Boolean);
}

function resolveMessageKey(message) {
  const normalized = normalizeMessage(message);
  if (!normalized) {
    return '';
  }
  if (EXACT_MESSAGE_KEYS[normalized]) {
    return EXACT_MESSAGE_KEYS[normalized];
  }
  if (i18n.global.te(normalized)) {
    return normalized;
  }
  const prefixMatch = PREFIX_MESSAGE_KEYS.find(([prefix]) => normalized.startsWith(prefix));
  return prefixMatch ? prefixMatch[1] : '';
}

export function resolveBackendErrorMessage(error) {
  const candidates = extractResponseCandidates(error?.response?.data);
  for (const candidate of candidates) {
    const key = resolveMessageKey(candidate);
    const translated = translate(key);
    if (translated) {
      return translated;
    }
  }

  for (const candidate of candidates) {
    const normalized = normalizeMessage(candidate);
    if (normalized && normalized.toLowerCase() !== 'error') {
      return normalized;
    }
  }

  const statusKey = HTTP_STATUS_KEYS[error?.response?.status];
  return translate(statusKey) || i18n.global.t('requestUnsuccessful');
}
