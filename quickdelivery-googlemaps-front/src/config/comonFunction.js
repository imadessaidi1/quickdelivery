export function validateNumericField(value) {
    if (!value) {
      return this.$i18n.t('mandatoryField');
    }else if(value <= 0){
       return this.$i18n.t('nonNegativeField');
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
    const regex = /^[a-zA-Z]+$/;
    if (!regex.test(value)) {
      return this.$i18n.t('invalidString');
    }
    return true;
}
export function validatePhone(value) {
    if (!value) {
      return this.$i18n.t('mandatoryField');
    }
    const regex = /^(\+\d{1,4}|00\d{1,4}|0)([1-9]\d{9})$/;
    if (!regex.test(value)) {
      return this.$i18n.t('invalidPhone');
    }
    return true;
}

export function validateAddress(value){
    if(!value){
        return false;
    }else if(value.split(',').length !== 3){
        return false;
    }else{
        return true;
    }
}