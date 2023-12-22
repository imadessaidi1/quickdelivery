// i18n.js
import { createI18n } from 'vue-i18n';

const messages = {
  en: {
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
    createNewPackage: 'New package',
    packageHeight: 'Height',
    packageWidth: 'Width',
    packageWeight: 'Weight',
    packageDepth: 'Depth',
    packagePicture: 'Picture URL',
    packagePrice: 'Price',
    packageCreateAction: 'Create Package',
    packageAddressFirstName: 'First name',
    packageAddressLastName: 'Last name',
    packageAddressLine1: 'Line 1',
    packageAddressLine2: 'Line 2',
    packageAddressCity: 'City',
    packageAddressZip: 'ZIP code',
    packageAddressCountry: 'Country',
    packageAddressType: 'Type',
    packageAddressAddAction: 'Add address',
    packageAddressListActions: 'Actions',
    packageAddressListActionsDelete: 'Delete',
    //Packages Arround
    packagesArround: 'Packages around you',
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
    createNewPackage: 'Nouveau Colis',
    packageHeight: 'Hauteur',
    packageWidth: 'Largeur',
    packageWeight: 'Poids',
    packageDepth: 'Profondeur',
    packagePrice: 'Prix',
    packagePicture: 'URL de la photo',
    packageCreateAction: 'Créer le package',
    packageAddressFirstName: 'Prénom',
    packageAddressLastName: 'Nom',
    packageAddressLine1: 'Ligne 1',
    packageAddressLine2: 'Ligne 2',
    packageAddressCity: 'Ville',
    packageAddressZip: 'Code postale',
    packageAddressCountry: 'Pays',
    packageAddressType: 'Type',
    packageAddressAddAction: 'Ajouter l\'adresse',
    packageAddressListActions: 'Actions',
    packageAddressListActionsDelete: 'Supprimer',
    //Colis autour de vous
    packagesArround: 'Colis autour de vous',
  },
  // Ajoutez d'autres langues si nécessaire...
};
function detectBrowserLanguage() {
  const userLang = navigator.language || navigator.userLanguage;
  return userLang.split('-')[0]; // Récupérer la partie principale de la langue (par exemple, "fr" à partir de "fr-FR")
}
const i18n = createI18n({
  locale: detectBrowserLanguage(), // Langue par défaut
  fallbackLocale: 'fr', // Langue de secours
  messages,
});

export default i18n;
