async function renderizarCarrusel(categoria, contenedorId) {
  const contenedor = document.getElementById(contenedorId);
  if (!contenedor) return;

  contenedor.innerHTML = '';
  const fnSkeleton = (typeof window !== 'undefined' && window.crearSkeletonCard) ? window.crearSkeletonCard : (typeof crearSkeletonCard === 'function' ? crearSkeletonCard : null);
  if (fnSkeleton) {
    for (let i = 0; i < 4; i++) {
      contenedor.appendChild(fnSkeleton());
    }
  }

  try {
    
    const fnObtener = (typeof window !== 'undefined' && window.obtenerProductosPorCategoria) ? window.obtenerProductosPorCategoria : (typeof obtenerProductosPorCategoria === 'function' ? obtenerProductosPorCategoria : null);
    const productos = fnObtener ? await fnObtener(categoria) : [];

    contenedor.innerHTML = '';

    if (productos.length === 0) {
      contenedor.innerHTML = '<div style="padding: 24px; color: var(--muted); font-size: 14px; text-align: center; width: 100%;">No hay productos registrados en esta sección actualmente.</div>';
      return;
    }

    productos.forEach(producto => {
      const articulo = document.createElement('div');
      articulo.className = 'carousel-item';

      articulo.onclick = () => {
        window.location.href = `producto.html?id=${producto.id}`;
      };

      const fab = producto.fabricante || 'RYA';
      const cat = producto.categoria || 'Componente';
      const nombreProducto = producto.nombre || cat;
      const precio = Number(producto.precio || 0).toFixed(2);

      const imgSrc = producto.imagenUrl || '';
      const imgContent = imgSrc
        ? `<img src="${imgSrc}" alt="${nombreProducto}" style="width:100%; height:100%; object-fit:contain; border-radius:12px;" onerror="this.parentElement.innerHTML='<span>[Foto ${fab}]</span>'">`
        : `<span>[Foto ${fab}]</span>`;

      articulo.innerHTML = `
        <div class="img-placeholder">
          ${imgContent}
        </div>
        <div class="product-info">
          <h4 style="color: var(--muted); font-size: 12px; margin-bottom: 4px;">${fab}</h4>
          <h3 style="font-size: 16px; margin: 0 0 8px 0;">${nombreProducto}</h3>
          <span class="price">S/ ${precio}</span>
        </div>
        <button class="add-to-cart" type="button" onclick="event.stopPropagation(); if (window.agregarAlCarrito) window.agregarAlCarrito('${producto.id}');">
          Añadir al carrito
        </button>
      `;

      contenedor.appendChild(articulo);
    });
  } catch (error) {
    console.error(`Error al cargar la categoría ${categoria}:`, error);
    contenedor.innerHTML = '';
  }
}

window.agregarAlCarrito = (id) => {
  if (window.Cart && window.Cart.addItemById) {
    window.Cart.addItemById(id);
  }
};

function inicializarAppHome() {
  if (window.__homeAppIniciada) return;
  window.__homeAppIniciada = true;

  if (typeof inicializarMonitorDeRed === 'function') inicializarMonitorDeRed();
  if (window.Cart && window.Cart.init) window.Cart.init();

  renderizarCarrusel('Placas Madre', 'track-placas');
  renderizarCarrusel('Procesadores', 'track-procesadores');
  renderizarCarrusel('Tarjetas Gráficas', 'track-gpus');

  const searchInput = document.getElementById('headerSearchInput');
  searchInput?.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && searchInput.value.trim()) {
      window.location.href = `index.html?search=${encodeURIComponent(searchInput.value.trim())}`;
    }
  });
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', inicializarAppHome);
} else {
  inicializarAppHome();
}