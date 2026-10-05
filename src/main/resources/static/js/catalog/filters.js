let productosBase = []; 
let estadoFiltros = {
  categoriaActual: null,
  filtrosSeleccionados: {}
};

function notificarCambioFiltros() {
  window.dispatchEvent(new CustomEvent('filtrosActualizados', { detail: { ...estadoFiltros } }));
}

function obtenerEstadoFiltros() {
  return estadoFiltros;
}

function resetearFiltros() {
  estadoFiltros.categoriaActual = null;
  estadoFiltros.filtrosSeleccionados = {};
  procesarFiltros();
  notificarCambioFiltros();
}

async function inicializarFiltros() {
  try {
    const fetchFn = (typeof window !== 'undefined' && window.fetchProductos)
      ? window.fetchProductos
      : (typeof fetchProductos !== 'undefined' ? fetchProductos : null);
    
    if (fetchFn) {
      productosBase = await fetchFn();
    }
    procesarFiltros();

    document.getElementById('btnClearFilters')?.addEventListener('click', () => {
      estadoFiltros.filtrosSeleccionados = {};
      procesarFiltros();
      notificarCambioFiltros();
    });

    document.getElementById('btnApplyFilters')?.addEventListener('click', () => {
      notificarCambioFiltros();
    });
  } catch (error) {
    console.error("Error al inicializar los filtros:", error);
  }
}

function cambiarCategoria(nuevaCategoria) {
  estadoFiltros.categoriaActual = nuevaCategoria;
  estadoFiltros.filtrosSeleccionados = {};
  procesarFiltros();
  notificarCambioFiltros();
}

function procesarFiltros() {
  const container = document.getElementById('dynamicFilterContainer');
  if (!container) return;
  
  if (!estadoFiltros.categoriaActual) {
    renderizarUI({});
    actualizarContadorGlobal();
    return;
  }

  const reglasDep = (typeof window !== 'undefined' && window.REGLAS_DE_DEPENDENCIA) ? window.REGLAS_DE_DEPENDENCIA : (typeof REGLAS_DE_DEPENDENCIA !== 'undefined' ? REGLAS_DE_DEPENDENCIA : {});
  const filtrosGlob = (typeof window !== 'undefined' && window.FILTROS_GLOBALES) ? window.FILTROS_GLOBALES : (typeof FILTROS_GLOBALES !== 'undefined' ? FILTROS_GLOBALES : ["fabricante"]);

  let productosCategoria = productosBase.filter(p => p.categoria === estadoFiltros.categoriaActual);
  const permitidos = [...filtrosGlob, ...(reglasDep[estadoFiltros.categoriaActual]?.activa || [])];

  let productosFiltradosFinal = productosCategoria.filter(p => {
    let pasaFiltro = true;
    for (const [clave, valores] of Object.entries(estadoFiltros.filtrosSeleccionados)) {
      if (!valores || valores.length === 0) continue;
      const valorProducto = clave === 'fabricante' ? p[clave] : p.atributos?.[clave];
      if (!valores.includes(valorProducto)) {
        pasaFiltro = false;
        break;
      }
    }
    return pasaFiltro;
  });

  let opcionesDisponibles = {};
  permitidos.forEach(attr => opcionesDisponibles[attr] = {});

  productosFiltradosFinal.forEach(p => {
    permitidos.forEach(attr => {
      const valor = attr === 'fabricante' ? p[attr] : p.atributos?.[attr];
      if (valor) {
        opcionesDisponibles[attr][valor] = (opcionesDisponibles[attr][valor] || 0) + 1;
      }
    });
  });

  renderizarUI(opcionesDisponibles);
  actualizarContadorGlobal();
}

function renderizarUI(opcionesDisponibles) {
  const container = document.getElementById('dynamicFilterContainer');
  if (!container) return;
  container.innerHTML = '';

  const jerarquia = (typeof window !== 'undefined' && window.JERARQUIA_CATEGORIAS) ? window.JERARQUIA_CATEGORIAS : (typeof JERARQUIA_CATEGORIAS !== 'undefined' ? JERARQUIA_CATEGORIAS : {});
  const nombresLeg = (typeof window !== 'undefined' && window.NOMBRES_LEGIBLES) ? window.NOMBRES_LEGIBLES : (typeof NOMBRES_LEGIBLES !== 'undefined' ? NOMBRES_LEGIBLES : {});

  if (estadoFiltros.categoriaActual) {
    const allCatBtn = document.createElement('button');
    allCatBtn.type = 'button';
    allCatBtn.className = 'btn-all-categories';
    allCatBtn.style.cssText = 'width: 100%; background: rgba(0, 212, 255, 0.1); border: 1px solid var(--color-cyan); color: var(--color-cyan); padding: 8px 12px; border-radius: 8px; font-size: 13px; font-weight: 600; cursor: pointer; margin-bottom: 16px; text-align: center;';
    allCatBtn.innerHTML = `← Ver todo el catálogo (${estadoFiltros.categoriaActual} activo)`;
    allCatBtn.addEventListener('click', () => {
      cambiarCategoria(null);
    });
    container.appendChild(allCatBtn);
  }

  for (const [macroCategoria, categorias] of Object.entries(jerarquia)) {
    const macroDetails = document.createElement('details');
    macroDetails.className = 'macro-category-group';
    macroDetails.style.marginBottom = '12px'; 
    
    if (categorias.includes(estadoFiltros.categoriaActual)) macroDetails.open = true;

    const macroSummary = document.createElement('summary');
    macroSummary.textContent = macroCategoria;
    macroSummary.style.fontWeight = 'bold';
    macroSummary.style.cursor = 'pointer';
    macroDetails.appendChild(macroSummary);

    const macroContent = document.createElement('div');
    macroContent.style.paddingLeft = '15px'; 

    categorias.forEach(catNombre => {
      const catDetails = document.createElement('details');
      catDetails.className = 'category-group';
      catDetails.style.margin = '2px 0'; 
      
      const catSummary = document.createElement('summary');
      catSummary.textContent = catNombre;
      catSummary.style.cursor = 'pointer';
      catSummary.style.padding = '4px 0'; 

      catSummary.addEventListener('click', (e) => {
        if (estadoFiltros.categoriaActual !== catNombre) {
          e.preventDefault();
          cambiarCategoria(catNombre);
        }
      });

      catDetails.appendChild(catSummary);

      if (estadoFiltros.categoriaActual === catNombre) {
        catDetails.open = true; 
        catSummary.style.color = 'var(--color-cyan)';
        catSummary.style.fontWeight = 'bold';

        const filtersContainer = document.createElement('div');
        filtersContainer.style.paddingLeft = '15px'; 
        filtersContainer.style.borderLeft = '2px solid var(--color-cyan)';
        filtersContainer.style.marginTop = '8px';

        Object.keys(opcionesDisponibles).forEach(attrClave => {
          const opcionesMap = opcionesDisponibles[attrClave];
          const opcionesKeys = Object.keys(opcionesMap);

          if (opcionesKeys.length === 0) return;
          opcionesKeys.sort((a, b) => opcionesMap[b] - opcionesMap[a]);

          const titulo = nombresLeg[attrClave] || attrClave;

          const filterDetails = document.createElement('details');
          filterDetails.className = 'filter-group';
          filterDetails.open = true;
          filterDetails.style.marginBottom = '8px';

          const filterSummary = document.createElement('summary');
          filterSummary.textContent = titulo;
          filterSummary.style.fontSize = '0.9em';
          filterSummary.style.color = 'var(--color-gray)';
          filterDetails.appendChild(filterSummary);

          const divOptions = document.createElement('div');
          divOptions.className = 'filter-options';
          divOptions.style.paddingLeft = '10px';

          opcionesKeys.forEach(valor => {
            const cantidad = opcionesMap[valor];
            const isChecked = estadoFiltros.filtrosSeleccionados[attrClave]?.includes(valor) || false;

            const label = document.createElement('label');
            label.style.display = 'block';
            label.style.fontSize = '0.85em';
            label.style.marginTop = '2px'; 
            label.style.marginBottom = '2px';
            
            const checkbox = document.createElement('input');
            checkbox.type = 'checkbox';
            checkbox.value = valor;
            checkbox.checked = isChecked;

            checkbox.addEventListener('change', (e) => {
              if (!estadoFiltros.filtrosSeleccionados[attrClave]) {
                estadoFiltros.filtrosSeleccionados[attrClave] = [];
              }
              if (e.target.checked) {
                estadoFiltros.filtrosSeleccionados[attrClave].push(valor);
              } else {
                estadoFiltros.filtrosSeleccionados[attrClave] = estadoFiltros.filtrosSeleccionados[attrClave].filter(v => v !== valor);
              }
              procesarFiltros();
              notificarCambioFiltros();
            });

            label.appendChild(checkbox);
            label.appendChild(document.createTextNode(` ${valor} (${cantidad})`));
            divOptions.appendChild(label);
          });

          filterDetails.appendChild(divOptions);
          filtersContainer.appendChild(filterDetails);
        });

        catDetails.appendChild(filtersContainer);
      }
      macroContent.appendChild(catDetails);
    });

    macroDetails.appendChild(macroContent);
    container.appendChild(macroDetails);
  }
}

function actualizarContadorGlobal() {
  const filterCountLabel = document.getElementById('filterCount');
  let totalSeleccionados = 0;
  Object.values(estadoFiltros.filtrosSeleccionados).forEach(arr => {
    totalSeleccionados += arr.length;
  });
  if (filterCountLabel) {
    filterCountLabel.textContent = `(${totalSeleccionados})`;
  }
}

function inicializarSidebar() {
  const btnOpenFilters = document.getElementById('btnOpenFilters');
  const btnCloseFilters = document.getElementById('btnCloseFilters');
  const filterSidebar = document.getElementById('filterSidebar');
  const filterOverlay = document.getElementById('filterOverlay');
  const btnApplyFilters = document.getElementById('btnApplyFilters');

  const toggleFilters = (show) => {
    if (filterSidebar && filterOverlay) {
      filterSidebar.classList.toggle('active', show);
      filterOverlay.classList.toggle('active', show);
    }
  };

  btnOpenFilters?.addEventListener('click', () => toggleFilters(true));
  btnCloseFilters?.addEventListener('click', () => toggleFilters(false));
  filterOverlay?.addEventListener('click', () => toggleFilters(false));
  btnApplyFilters?.addEventListener('click', () => toggleFilters(false));
}

if (typeof window !== 'undefined') {
  window.estado = estadoFiltros;
  window.obtenerEstadoFiltros = obtenerEstadoFiltros;
  window.resetearFiltros = resetearFiltros;
  window.inicializarFiltros = inicializarFiltros;
  window.cambiarCategoria = cambiarCategoria;
  window.inicializarSidebar = inicializarSidebar;
}
