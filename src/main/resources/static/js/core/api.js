

(function (global) {
  'use strict';

  function resolveBaseUrl() {
    if (typeof window !== 'undefined') {
      if (window.RYA_API_URL) return window.RYA_API_URL;
      const stored = localStorage.getItem('rya_api_base_url');
      if (stored) return stored;

      if (window.location.port === '8080' || window.location.hostname === 'localhost' && window.location.port === '') {
        return '/api';
      }

      return 'http://localhost:8080/api';
    }
    return 'http://localhost:8080/api';
  }

  const API_BASE_URL = resolveBaseUrl();

  function normalizarProductoBackend(p) {
    if (!p) return null;

    const atributos = {};
    let fabricante = 'RYA';

    if (p.especificaciones && Array.isArray(p.especificaciones)) {
      p.especificaciones.forEach(e => {
        if (!e || !e.clave) return;
        atributos[e.clave] = e.valor;
        if (e.clave.toLowerCase() === 'fabricante' || e.clave.toLowerCase() === 'marca') {
          fabricante = e.valor;
        }
      });
    }

    if (fabricante === 'RYA' && p.nombre) {
      const primerToken = p.nombre.split(' ')[0];
      const marcasComunes = [
        'ASUS', 'MSI', 'Gigabyte', 'Intel', 'AMD', 'Corsair', 'EVGA', 'Kingston',
        'Logitech', 'Razer', 'Samsung', 'LG', 'Crucial', 'NZXT', 'Lian Li', 'Noctua',
        'Thermaltake', 'HyperX', 'SteelSeries', 'Creative', 'JBL', 'Western Digital',
        'Seagate', 'TP-Link', 'G.Skill', 'Sapphire', 'Microsoft'
      ];
      if (marcasComunes.includes(primerToken)) {
        fabricante = primerToken;
      }
    }

    return {
      id: p.id,
      sku: p.sku || '',
      nombre: p.nombre || '',
      descripcion: p.descripcion || '',
      categoria: p.subcategoriaNombre || (p.categoria && p.categoria.nombre) || 'Componente',
      subcategoriaId: p.subcategoriaId || (p.categoria && p.categoria.id) || null,
      fabricante: fabricante,
      precio: Number(p.precioBase !== undefined ? p.precioBase : (p.precio || 0)),
      stock: p.stock !== undefined ? p.stock : 25,
      activo: p.activo !== false,
      imagenUrl: p.imagenUrl || p.imagen_url || '',
      atributos: atributos,
      especificaciones: p.especificaciones || [],
      tipoProducto: p.tipoProducto || 'HARDWARE'
    };
  }

  async function request(endpoint, options = {}) {
    const url = `${API_BASE_URL}${endpoint}`;
    const defaultHeaders = {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };

    const config = {
      ...options,
      headers: {
        ...defaultHeaders,
        ...(options.headers || {})
      }
    };

    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), options.timeout || 8000);
    config.signal = controller.signal;

    try {
      const response = await fetch(url, config);
      clearTimeout(timeoutId);

      if (!response.ok) {
        let errorData = null;
        try {
          errorData = await response.json();
        } catch (_) {}

        const error = new Error(errorData?.mensaje || `Error HTTP ${response.status}: ${response.statusText}`);
        error.status = response.status;
        error.data = errorData;
        throw error;
      }

      if (response.status === 204) {
        return null;
      }

      return await response.json();
    } catch (err) {
      clearTimeout(timeoutId);
      throw err;
    }
  }

  async function obtenerProductos(subcategoriaId = null) {
    try {
      const endpoint = subcategoriaId ? `/productos?categoriaId=${subcategoriaId}` : '/productos';
      const data = await request(endpoint);
      if (Array.isArray(data)) {
        return data.map(normalizarProductoBackend);
      }
    } catch (error) {
      console.warn('Backend no disponible o error al conectar con /api/productos:', error.message);
    }

    return [];
  }

  async function obtenerProductoPorId(id) {
    if (!id) return null;

    const isUuid = typeof id === 'string' && /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(id);
    if (isUuid) {
      try {
        const prodBackend = await request(`/productos/${id}`);
        if (prodBackend) return normalizarProductoBackend(prodBackend);
      } catch (err) {
        console.warn(`No se pudo obtener producto ${id} por endpoint directo:`, err.message);
      }
    }

    const productos = await obtenerProductos();
    return productos.find(p => String(p.id) === String(id)) || null;
  }

  async function buscarProductoPorNombre(nombre) {
    if (!nombre) throw new Error('El nombre es requerido');
    const data = await request(`/productos/buscar?nombre=${encodeURIComponent(nombre.trim())}`);
    return normalizarProductoBackend(data);
  }

  async function crearProducto(payload) {
    const data = await request('/productos', {
      method: 'POST',
      body: JSON.stringify(payload)
    });
    return normalizarProductoBackend(data);
  }

  async function actualizarProducto(id, payload) {
    const data = await request(`/productos/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    });
    return normalizarProductoBackend(data);
  }

  async function eliminarProductoPorNombre(nombre) {
    return await request(`/productos/nombre?nombre=${encodeURIComponent(nombre.trim())}`, {
      method: 'DELETE'
    });
  }

  async function crearCategoria(payload) {
    return await request('/categorias', {
      method: 'POST',
      body: JSON.stringify(payload)
    });
  }

  async function crearEspecialidadPerfil(payload) {
    return await request('/asesorias', {
      method: 'POST',
      body: JSON.stringify(payload)
    });
  }

  async function obtenerProductosPorCategoria(categoriaNombre, limite = 6) {
    const todos = await obtenerProductos();
    const filtrados = todos.filter(p => p.categoria === categoriaNombre);
    return limite ? filtrados.slice(0, limite) : filtrados;
  }

  async function obtenerCategorias(soloRaices = false) {
    try {
      const endpoint = soloRaices ? '/categorias?soloRaices=true' : '/categorias';
      return await request(endpoint);
    } catch (err) {
      console.warn('Error al obtener categorías del backend:', err.message);
      return null;
    }
  }

  async function obtenerUsuarioPorEmail(email) {
    try {
      return await request(`/usuarios/email?email=${encodeURIComponent(email)}`);
    } catch (err) {
      if (err.status === 404) return null;
      throw err;
    }
  }

  async function iniciarSesion(credenciales) {
    return await request('/usuarios/login', {
      method: 'POST',
      body: JSON.stringify(credenciales)
    });
  }

  async function crearUsuario(usuarioDto) {
    return await request('/usuarios', {
      method: 'POST',
      body: JSON.stringify({
        nombre: usuarioDto.nombre,
        email: usuarioDto.email,
        telefono: usuarioDto.telefono || '',
        password: usuarioDto.password || 'Password123!',
        rol: usuarioDto.rol || 'CLIENT'
      })
    });
  }

  async function registrarOObtenerUsuario(cliente) {
    if (!cliente || !cliente.email) {
      throw new Error('El correo del cliente es requerido.');
    }

    try {
      const usuarioExistente = await obtenerUsuarioPorEmail(cliente.email);
      if (usuarioExistente && usuarioExistente.id) {
        return usuarioExistente;
      }
    } catch (err) {
      console.warn('Error buscando usuario existente:', err.message);
    }

    try {
      return await crearUsuario({
        nombre: cliente.nombre,
        email: cliente.email,
        telefono: cliente.telefono,
        password: 'Password123!',
        rol: 'CLIENT'
      });
    } catch (createErr) {
      console.warn('No se pudo registrar usuario en el backend:', createErr.message);
      return null;
    }
  }

  async function crearPedido(pedidoPayload) {
    return await request('/pedidos', {
      method: 'POST',
      body: JSON.stringify(pedidoPayload)
    });
  }

  async function obtenerPedidoPorId(id) {
    return await request(`/pedidos/${id}`);
  }

  async function verificarConexionBackend() {
    try {
      const response = await fetch(`${API_BASE_URL}/productos`, {
        method: 'GET',
        headers: { 'Accept': 'application/json' },
        signal: AbortSignal.timeout(3000)
      });
      return {
        online: response.ok,
        status: response.status,
        url: API_BASE_URL
      };
    } catch (err) {
      return {
        online: false,
        status: 0,
        url: API_BASE_URL,
        error: err.message
      };
    }
  }

  const RyaApi = {
    BASE_URL: API_BASE_URL,
    obtenerProductos,
    obtenerProductoPorId,
    buscarProductoPorNombre,
    crearProducto,
    actualizarProducto,
    eliminarProductoPorNombre,
    crearCategoria,
    crearEspecialidadPerfil,
    obtenerProductosPorCategoria,
    obtenerCategorias,
    obtenerUsuarioPorEmail,
    iniciarSesion,
    crearUsuario,
    registrarOObtenerUsuario,
    crearPedido,
    obtenerPedidoPorId,
    verificarConexionBackend
  };

  global.RyaApi = RyaApi;
  global.api = RyaApi;
  global.obtenerProductosPorCategoria = obtenerProductosPorCategoria;
  global.obtenerProductos = obtenerProductos;
  global.obtenerProductoPorId = obtenerProductoPorId;
  global.crearPedidoApi = crearPedido;
  global.registrarOObtenerUsuarioApi = registrarOObtenerUsuario;
  global.verificarConexionBackend = verificarConexionBackend;

})(typeof window !== 'undefined' ? window : this);
