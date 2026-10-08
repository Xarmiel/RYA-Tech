(function () {
  'use strict';

  function protegerVistaAdmin() {
    try {
      const raw = sessionStorage.getItem('rya_usuario');
      if (!raw) {
        window.location.replace('login.html');
        return;
      }
      const user = JSON.parse(raw);
      const rolNombre = typeof user.rol === 'string' ? user.rol : (user.rol && user.rol.nombre);
      if (rolNombre !== 'ADMIN') {
        window.location.replace('inicio.html');
      }
    } catch (e) {
      window.location.replace('login.html');
    }
  }

  protegerVistaAdmin();

  let categoriasCache = [];
  let productoEncontradoEditar = null;
  let productoEncontradoEliminar = null;

  async function cargarCategorias() {
    try {
      categoriasCache = await window.api.obtenerCategorias();
      poblarSelectoresSubcategorias();
    } catch (e) {
      console.warn('Error cargando categorías:', e);
    }
  }

  const ESPECIFICACIONES_COMPONENTES = {
    "placas-madre": [
      { clave: "Fabricante_placa", label: "Fabricante", tipo: "text", placeholder: "Ej: ASUS" },
      { clave: "Chipset_placa", label: "Chipset", tipo: "text", placeholder: "Ej: AMD B650" },
      { clave: "Socket_placa", label: "Socket", tipo: "text", placeholder: "Ej: AM5" },
      { clave: "Tamaño_placa", label: "Tamaño", tipo: "radio", opciones: ["E-ATX", "ATX", "micro-ATX", "mini-ITX"] },
      { clave: "Generacion_placa", label: "Generación de Memoria", tipo: "text", placeholder: "Ej: DDR5" },
      { clave: "PuertosRAM_placa", label: "Puertos RAM", tipo: "text", placeholder: "Ej: 4" },
      { clave: "PuertosM2_placa", label: "Puertos M.2", tipo: "text", placeholder: "Ej: 2" },
      { clave: "Conectividad_placa", label: "Conectividad", tipo: "radio", opciones: ["WIFI", "ETHERNET"] }
    ],
    "procesadores": [
      { clave: "Fabricante_cpu", label: "Fabricante", tipo: "text", placeholder: "Ej: AMD" },
      { clave: "Nucleos_cpu", label: "Núcleos", tipo: "text", placeholder: "Ej: 8" },
      { clave: "Hilos_cpu", label: "Hilos", tipo: "text", placeholder: "Ej: 16" },
      { clave: "Frecuencia_cpu", label: "Frecuencia", tipo: "text", placeholder: "Ej: 4.2GHz" },
      { clave: "Cache_cpu", label: "Memoria Caché", tipo: "text", placeholder: "Ej: L3 96MB" },
      { clave: "Socket_cpu", label: "Socket", tipo: "text", placeholder: "Ej: AM5" },
      { clave: "Graficos", label: "Gráficos Integrados", tipo: "radio", opciones: ["true", "false"] },
      { clave: "TDP_cpu", label: "TDP", tipo: "text", placeholder: "Ej: TDP 120W" },
      { clave: "Consumo_cpu", label: "Consumo", tipo: "text", placeholder: "Ej: 162W" }
    ],
    "tarjetas-graficas": [
      { clave: "Fabricante_tarjeta", label: "Fabricante", tipo: "text", placeholder: "Ej: NVIDIA" },
      { clave: "GPU_tarjeta", label: "Chip GPU", tipo: "text", placeholder: "Ej: AD104" },
      { clave: "CapacidadVRAM_tarjeta", label: "Capacidad VRAM", tipo: "text", placeholder: "Ej: 12GB" },
      { clave: "TipoVRAM_tarjeta", label: "Tipo VRAM", tipo: "text", placeholder: "Ej: GDDR6" },
      { clave: "HDMI_tarjeta", label: "Puerto HDMI", tipo: "radio", opciones: ["true", "false"] },
      { clave: "DisplayPort_tarjeta", label: "Puerto DisplayPort", tipo: "radio", opciones: ["true", "false"] },
      { clave: "Largo_tarjeta", label: "Largo", tipo: "text", placeholder: "Ej: 201mm" },
      { clave: "Ancho_tarjeta", label: "Ancho", tipo: "text", placeholder: "Ej: 2.5 ranuras" },
      { clave: "Consumo_tarjeta", label: "Consumo", tipo: "text", placeholder: "Ej: 170W" }
    ],
    "memoria-ram": [
      { clave: "Fabricante_ram", label: "Fabricante", tipo: "text", placeholder: "Ej: KINGSTON" },
      { clave: "Generacion_ram", label: "Generación", tipo: "text", placeholder: "Ej: DDR5" },
      { clave: "Capacidad_ram", label: "Capacidad", tipo: "text", placeholder: "Ej: 32GB" },
      { clave: "Consumo_ram", label: "Consumo", tipo: "text", placeholder: "Ej: 20W" }
    ],
    "almacenamiento": [
      { clave: "Fabricante_almacenamiento", label: "Fabricante", tipo: "text", placeholder: "Ej: KINGSTON" },
      { clave: "FactorForma_almacenamiento", label: "Factor de Forma", tipo: "text", placeholder: "Ej: M.2/SATA" },
      { clave: "Capacidad_almacenamiento", label: "Capacidad", tipo: "text", placeholder: "Ej: 500GB" },
      { clave: "VelocidadEscritura_almacenamiento", label: "Velocidad de Escritura", tipo: "text", placeholder: "Ej: 2100 MB/s" },
      { clave: "VelocidadLectura_almacenamiento", label: "Velocidad de Lectura", tipo: "text", placeholder: "Ej: 3500 MB/s" },
      { clave: "Consumo_almacenamiento", label: "Consumo", tipo: "text", placeholder: "Ej: 4W" }
    ],
    "fuente-de-poder": [
      { clave: "Fabricante_fuente", label: "Fabricante", tipo: "text", placeholder: "Ej: Corsair" },
      { clave: "Certificacion_fuente", label: "Certificación", tipo: "text", placeholder: "Ej: 80 PLUS GOLD" },
      { clave: "Potencia_fuente", label: "Potencia", tipo: "text", placeholder: "Ej: 850W" },
      { clave: "OVP_fuente", label: "Protección OVP", tipo: "radio", opciones: ["true", "false"] },
      { clave: "OTP_fuente", label: "Protección OTP", tipo: "radio", opciones: ["true", "false"] },
      { clave: "OCP_fuente", label: "Protección OCP", tipo: "radio", opciones: ["true", "false"] },
      { clave: "OPP_fuente", label: "Protección OPP", tipo: "radio", opciones: ["true", "false"] },
      { clave: "UVP_fuente", label: "Protección UVP", tipo: "radio", opciones: ["true", "false"] },
      { clave: "SCP_fuente", label: "Protección SCP", tipo: "radio", opciones: ["true", "false"] }
    ],
    "gabinete": [
      { clave: "Fabricante_case", label: "Fabricante", tipo: "text", placeholder: "Ej: Lian Li" },
      { clave: "Tipo_case", label: "Tipo / Factor", tipo: "radio", opciones: ["Mini-ITX", "Micro-ATX", "Mid Tower", "Full Tower"] },
      { clave: "Altura_case", label: "Altura", tipo: "text", placeholder: "Ej: 482mm" },
      { clave: "Longitud_case", label: "Longitud", tipo: "text", placeholder: "Ej: 464mm" },
      { clave: "Grosor_case", label: "Grosor", tipo: "text", placeholder: "Ej: 238mm" },
      { clave: "Material_case", label: "Material", tipo: "text", placeholder: "Ej: Acero SPCC" },
      { clave: "PlacaCompatible_case", label: "Placas Compatibles", tipo: "radio", opciones: ["E-ATX", "ATX", "micro-ATX", "mini-ITX"] }
    ]
  };

  function normalizarClaveComponente(slugOTexto) {
    if (!slugOTexto) return '';
    const s = toSlug(slugOTexto);
    if (s.includes('placa')) return 'placas-madre';
    if (s.includes('procesador')) return 'procesadores';
    if (s.includes('tarjeta') && (s.includes('grafica') || s.includes('video'))) return 'tarjetas-graficas';
    if (s.includes('ram')) return 'memoria-ram';
    if (s.includes('almacenamiento') || s.includes('disco')) return 'almacenamiento';
    if (s.includes('fuente')) return 'fuente-de-poder';
    if (s.includes('gabinete') || s.includes('case')) return 'gabinete';
    return s;
  }

  function obtenerPrefijoSubcategoria(subcatNombre) {
    if (!subcatNombre) return 'GEN';
    const s = toSlug(subcatNombre);
    if (s.includes('placa')) return 'MB';
    if (s.includes('procesador') || s.includes('cpu')) return 'CPU';
    if (s.includes('tarjeta') || s.includes('grafica') || s.includes('video') || s.includes('gpu')) return 'GPU';
    if (s.includes('almacenamiento') || s.includes('disco')) return 'ALM';
    if (s.includes('ram') || s.includes('memoria')) return 'RAM';
    if (s.includes('fuente') || s.includes('poder') || s.includes('psu')) return 'PSU';
    if (s.includes('gabinete') || s.includes('case')) return 'CAS';

    // Si fue agregada dinámicamente con un prefijo guardado en localStorage o slug corto
    try {
      const prefijosGuardados = JSON.parse(localStorage.getItem('rya_prefijos_categorias') || '{}');
      if (prefijosGuardados[s]) {
        return prefijosGuardados[s].toUpperCase();
      }
    } catch (e) {}

    // Fallback con las primeras 3 letras de la subcategoría
    const limpia = subcatNombre.replace(/[^a-zA-Z0-9]/g, '').toUpperCase();
    return limpia.slice(0, 3) || 'GEN';
  }

  function renderizarCamposEspecificaciones() {
    const selectMacro = document.getElementById('prod-macro-select');
    const selectSub = document.getElementById('prod-subcat-select');
    const wrapper = document.getElementById('prod-specs-wrapper');
    const container = document.getElementById('prod-specs-fields');

    if (!wrapper || !container) return;

    container.innerHTML = '';

    if (!selectMacro || selectMacro.value !== 'Componente') {
      wrapper.style.display = 'none';
      return;
    }

    const subcatId = selectSub?.value;
    if (!subcatId) {
      wrapper.style.display = 'none';
      return;
    }

    const subcatNombre = selectSub.options[selectSub.selectedIndex]?.text || '';
    const claveComponente = normalizarClaveComponente(subcatNombre);
    const listaSpecs = ESPECIFICACIONES_COMPONENTES[claveComponente];

    if (!listaSpecs || listaSpecs.length === 0) {
      wrapper.style.display = 'none';
      return;
    }

    listaSpecs.forEach(spec => {
      const fieldDiv = document.createElement('div');
      fieldDiv.className = 'admin-spec-field';

      const label = document.createElement('label');
      label.textContent = `${spec.label} (${spec.clave}) *`;
      fieldDiv.appendChild(label);

      if (spec.tipo === 'radio') {
        const groupDiv = document.createElement('div');
        groupDiv.className = 'admin-spec-radio-group';
        groupDiv.dataset.specKey = spec.clave;

        spec.opciones.forEach((opt, idx) => {
          const radioLabel = document.createElement('label');
          radioLabel.className = 'admin-spec-radio-option';

          const radioInput = document.createElement('input');
          radioInput.type = 'radio';
          radioInput.name = `spec_radio_${spec.clave}`;
          radioInput.value = opt;
          radioInput.required = true;
          if (idx === 0) radioInput.checked = true;

          radioLabel.appendChild(radioInput);
          radioLabel.appendChild(document.createTextNode(` ${opt}`));
          groupDiv.appendChild(radioLabel);
        });

        fieldDiv.appendChild(groupDiv);
      } else {
        const textInput = document.createElement('input');
        textInput.type = 'text';
        textInput.className = 'admin-spec-text-input';
        textInput.name = `spec_text_${spec.clave}`;
        textInput.dataset.specKey = spec.clave;
        textInput.placeholder = spec.placeholder || '';
        textInput.required = true;

        fieldDiv.appendChild(textInput);
      }

      container.appendChild(fieldDiv);
    });

    wrapper.style.display = 'block';
  }

  function poblarSelectoresSubcategorias() {
    const selectMacro = document.getElementById('prod-macro-select');
    const selectSub = document.getElementById('prod-subcat-select');
    if (!selectMacro || !selectSub) return;

    const macroSeleccionada = selectMacro.value;
    selectSub.innerHTML = '<option value="">Seleccione subcategoría</option>';

    if (!macroSeleccionada) {
      renderizarCamposEspecificaciones();
      return;
    }

    const raiz = (categoriasCache || []).find(c => c.nombre.toLowerCase() === macroSeleccionada.toLowerCase());
    if (raiz && Array.isArray(raiz.subcategorias)) {
      raiz.subcategorias.forEach(sub => {
        const opt = document.createElement('option');
        opt.value = sub.id;
        opt.textContent = sub.nombre;
        selectSub.appendChild(opt);
      });
    }

    renderizarCamposEspecificaciones();
  }

  function mostrarMensaje(elementId, texto, esExito) {
    const el = document.getElementById(elementId);
    if (!el) return;
    el.textContent = texto;
    el.className = `admin-message ${esExito ? 'is-success' : 'is-error'}`;
    el.style.display = 'block';
  }

  function limpiarMensaje(elementId) {
    const el = document.getElementById(elementId);
    if (!el) return;
    el.style.display = 'none';
    el.textContent = '';
  }

  function configurarPestanas() {
    const tabBtns = document.querySelectorAll('.admin-tab-btn');
    const panels = document.querySelectorAll('.admin-panel');

    tabBtns.forEach(btn => {
      btn.addEventListener('click', () => {
        tabBtns.forEach(b => b.classList.remove('is-active'));
        panels.forEach(p => p.classList.remove('is-active'));

        btn.classList.add('is-active');
        const targetId = btn.getAttribute('data-tab');
        const targetPanel = document.getElementById(targetId);
        if (targetPanel) targetPanel.classList.add('is-active');
      });
    });

    const subnavBtns = document.querySelectorAll('.admin-subnav-btn');
    const subviews = document.querySelectorAll('.admin-subview');

    subnavBtns.forEach(btn => {
      btn.addEventListener('click', () => {
        subnavBtns.forEach(b => b.classList.remove('is-active'));
        subviews.forEach(v => v.style.display = 'none');

        btn.classList.add('is-active');
        const targetId = btn.getAttribute('data-sub');
        const targetView = document.getElementById(targetId);
        if (targetView) targetView.style.display = 'block';
      });
    });

    const radiosAgregar = document.querySelectorAll('input[name="tipoAgregarCatalogo"]');
    const formCat = document.getElementById('form-agregar-categoria');
    const formProd = document.getElementById('form-agregar-producto');

    radiosAgregar.forEach(radio => {
      radio.addEventListener('change', () => {
        if (radio.value === 'categoria') {
          if (formCat) formCat.style.display = 'block';
          if (formProd) formProd.style.display = 'none';
        } else {
          if (formCat) formCat.style.display = 'none';
          if (formProd) formProd.style.display = 'block';
        }
      });
    });

    document.getElementById('prod-macro-select')?.addEventListener('change', poblarSelectoresSubcategorias);
    document.getElementById('prod-subcat-select')?.addEventListener('change', renderizarCamposEspecificaciones);
  }

  function toSlug(texto) {
    return (texto || '')
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/(^-|-$)/g, '');
  }

  function configurarModuloCatalogo() {
    // 1. AGREGAR CATEGORÍA
    // Control de visibilidad del prefijo para subcategorías de Componente
    const catPadreSelect = document.getElementById('cat-padre-select');
    const catPrefijoGroup = document.getElementById('cat-prefijo-group');
    const catPrefijoInput = document.getElementById('cat-prefijo-input');

    catPadreSelect?.addEventListener('change', () => {
      if (catPadreSelect.value === 'Componente') {
        if (catPrefijoGroup) catPrefijoGroup.style.display = 'block';
        if (catPrefijoInput) catPrefijoInput.required = true;
      } else {
        if (catPrefijoGroup) catPrefijoGroup.style.display = 'none';
        if (catPrefijoInput) {
          catPrefijoInput.required = false;
          catPrefijoInput.value = '';
        }
      }
    });

    const formCat = document.getElementById('form-agregar-categoria');
    formCat?.addEventListener('submit', async (e) => {
      e.preventDefault();
      limpiarMensaje('msg-agregar-categoria');

      const padreNombre = document.getElementById('cat-padre-select').value;
      const subNombre = document.getElementById('cat-nombre-input').value.trim();
      const prefijo = (document.getElementById('cat-prefijo-input')?.value || '').trim().toUpperCase();

      if (!padreNombre || !subNombre) {
        mostrarMensaje('msg-agregar-categoria', 'Todos los campos son obligatorios', false);
        return;
      }

      if (padreNombre === 'Componente') {
        if (!prefijo) {
          mostrarMensaje('msg-agregar-categoria', 'El prefijo de subcategoría es obligatorio para Componentes.', false);
          return;
        }
        if (prefijo.length > 3) {
          mostrarMensaje('msg-agregar-categoria', 'El prefijo debe tener un máximo de 3 letras.', false);
          return;
        }
      }

      const raiz = (categoriasCache || []).find(c => c.nombre.toLowerCase() === padreNombre.toLowerCase());
      if (!raiz) {
        mostrarMensaje('msg-agregar-categoria', 'Categoría principal no encontrada en base de datos', false);
        return;
      }

      try {
        await window.api.crearCategoria({
          parentId: raiz.id,
          nombre: subNombre,
          slug: toSlug(subNombre)
        });

        if (padreNombre === 'Componente' && prefijo) {
          try {
            const guardados = JSON.parse(localStorage.getItem('rya_prefijos_categorias') || '{}');
            guardados[toSlug(subNombre)] = prefijo;
            localStorage.setItem('rya_prefijos_categorias', JSON.stringify(guardados));
          } catch (e) {}
        }

        mostrarMensaje('msg-agregar-categoria', `¡Subcategoría "${subNombre}" agregada exitosamente!`, true);
        formCat.reset();
        if (catPrefijoGroup) catPrefijoGroup.style.display = 'none';
        await cargarCategorias();
      } catch (err) {
        if (err.message && err.message.includes('Categoria existente')) {
          mostrarMensaje('msg-agregar-categoria', 'Categoria existente', false);
        } else {
          mostrarMensaje('msg-agregar-categoria', err.message || 'Error al agregar categoría', false);
        }
      }
    });

    // 2. AGREGAR EN CATEGORÍA (PRODUCTO)
    const formProd = document.getElementById('form-agregar-producto');
    formProd?.addEventListener('submit', async (e) => {
      e.preventDefault();
      limpiarMensaje('msg-agregar-producto');

      const macro = document.getElementById('prod-macro-select').value;
      const subcatId = document.getElementById('prod-subcat-select').value;
      const nombre = document.getElementById('prod-nombre-input').value.trim();
      const precio = parseFloat(document.getElementById('prod-precio-input').value);
      const stock = parseInt(document.getElementById('prod-stock-input').value, 10);
      const url = document.getElementById('prod-url-input').value.trim();
      const descripcion = document.getElementById('prod-desc-input').value.trim();

      if (!macro || !subcatId || !nombre || isNaN(precio) || isNaN(stock) || stock < 0 || !url || !descripcion) {
        mostrarMensaje('msg-agregar-producto', 'Todos los campos son obligatorios', false);
        return;
      }

      const selectSub = document.getElementById('prod-subcat-select');
      const subcatNombre = selectSub?.options[selectSub.selectedIndex]?.text || '';

      const especificaciones = [];
      if (macro === 'Componente') {
        const claveComponente = normalizarClaveComponente(subcatNombre);
        const listaSpecs = ESPECIFICACIONES_COMPONENTES[claveComponente];

        if (listaSpecs && listaSpecs.length > 0) {
          for (const spec of listaSpecs) {
            let valor = '';
            if (spec.tipo === 'radio') {
              const checkedRadio = formProd.querySelector(`input[name="spec_radio_${spec.clave}"]:checked`);
              if (checkedRadio) valor = checkedRadio.value.trim();
            } else {
              const textInput = formProd.querySelector(`input[name="spec_text_${spec.clave}"]`);
              if (textInput) valor = textInput.value.trim();
            }

            if (!valor) {
              mostrarMensaje('msg-agregar-producto', `El parámetro "${spec.label} (${spec.clave})" es obligatorio para este componente.`, false);
              return;
            }

            especificaciones.push({
              clave: spec.clave,
              valor: valor
            });
          }
        }
      }

      const tipoProducto = macro === 'Periféricos' ? 'PERIFERICO' : 'HARDWARE';
      const prefijoSubcat = obtenerPrefijoSubcategoria(subcatNombre);
      const timestamp6 = Date.now().toString().slice(-6);
      const skuFormateado = `RYA-${prefijoSubcat}-${timestamp6}`;

      try {
        await window.api.crearProducto({
          subcategoriaId: parseInt(subcatId, 10),
          sku: skuFormateado,
          nombre: nombre,
          descripcion: descripcion,
          precioBase: precio,
          imagenUrl: url,
          tipoProducto: tipoProducto,
          stock: stock,
          activo: true,
          especificaciones: especificaciones
        });

        mostrarMensaje('msg-agregar-producto', `¡Producto "${nombre}" creado con éxito con sus especificaciones técnicas!`, true);
        formProd.reset();
        poblarSelectoresSubcategorias();
      } catch (err) {
        mostrarMensaje('msg-agregar-producto', err.message || 'Error al guardar el producto', false);
      }
    });

    // 3. EDITAR PRODUCTO
    const formBuscarEditar = document.getElementById('form-buscar-editar');
    const formGuardarEditar = document.getElementById('form-guardar-editar');
    formBuscarEditar?.addEventListener('submit', async (e) => {
      e.preventDefault();
      limpiarMensaje('msg-buscar-editar');
      limpiarMensaje('msg-guardar-editar');
      if (formGuardarEditar) formGuardarEditar.style.display = 'none';

      const nombreBusqueda = document.getElementById('edit-search-nombre').value.trim();
      if (!nombreBusqueda) {
        mostrarMensaje('msg-buscar-editar', 'Todos los parámetros son obligatorios.', false);
        return;
      }

      try {
        const prod = await window.api.buscarProductoPorNombre(nombreBusqueda);
        if (!prod) {
          mostrarMensaje('msg-buscar-editar', 'Producto no encontrado', false);
          return;
        }

        productoEncontradoEditar = prod;
        document.getElementById('edit-nombre-input').value = prod.nombre;
        document.getElementById('edit-precio-input').value = prod.precio;
        document.getElementById('edit-stock-input').value = prod.stock !== undefined ? prod.stock : 0;
        document.getElementById('edit-url-input').value = prod.imagenUrl || '';
        document.getElementById('edit-desc-input').value = prod.descripcion || '';

        if (formGuardarEditar) formGuardarEditar.style.display = 'block';
      } catch (err) {
        mostrarMensaje('msg-buscar-editar', 'Producto no encontrado', false);
      }
    });

    formGuardarEditar?.addEventListener('submit', async (e) => {
      e.preventDefault();
      limpiarMensaje('msg-guardar-editar');

      if (!productoEncontradoEditar) return;

      const nuevoNombre = document.getElementById('edit-nombre-input').value.trim();
      const nuevoPrecio = parseFloat(document.getElementById('edit-precio-input').value);
      const nuevoStock = parseInt(document.getElementById('edit-stock-input').value, 10);
      const nuevaUrl = document.getElementById('edit-url-input').value.trim();
      const nuevaDesc = document.getElementById('edit-desc-input').value.trim();

      if (!nuevoNombre || isNaN(nuevoPrecio) || isNaN(nuevoStock) || nuevoStock < 0 || !nuevaUrl || !nuevaDesc) {
        mostrarMensaje('msg-guardar-editar', 'Todos los parámetros son obligatorios.', false);
        return;
      }

      try {
        await window.api.actualizarProducto(productoEncontradoEditar.id, {
          nombre: nuevoNombre,
          precioBase: nuevoPrecio,
          stock: nuevoStock,
          imagenUrl: nuevaUrl,
          descripcion: nuevaDesc
        });

        mostrarMensaje('msg-guardar-editar', '¡Producto actualizado correctamente!', true);
      } catch (err) {
        mostrarMensaje('msg-guardar-editar', err.message || 'Error al actualizar producto', false);
      }
    });

    // 4. ELIMINAR PRODUCTO
    const formBuscarEliminar = document.getElementById('form-buscar-eliminar');
    const previewContainer = document.getElementById('del-preview-container');
    const cardWrapper = document.getElementById('del-card-wrapper');
    const btnIniciarEliminar = document.getElementById('btn-iniciar-eliminar');

    formBuscarEliminar?.addEventListener('submit', async (e) => {
      e.preventDefault();
      limpiarMensaje('msg-buscar-eliminar');
      if (previewContainer) previewContainer.style.display = 'none';
      if (cardWrapper) cardWrapper.innerHTML = '';
      productoEncontradoEliminar = null;

      const nombreBusqueda = document.getElementById('del-search-nombre').value.trim();
      if (!nombreBusqueda) {
        mostrarMensaje('msg-buscar-eliminar', 'Todos los parámetros son obligatorios.', false);
        return;
      }

      try {
        const prod = await window.api.buscarProductoPorNombre(nombreBusqueda);
        if (!prod) {
          mostrarMensaje('msg-buscar-eliminar', 'Producto no encontrado', false);
          return;
        }

        productoEncontradoEliminar = prod;
        if (window.crearTarjetaProducto && cardWrapper) {
          const cardEl = window.crearTarjetaProducto(prod, true);
          cardWrapper.appendChild(cardEl);
        }
        if (previewContainer) previewContainer.style.display = 'flex';
      } catch (err) {
        mostrarMensaje('msg-buscar-eliminar', 'Producto no encontrado', false);
      }
    });

    const modalDelete = document.getElementById('modal-confirm-delete');
    const modalDeleteName = document.getElementById('modal-delete-product-name');
    const btnYes = document.getElementById('btn-confirm-delete-yes');
    const btnNo = document.getElementById('btn-confirm-delete-no');

    btnIniciarEliminar?.addEventListener('click', () => {
      if (!productoEncontradoEliminar) return;
      if (modalDeleteName) modalDeleteName.textContent = productoEncontradoEliminar.nombre;
      if (modalDelete) modalDelete.classList.add('is-open');
    });

    btnNo?.addEventListener('click', () => {
      if (modalDelete) modalDelete.classList.remove('is-open');
    });

    btnYes?.addEventListener('click', async () => {
      if (!productoEncontradoEliminar) return;
      try {
        await window.api.eliminarProductoPorNombre(productoEncontradoEliminar.nombre);
        if (modalDelete) modalDelete.classList.remove('is-open');
        if (previewContainer) previewContainer.style.display = 'none';
        formBuscarEliminar.reset();
        mostrarMensaje('msg-buscar-eliminar', `¡Producto "${productoEncontradoEliminar.nombre}" eliminado exitosamente!`, true);
        productoEncontradoEliminar = null;
      } catch (err) {
        if (modalDelete) modalDelete.classList.remove('is-open');
        mostrarMensaje('msg-buscar-eliminar', err.message || 'Error al eliminar producto', false);
      }
    });
  }

  function configurarModuloAsesorias() {
    const formAsesoria = document.getElementById('form-asesoria-especialidad');
    formAsesoria?.addEventListener('submit', async (e) => {
      e.preventDefault();
      limpiarMensaje('msg-asesoria-especialidad');

      const nombre = document.getElementById('esp-nombre').value.trim();
      const campo = document.getElementById('esp-campo').value.trim();
      const desc = document.getElementById('esp-desc').value.trim();

      const prioCpu = parseInt(document.getElementById('prio-cpu').value, 10);
      const prioGpu = parseInt(document.getElementById('prio-gpu').value, 10);
      const prioDisco = parseInt(document.getElementById('prio-disco').value, 10);
      const prioRam = parseInt(document.getElementById('prio-ram').value, 10);
      const prioFuente = parseInt(document.getElementById('prio-fuente').value, 10);
      const prioPlaca = parseInt(document.getElementById('prio-placa').value, 10);
      const prioCase = parseInt(document.getElementById('prio-case').value, 10);

      const prios = [prioCpu, prioGpu, prioDisco, prioRam, prioFuente, prioPlaca, prioCase];

      if (!nombre || !campo || !desc || prios.some(p => isNaN(p) || p < 1 || p > 7)) {
        mostrarMensaje('msg-asesoria-especialidad', 'Todos los parámetros son obligatorios y deben estar entre 1 y 7.', false);
        return;
      }

      const prioSet = new Set(prios);
      if (prioSet.size !== 7) {
        mostrarMensaje('msg-asesoria-especialidad', 'Los niveles de prioridad (1 al 7) no se pueden repetir.', false);
        return;
      }

      try {
        await window.api.crearEspecialidadPerfil({
          nombre: nombre,
          campo: campo,
          prioCpu: prioCpu,
          prioGpu: prioGpu,
          prioDisco: prioDisco,
          prioRam: prioRam,
          prioFuente: prioFuente,
          prioPlaca: prioPlaca,
          prioCase: prioCase,
          descripcion: desc
        });

        mostrarMensaje('msg-asesoria-especialidad', `¡Especialidad y perfil para "${nombre}" registrados exitosamente!`, true);
        formAsesoria.reset();
      } catch (err) {
        mostrarMensaje('msg-asesoria-especialidad', err.message || 'Error al registrar especialidad', false);
      }
    });
  }

  function initAdmin() {
    configurarPestanas();
    configurarModuloCatalogo();
    configurarModuloAsesorias();
    cargarCategorias();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initAdmin);
  } else {
    initAdmin();
  }
})();
