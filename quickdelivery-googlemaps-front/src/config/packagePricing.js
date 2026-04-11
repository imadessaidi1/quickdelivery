import { getCurrentUserRoles } from './auth';

function asNumber(value) {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
}

export function isCourierPricingView() {
  const roles = getCurrentUserRoles();
  return roles.includes('ROLE_LIVREUR') && !roles.includes('ROLE_CLIENT') && !roles.includes('ROLE_CLIENT_PRO');
}

function resolveCustomerAmount(package_) {
  const customerTotalPrice = asNumber(package_?.customerTotalPrice);
  if (customerTotalPrice != null) {
    return customerTotalPrice;
  }

  const deliveryPrice = asNumber(package_?.deliveryPrice);
  if (deliveryPrice != null) {
    return deliveryPrice;
  }

  const base = asNumber(package_?.deliveryBaseAmount) || 0;
  const insurance = asNumber(package_?.insuranceFee) || 0;
  const serviceFee = asNumber(package_?.platformServiceFee) || 0;
  const computed = base + insurance + serviceFee;
  return computed > 0 ? computed : null;
}

function resolveCourierAmount(package_) {
  const courierPayoutAmount = asNumber(package_?.courierPayoutAmount);
  if (courierPayoutAmount != null) {
    return courierPayoutAmount;
  }

  const revenue = asNumber(package_?.deliveryRevenueExcludingServiceFee);
  const shareRate = asNumber(package_?.courierShareRate);
  if (revenue != null && shareRate != null) {
    return revenue * shareRate;
  }

  const deliveryPrice = asNumber(package_?.deliveryPrice);
  return deliveryPrice;
}

export function resolveDisplayedPackageAmount(package_) {
  return isCourierPricingView() ? resolveCourierAmount(package_) : resolveCustomerAmount(package_);
}

export function formatDisplayedPackageAmount(i18n, package_) {
  const amount = resolveDisplayedPackageAmount(package_);
  if (amount == null) {
    return '-';
  }
  return `${amount.toFixed(2)} ${i18n.t('currency')}`;
}

export function resolveDisplayedPackagePriceLabel(i18n) {
  return i18n.t(isCourierPricingView() ? 'packagePriceCourierShare' : 'packagePriceCustomerTotal');
}
