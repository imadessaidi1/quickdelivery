export function validateNumericField(value) {
    if (!value) {
      return this.$i18n.t('mandatoryField');
    }else if(value <= 0){
       return this.$i18n.t('nonNegativeField');
    }
    return true;
}

export function validateRequired(value) {
    if (!value) {
      return this.$i18n.t('mandatoryField');
    }
    return true;
}

export function validateNumericFieldAcceptZero(value) {
    if(value === 0){
        return true;
    }
    else if (!value) {
      return this.$i18n.t('mandatoryField');
    }else if(value < 0){
       return this.$i18n.t('nonNegativeField');
    }
    return true;
}

export function validateEmail(value) {
    if (!value) {
      return this.$i18n.t('mandatoryField');
    }
    const regex = /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,4}$/i;
    if (!regex.test(value)) {
      return this.$i18n.t('invalidEmail');
    }
    return true;
}
export function validateString(value) {
    if (!value) {
      return this.$i18n.t('mandatoryField');
    }
    const normalizedValue = String(value).trim();
    const regex = /^[\p{L}]+(?:[ '-][\p{L}]+)*$/u;
    if (!regex.test(normalizedValue)) {
      return this.$i18n.t('invalidString');
    }
    return true;
}
export function validatePhone(value) {
    if (!value) {
      return this.$i18n.t('mandatoryField');
    }
    const regex = /^\+[1-9]\d{5,14}$/;
    if (!regex.test(value)) {
      return this.$i18n.t('invalidPhone');
    }
    return true;
}

export function validateAddress(value){
    if(!value){
        return false;
    }else if(value.split(',').map((chunk) => chunk.trim()).filter(Boolean).length < 3){
        return false;
    }else{
        return true;
    }
}

export function validateDeliveryDateTime(pickupDateTime, deliveryDateTime){
    if(deliveryDateTime < pickupDateTime){
        return false;
    }else{
        return true;
    }
}

export function validateIBAN(input) {
           if (!input) {
               return this.$i18n.t('mandatoryField');
           }
           var CODE_LENGTHS = {
               AD: 24, AE: 23, AT: 20, AZ: 28, BA: 20, BE: 16, BG: 22, BH: 22, BR: 29,
               CH: 21, CR: 21, CY: 28, CZ: 24, DE: 22, DK: 18, DO: 28, EE: 20, ES: 24,
               FI: 18, FO: 18, FR: 27, GB: 22, GI: 23, GL: 18, GR: 27, GT: 28, HR: 21,
               HU: 28, IE: 22, IL: 23, IS: 26, IT: 27, JO: 30, KW: 30, KZ: 20, LB: 28,
               LI: 21, LT: 20, LU: 20, LV: 21, MC: 27, MD: 24, ME: 22, MK: 19, MR: 27,
               MT: 31, MU: 30, NL: 18, NO: 15, PK: 24, PL: 28, PS: 29, PT: 25, QA: 29,
               RO: 24, RS: 22, SA: 24, SE: 24, SI: 19, SK: 24, SM: 27, TN: 24, TR: 26,
               AL: 28, BY: 28, EG: 29, GE: 22, IQ: 23, LC: 32, SC: 31, ST: 25,
               SV: 28, TL: 23, UA: 29, VA: 22, VG: 24, XK: 20
           };
           var iban = String(input).toUpperCase().replace(/[\s-]/g, '').replace(/[^A-Z0-9]/g, ''), // keep only alphanumeric characters
                   code = iban.match(/^([A-Z]{2})(\d{2})([A-Z\d]+)$/), // match and capture (1) the country code, (2) the check digits, and (3) the rest
                   digits;
           // check syntax and length
           if (!code || iban.length !== CODE_LENGTHS[code[1]]) {
               return this.$i18n.t('IBANInvalid');
           }
           // rearrange country code and check digits, and convert chars to ints
           digits = (code[3] + code[1] + code[2]).replace(/[A-Z]/g, function (letter) {
               return letter.charCodeAt(0) - 55;
           });
           // final check
           if (mod97(digits) != 1){
              return this.$i18n.t('IBANInvalid');
           }else{
              return true;
           }
       }

       function mod97(string) {
           var checksum = string.slice(0, 2), fragment;
           for (var offset = 2; offset < string.length; offset += 7) {
               fragment = String(checksum) + string.substring(offset, offset + 7);
               checksum = parseInt(fragment, 10) % 97;
           }
           return checksum;
       }

export function validateBIC(bic) {
            if (!bic) {
                return this.$i18n.t('mandatoryField');
            }else{
           // Supprimer les espaces
           bic = bic.replace(/\s/g,'');

           // Vérifier la longueur du code BIC
           if (bic.length < 8 || bic.length > 11) {
               return this.$i18n.t('bicLength');
           }

           // Vérifier que le code BIC est composé uniquement de lettres et de chiffres
           if (!/^[0-9a-zA-Z]+$/.test(bic)) {
               return this.$i18n.t('ibanCharAndNumericOnly');
           }

           // Vérifier que les 4 premiers caractères sont alphabétiques
           if (!/^[a-zA-Z]{4}/.test(bic)) {
               return this.$i18n.t('bicCharOnly');
           }

           // Vérifier que les caractères 5-6 sont alphabétiques ou numériques
           /*if (!/^[0-9a-zA-Z]{2}/.test(bic.substring(4, 6))) {
               return false;
           }*/

           // Vérifier que les caractères 7-8 (optionnels) sont alphabétiques ou numériques
          /* if (bic.length === 11 && !/^[0-9a-zA-Z]{2}/.test(bic.substring(6, 8))) {
               return false;
           }*/

           return true;
          }
}

export function validateCreditCardNumber(cardNumber) {
            if (!cardNumber) {
                return this.$i18n.t('mandatoryField');
            }else{
           // Supprimer les espaces et tirets de la saisie
           cardNumber = cardNumber.replace(/[\s-]/g, '');

           // Vérifier que la carte contient uniquement des chiffres
           if (!/^\d+$/.test(cardNumber)) {
               return this.$i18n.t('cardNumericOnly');
           }

           // Vérifier la longueur de la carte (généralement entre 13 et 19 chiffres)
           if (cardNumber.length < 13 || cardNumber.length > 19) {
               return this.$i18n.t('cardLength');
           }

           // Luhn algorithm pour la validation de la carte de crédit
           let sum = 0;
           let doubleUp = false;

           // Parcourir la carte de droite à gauche
           for (let i = cardNumber.length - 1; i >= 0; i--) {
               let curDigit = parseInt(cardNumber.charAt(i), 10);

               // Si on a déjà doublé, prendre le chiffre courant
               if (doubleUp) {
                   if ((curDigit *= 2) > 9) curDigit -= 9;
               }

               // Ajouter le chiffre courant à la somme
               sum += curDigit;

               // Alterner le flag doubleUp
               doubleUp = !doubleUp;
           }

           // La carte est valide si la somme est un multiple de 10
           if(!(sum % 10 === 0)){
               return this.$i18n.t('cardWrongNumber');
           }

           return true;
           }
}

export function validateCreditCardExpiration(expirationDate) {
            if (!expirationDate) {
                return this.$i18n.t('mandatoryField');
            }else{
           // Vérifier si la chaîne est au format "mm/yyyy"
           const expirationPattern = /^(0[1-9]|1[0-2])\/\d{4}$/;
           if (!expirationPattern.test(expirationDate)) {
               return this.$i18n.t('cardWrongExpiryDate');
           }


           // Extraire le mois et l'année de la date d'expiration
           const [month, year] = expirationDate.split('/');
           if (month < 1 || month > 12) {
               return this.$i18n.t('cardWrongMonth');
           }
           const expirationMonth = parseInt(month, 10);
           const expirationYear = parseInt(year, 10);

           // Obtenir la date actuelle
           const currentDate = new Date();
           const currentYear = currentDate.getFullYear();
           const currentMonth = currentDate.getMonth() + 1; // Les mois sont indexés à partir de zéro

           // Vérifier si l'année d'expiration est valide
           if (expirationYear < currentYear || expirationYear > currentYear + 10) {
               return this.$i18n.t('cardExpired');
           }

           // Si l'année d'expiration est égale à l'année actuelle, vérifier le mois
           if (expirationYear === currentYear) {
               if (expirationMonth < currentMonth || expirationMonth > 12) {
                   return this.$i18n.t('cardExpired');
               }
           }

           // Toutes les conditions sont remplies, la date d'expiration est valide
           return true;
           }
}

export function validateCreditCardCVV(cvv) {
            if (!cvv) {
                return this.$i18n.t('mandatoryField');
            }else{
           // Vérifier si le CVV est composé de 3 ou 4 chiffres
           const cvvPattern = /^[0-9]{3,4}$/;
           if(!cvvPattern.test(cvv)){
                return this.$i18n.t('cardNumericOnly');
           }else{
                return true;
           }
           }
}

export function validateCarRegistrationNumber(registrationNumber) {
            if (!registrationNumber) {
                return this.$i18n.t('mandatoryField');
            }else{
           const normalizedRegistrationNumber = registrationNumber.trim().toUpperCase();
           // Format français SIV: AA-123-AA, with optional spaces or dashes in input
           const regex = /^[A-Z]{2}[- ]?\d{3}[- ]?[A-Z]{2}$/;

           // Vérifier si le numéro d'immatriculation correspond à l'expression régulière
           if(!regex.test(normalizedRegistrationNumber)){
                return this.$i18n.t('incorrectCarRegistrationNumber');
           }
           return true;
           }
}

export function validatePassword(password) {
           const regex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*()_+={}[\]:;'"|\\<>,.?/~]).{8,}$/;

           if (!password) {
               return this.$i18n.t('mandatoryField');
           } else if (!regex.test(password)) {
               return this.$i18n.t('incorrectPasswordFormat');
           } else {
               return true;
           }
}

export function validatePasswordConfirmation(password, confirmation) {
           if (!confirmation || password != confirmation) {
               return false;
           } else {
               return true;
           }
}

export function validateEmailConfirmation(email, confirmation) {
           if (!confirmation || email != confirmation) {
               return false;
           } else {
               return true;
           }
}
export function validatePhoneConfirmation(phone, confirmation) {
           if (!confirmation || phone != confirmation) {
               return false;
           } else {
               return true;
           }
}

export function validateFileInput(selectedFilesKeys, documentsList){
    let results = [];
    if(selectedFilesKeys){
        selectedFilesKeys.forEach(key => {
              if(!documentsList[key]?.file){
                results.push({status : false, missingKey: key});
              }
        });
    }
    return results;
}
export function getArrivalAddress(addresses) {
           for (let i = 0; i < addresses.length; i++) {
               const address = addresses[i];
               if (address.type === 'ARRIVAL') {
                   return address;
               }
           }
           return null;
       }

export function getDepartureAddress(addresses) {
           for (let i = 0; i < addresses.length; i++) {
               const address = addresses[i];
               if (address.type === 'DEPARTURE') {
                   return address;
               }
           }
           return null;
       }
