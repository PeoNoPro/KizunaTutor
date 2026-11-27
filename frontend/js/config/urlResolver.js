/**
 * Resolves backend API and Agent Memory base URLs depending on whether
 * the frontend is served through the Node/Caddy reverse proxy or opened
 * directly from a local dev server / file:// URL.
 */

export function isProxiedOrigin(loc = typeof window !== 'undefined' ? window.location : undefined) {
  return Boolean(
    loc &&
    typeof loc.protocol === 'string' &&
    loc.protocol.startsWith('http') &&
    (loc.port === '3000' || loc.port === '' || (loc.hostname !== 'localhost' && loc.hostname !== '127.0.0.1'))
  );
}

export function resolveApiBaseUrl(loc = typeof window !== 'undefined' ? window.location : undefined) {
  return isProxiedOrigin(loc) ? '/api' : 'http://localhost:8085/api';
}

export function resolveMemoryUrl(loc = typeof window !== 'undefined' ? window.location : undefined) {
  return isProxiedOrigin(loc) ? '/memory' : 'http://localhost:8081/memory';
}
