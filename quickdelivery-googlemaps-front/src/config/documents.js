export function normalizeDocumentCollection(documents) {
  if (Array.isArray(documents)) {
    return documents.reduce((accumulator, document) => {
      const key = `${document?.type || document?.key || ''}`.trim();
      if (!key) {
        return accumulator;
      }
      accumulator[key] = document;
      return accumulator;
    }, {});
  }

  if (documents && typeof documents === 'object') {
    return documents;
  }

  return {};
}
