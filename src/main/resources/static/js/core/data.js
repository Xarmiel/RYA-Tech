const PRODUCTOS_DATA = [];

async function fetchProductos() {
  if (typeof window !== 'undefined' && window.api && typeof window.api.obtenerProductos === 'function') {
    return await window.api.obtenerProductos();
  }
  if (typeof window !== 'undefined' && typeof window.obtenerProductos === 'function') {
    return await window.obtenerProductos();
  }
  return [];
}

if (typeof window !== 'undefined') {
  window.PRODUCTOS_DATA = PRODUCTOS_DATA;
  window.fetchProductos = fetchProductos;
}
