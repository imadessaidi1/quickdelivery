// i18n.js
import { createI18n } from 'vue-i18n';

const messages = {
  en: {
    //API URLS
    rootURL: 'http://localhost:8082/packages/v1/',
    createPackageUrl: 'create',
    reservePackageUrl: 'reserve?',
    getPackagesByDeliveryPersonUrl: 'getPackagesByDeliveryPerson?deliveryPersonID=',
    getPackagesAroundMe: 'packages-around-me?latitude=',
    //FOOTER LABELS
    applicationName: 'QuickDelivery',
    footerAssistance: 'Assistance',
    footerAssistanceHelp: 'Help Center',
    footerWelcome: 'Welcome',
    footerWelcomeForum: 'Community Forum',
    footerWelcomeAbout: 'About Us',
    footerWelcomeContact: 'Contact us',
    footerPrivacy: 'Privacy',
    footerConditions: 'Terms and conditions',
    footerCompany: 'Company Info',
    //NEW PACKAGE
    createNewPackage: 'Package info.',
    packageHeight: 'Height (cm)',
    packageWidth: 'Width (cm)',
    packageWeight: 'Weight (kg)',
    packageDepth: 'Length (cm)',
    packagePicture: 'Picture (Packages with a photo are more likely to quickly find a delivery person)',
    packageInvoice: 'Receipt/Invoice (Necessary to collect your package from a professional)',
    packagePrice: 'Price',
    packageDistanceToDestination: 'Distance to destination',
    packageDestination: 'Destination',
    packageDeparture: 'Departure',
    packageNextAction: 'Next',
    packagePreviousAction: 'Previous',
    packageSummaryAction: 'Summary',
    packageCreateAction: 'Create Package',
    packageAddressDepartureAddresses: 'Departure addresses',
    packageAddressArrivalAddresses: 'Arrival addresses',
    packageAddressAddress: 'Address',
    packageAddressFirstName: 'First name',
    packageAddressLastName: 'Last name',
    packageAddressLine1: 'Line 1',
    packageAddressLine2: 'Line 2',
    packageAddressCity: 'City',
    packageAddressZip: 'ZIP code',
    packageAddressCountry: 'Country',
    packageAddressEmail: 'Email',
    packageAddressPhone: 'Phone',
    packageAddressType: 'Type',
    packageAddressAddAction: 'Add address',
    packageAddressListActions: 'Actions',
    packageAddressListActionsDelete: 'Delete',
    //Packages Arround
    packagesArround: 'Packages around you',
    packagesArroundDistanceFromYou: 'Distance to package from your position',
    packagesArroundMArkerDetailActionsShowDirection: 'Direction',
    packagesArroundMArkerDetailActionsShowPackagesOnMyDirection: 'On my road',
    packagesArroundMArkerDetailActionsReserve: 'Reserve',
    //MENU
    menuHome: 'Home',
    menuMyPackages: 'My Packages',
    menuNewPackage: 'New Package',
    //MESSAGES
    messageExistingAddressType: 'Address type already added',
    messageExistingAddress: 'You have already entered this address',
  },
  fr: {
  //FOOTER LABELS
    greeting: 'Bonjour !',
    footerAssistance: 'Assistance',
    footerAssistanceHelp: 'Centre d\'aide',
    footerWelcome: 'Accueil',
    footerWelcomeForum: 'Forum de la communauté',
    applicationName: 'QuickDelivery',
    footerWelcomeAbout: 'À propos de nous',
    footerWelcomeContact: 'Nous contacter',
    footerPrivacy: 'Confidentialité',
    footerConditions: 'Conditions générales',
    footerCompany: 'Infos sur l\'entreprise',
    //Nouveau colis
    createNewPackage: 'Info. du colis',
    packageHeight: 'Hauteur (cm)',
    packageWidth: 'Largeur (cm)',
    packageWeight: 'Poids (kg)',
    packageDepth: 'Longueur (cm)',
    packagePrice: 'Prix (€)',
    packageDistanceToDestination: 'Distance jusqu\'à destination',
    packageDestination: 'Destination',
    packageDeparture: 'Depart',
    packagePicture: 'Photo (Les colis avec photos ont plus de chances de trouver rapidement un livreur)',
    packageInvoice: 'Ticket de caisse / Facture (Necessaire pour recuperer votre colis auprès d\'un professionnel)',
    packageNextAction: 'Suivant',
    packagePreviousAction: 'Precédent',
    packageSummaryAction: 'Resumé',
    packageCreateAction: 'Créer le package',
    packageAddressDepartureAddresses: 'Adresse de départ',
    packageAddressArrivalAddresses: 'Adresse d\'arrivée',
    packageAddressAddress: 'Adresse',
    packageAddressFirstName: 'Prénom',
    packageAddressLastName: 'Nom',
    packageAddressLine1: 'Ligne 1',
    packageAddressLine2: 'Ligne 2',
    packageAddressCity: 'Ville',
    packageAddressZip: 'Code postale',
    packageAddressCountry: 'Pays',
    packageAddressEmail: 'Email',
    packageAddressPhone: 'Téléphone',
    packageAddressType: 'Type',
    packageAddressAddAction: 'Ajouter l\'adresse',
    packageAddressListActions: 'Actions',
    packageAddressListActionsDelete: 'Supprimer',
    //Colis autour de vous
    packagesArround: 'Colis autour de vous',
    packagesArroundDistanceFromYou: 'Vers le colis de votre position',
    packagesArroundMArkerDetailActionsShowDirection: 'Itinéraire',
    packagesArroundMArkerDetailActionsShowPackagesOnMyDirection: 'Sur mon chemin',
    packagesArroundMArkerDetailActionsReserve: 'Reserver',
    //MENU
    menuHome: 'Accueil',
    menuMyPackages: 'Mes Colis',
    menuNewPackage: 'Neouveau Colis',
    //MESSAGE
    messageExistingAddressType: 'Type d\'adresse existant',
    messageExistingAddress: 'Vous avez déjà renseigné cette adresse',
  },
  // Ajoutez d'autres langues si nécessaire...
};
function detectBrowserLanguage() {
  const userLang = navigator.language || navigator.userLanguage;
  return userLang.split('-')[0]; // Récupérer la partie principale de la langue (par exemple, "fr" à partir de "fr-FR")
}
const i18n = createI18n({
  locale: detectBrowserLanguage(), // Langue par défaut
  fallbackLocale: 'en', // Langue de secours
  messages,
});

export default i18n;
