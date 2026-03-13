const LEGAL_SESSION_PREFIX = 'quickdelivery-legal-consulted:';
const LEGAL_DRAFT_PREFIX = 'quickdelivery-legal-draft:';

export const LEGAL_FLOW_ACCOUNT_CREATION = 'account-creation';
export const LEGAL_FLOW_PACKAGE_CREATION = 'package-creation';

function getStorageKey(flow) {
  return `${LEGAL_SESSION_PREFIX}${flow}`;
}

export function markLegalPageConsulted(flow) {
  if (!flow || typeof window === 'undefined') {
    return;
  }

  window.sessionStorage.setItem(getStorageKey(flow), 'true');
}

export function hasLegalPageBeenConsulted(flow) {
  if (!flow || typeof window === 'undefined') {
    return false;
  }

  return window.sessionStorage.getItem(getStorageKey(flow)) === 'true';
}

export function saveLegalDraft(flow, draft) {
  if (!flow || typeof window === 'undefined') {
    return;
  }

  window.sessionStorage.setItem(`${LEGAL_DRAFT_PREFIX}${flow}`, JSON.stringify(draft));
}

export function loadLegalDraft(flow) {
  if (!flow || typeof window === 'undefined') {
    return null;
  }

  const raw = window.sessionStorage.getItem(`${LEGAL_DRAFT_PREFIX}${flow}`);
  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw);
  } catch (error) {
    window.sessionStorage.removeItem(`${LEGAL_DRAFT_PREFIX}${flow}`);
    return null;
  }
}

export function clearLegalDraft(flow) {
  if (!flow || typeof window === 'undefined') {
    return;
  }

  window.sessionStorage.removeItem(`${LEGAL_DRAFT_PREFIX}${flow}`);
}
