function normalizePartValue(value) {
  if (value === null || value === undefined) {
    return '';
  }
  return String(value);
}

function isFilePart(value) {
  return value && typeof value === 'object' && Object.prototype.hasOwnProperty.call(value, 'filename');
}

function appendTextPart(chunks, boundary, name, rawValue) {
  const value = normalizePartValue(rawValue);
  chunks.push(`--${boundary}\r\n`);
  chunks.push(`Content-Disposition: form-data; name="${name}"\r\n\r\n`);
  chunks.push(value);
  chunks.push('\r\n');
}

function appendFilePart(chunks, boundary, name, rawValue) {
  const fileName = normalizePartValue(rawValue.filename || `${name}.bin`);
  const contentType = normalizePartValue(rawValue.contentType || 'application/octet-stream');
  const content = normalizePartValue(rawValue.content || '');
  chunks.push(`--${boundary}\r\n`);
  chunks.push(`Content-Disposition: form-data; name="${name}"; filename="${fileName}"\r\n`);
  chunks.push(`Content-Type: ${contentType}\r\n\r\n`);
  chunks.push(content);
  chunks.push('\r\n');
}

export function buildMultipartFormData(fields) {
  const boundary = `----qd-k6-${Date.now()}-${Math.random().toString(16).slice(2)}`;
  const chunks = [];

  Object.entries(fields || {}).forEach(([name, rawValue]) => {
    if (Array.isArray(rawValue)) {
      rawValue.forEach((item) => {
        if (isFilePart(item)) {
          appendFilePart(chunks, boundary, name, item);
          return;
        }
        appendTextPart(chunks, boundary, name, item);
      });
      return;
    }

    if (isFilePart(rawValue)) {
      appendFilePart(chunks, boundary, name, rawValue);
      return;
    }

    appendTextPart(chunks, boundary, name, rawValue);
  });

  chunks.push(`--${boundary}--\r\n`);

  return {
    body: chunks.join(''),
    contentType: `multipart/form-data; boundary=${boundary}`,
  };
}
