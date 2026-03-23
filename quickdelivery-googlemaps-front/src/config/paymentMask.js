function sanitizeValue(value) {
  return String(value || '').trim();
}

export function maskCardNumber(cardNumber) {
  const sanitized = sanitizeValue(cardNumber).replace(/\s+/g, '');
  if (!sanitized) {
    return '';
  }

  const lastDigits = sanitized.slice(-4);
  return `**** **** **** ${lastDigits}`;
}

export function maskIban(iban) {
  const sanitized = sanitizeValue(iban).replace(/\s+/g, '');
  if (!sanitized) {
    return '';
  }

  if (sanitized.length <= 8) {
    return sanitized;
  }

  return `${sanitized.slice(0, 4)} **** **** ${sanitized.slice(-4)}`;
}

export function maskBic(bic) {
  const sanitized = sanitizeValue(bic).replace(/\s+/g, '');
  if (!sanitized) {
    return '';
  }

  if (sanitized.length <= 4) {
    return sanitized;
  }

  return `${sanitized.slice(0, 4)}****`;
}
