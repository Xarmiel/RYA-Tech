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

  function poblarSelectoresSubcategorias() {
    const selectMacro = document.getElementById('prod-macro-select');
    const selectSub = document.getElementById('prod-subcat-select');
    if (!selectMacro || !selectSub) return;

    const macroSeleccionada = selectMacro.value;
    selectSub.innerHTML = '<option value="">Seleccione subcategoría</option>';

    if (!macroSeleccionada) return;

    const raiz = (categoriasCache || []).find(c => c.nombre.toLowerCase() === macroSeleccionada.toLowerCase());
    if (raiz && Array.isArray(raiz.subcategorias)) {
      raiz.subcategorias.forEach(sub => {
        const opt = document.createElement('option');
        opt.value = sub.id;
        opt.textContent = sub.nombre;
        selectSub.appendChild(opt);
      });
    }
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
    const formCat = document.getElementById('form-agregar-categoria');
    formCat?.addEventListener('submit', async (e) => {
      e.preventDefault();
      limpiarMensaje('msg-agregar-categoria');

      const padreNombre = document.getElementById('cat-padre-select').value;
      const subNombre = document.getElementById('cat-nombre-input').value.trim();

      if (!padreNombre || !subNombre) {
        mostrarMensaje('msg-agregar-categoria', 'Todos los campos son obligatorios', false);
        return;
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

        mostrarMensaje('msg-agregar-categoria', `¡Subcategoría "${subNombre}" agregada exitosamente!`, true);
        formCat.reset();
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
      const url = document.getElementById('prod-url-input').value.trim();

      if (!macro || !subcatId || !nombre || isNaN(precio) || !url) {
        mostrarMensaje('msg-agregar-producto', 'Todos los campos son obligatorios', false);
        return;
      }

      const tipoProducto = macro === 'Periféricos' ? 'PERIFERICO' : 'HARDWARE';
      const skuRandom = 'RYA-ADMIN-' + Date.now().toString().slice(-6);

      try {
        await window.api.crearProducto({
          subcategoriaId: parseInt(subcatId, 10),
          sku: skuRandom,
          nombre: nombre,
          descripcion: `Producto agregado desde panel de administración (${macro})`,
          precioBase: precio,
          imagenUrl: url,
          tipoProducto: tipoProducto,
          stock: 50,
          activo: true,
          especificaciones: []
        });

        mostrarMensaje('msg-agregar-producto', `¡Producto "${nombre}" creado con éxito!`, true);
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
        document.getElementById('edit-url-input').value = prod.imagenUrl || '';

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
      const nuevaUrl = document.getElementById('edit-url-input').value.trim();

      if (!nuevoNombre || isNaN(nuevoPrecio) || !nuevaUrl) {
        mostrarMensaje('msg-guardar-editar', 'Todos los parámetros son obligatorios.', false);
        return;
      }

      try {
        await window.api.actualizarProducto(productoEncontradoEditar.id, {
          nombre: nuevoNombre,
          precioBase: nuevoPrecio,
          imagenUrl: nuevaUrl
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
