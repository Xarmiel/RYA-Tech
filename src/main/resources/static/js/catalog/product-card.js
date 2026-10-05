

function limpiarValor(valor) {
  if (!valor) return '';
  return valor
    .toString()
    .replace(/\s*\([^)]*\)\s*$/, '') 
    .replace(/^Para\s+/i, '')        
    .trim();
}

function obtenerSpecs(producto) {
  if (!producto) return [];

  const valores = [];

  if (Array.isArray(producto.especificaciones) && producto.especificaciones.length > 0) {
    for (const esp of producto.especificaciones) {
      if (!esp) continue;
      const clave = (esp.clave || '').toString().toLowerCase().trim();
      if (clave === 'fabricante') continue;

      const valorLimpio = limpiarValor(esp.valor);
      if (valorLimpio && !valores.includes(valorLimpio)) {
        valores.push(valorLimpio);
        if (valores.length === 3) break;
      }
    }
  }

  if (valores.length < 3 && producto.atributos && typeof producto.atributos === 'object') {
    for (const [clave, val] of Object.entries(producto.atributos)) {
      if (clave.toLowerCase().trim() === 'fabricante') continue;

      const valorLimpio = limpiarValor(val);
      if (valorLimpio && !valores.includes(valorLimpio)) {
        valores.push(valorLimpio);
        if (valores.length === 3) break;
      }
    }
  }

  return valores;
}

function escapeHtmlProductCard(texto) {
  if (!texto) return '';
  return texto.toString().replace(/[&<>"']/g, (m) => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;'
  }[m]));
}

/**
 * Crea el elemento DOM de una tarjeta de producto (usada en carruseles y grillas).
 */
function crearTarjetaProducto(producto, soloLectura = false) {
  const articulo = document.createElement('div');
  articulo.className = 'carousel-item';
  if (!soloLectura) {
    articulo.onclick = () => {
      window.location.href = `producto.html?id=${producto.id}`;
    };
  } else {
    articulo.style.cursor = 'default';
  }

  const specs = obtenerSpecs(producto);
  const precioFormateado = Number(producto.precio || 0).toFixed(2);
  const fab = escapeHtmlProductCard(producto.fabricante);
  const nombreProducto = escapeHtmlProductCard(producto.nombre || producto.categoria);
  const cat = escapeHtmlProductCard(producto.categoria);

  const imgSrc = producto.imagenUrl ? escapeHtmlProductCard(producto.imagenUrl) : '';
  const imgContent = imgSrc 
    ? `<img src="${imgSrc}" alt="${nombreProducto}" style="width:100%; height:100%; object-fit:contain; border-radius:12px;" onerror="this.parentElement.innerHTML='<span>[Foto ${fab}]</span>'">`
    : `<span>[Foto ${fab}]</span>`;

  articulo.innerHTML = `
    <div class="img-placeholder">
      ${imgContent}
    </div>
    <div class="product-info">
      <span class="product-brand">${fab}</span>
      <h3 class="product-name">${nombreProducto}</h3>
      ${specs.length ? `<p class="product-specs">${specs.map(s => escapeHtmlProductCard(s)).join(' &nbsp;|&nbsp; ')}</p>` : ''}
      <span class="price">S/ ${precioFormateado}</span>
    </div>
    <button class="add-to-cart" type="button" ${soloLectura ? 'disabled style="opacity:0.5; cursor:not-allowed;"' : `onclick="event.stopPropagation(); if (window.agregarAlCarrito) window.agregarAlCarrito('${escapeHtmlProductCard(producto.id)}');"`}>
      Añadir al carrito
    </button>
  `;

  return articulo;
}

if (typeof window !== 'undefined') {
  window.crearTarjetaProducto = crearTarjetaProducto;
}