const STORAGE_KEY = 'rya_cart_v1';
const ORDERS_STORAGE_KEY = 'rya_orders_v1';

function formatCurrency(amount) {
  return `S/ ${Number(amount || 0).toLocaleString('es-PE', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })}`;
}

function getProductThumbnail(item) {
  if (item.imagenUrl) return item.imagenUrl;
  if (item.imagen) return item.imagen;
  const marca = item.fabricante || 'RYA';
  const cat = item.categoria || 'Hardware';
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="120" height="120" viewBox="0 0 120 120">
    <rect width="120" height="120" fill="#030914"/>
    <rect x="6" y="6" width="108" height="108" rx="8" fill="#071326" stroke="#00D4FF" stroke-width="1.5" opacity="0.8"/>
    <circle cx="60" cy="46" r="22" fill="#00D4FF" opacity="0.15"/>
    <path d="M48 46h24M60 34v24" stroke="#00D4FF" stroke-width="2.5" stroke-linecap="round"/>
    <text x="60" y="82" fill="#F5F7FA" font-family="sans-serif" font-size="10" font-weight="bold" text-anchor="middle">${marca}</text>
    <text x="60" y="96" fill="#C2C5CC" font-family="sans-serif" font-size="8" text-anchor="middle">${cat.substring(0, 14)}</text>
  </svg>`;
  return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`;
}

function generateUUID() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}

class CartManager {
  constructor() {
    this.drawerEl = null;
    this.overlayEl = null;
    this.toastContainerEl = null;
    this.checkoutModalEl = null;
    this.isInitialized = false;

    this.checkoutStep = 1;
    this.checkoutData = {
      cliente: {
        tipoDoc: 'DNI',
        numDoc: '',
        nombre: '',
        email: '',
        telefono: ''
      },
      entrega: {
        metodo: 'domicilio',
        departamento: 'Ica',
        ciudad: 'Ica',
        direccion: '',
        referencia: '',
        titularRetiroNombre: '',
        titularRetiroDni: ''
      },
      pago: {
        metodo: 'yape',
        numeroOp: '',
        titularTarjeta: '',
        numTarjeta: '',
        expiracion: '',
        cvv: ''
      }
    };
    this.orderResult = null;
  }

  init() {
    if (this.isInitialized) return;
    this.ensureDOMStructure();
    this.bindGlobalEvents();
    this.updateCartBadge();
    this.isInitialized = true;
  }

  getCart() {
    try {
      const data = localStorage.getItem(STORAGE_KEY);
      return data ? JSON.parse(data) : [];
    } catch (err) {
      console.error('Error al leer el carrito desde localStorage:', err);
      return [];
    }
  }

  saveCart(cart) {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(cart));
      this.updateCartBadge();
      this.renderDrawer();
      window.dispatchEvent(new CustomEvent('cart:updated', { detail: { cart } }));
    } catch (err) {
      console.error('Error al guardar el carrito:', err);
    }
  }

  getTotals() {
    const cart = this.getCart();
    const count = cart.reduce((sum, item) => sum + (Number(item.cantidad) || 0), 0);
    const total = cart.reduce((sum, item) => sum + (Number(item.precio) * Number(item.cantidad) || 0), 0);
    
    const subtotal = total / 1.18;
    const igv = total - subtotal;

    return {
      count,
      total,
      subtotal,
      igv,
      formattedTotal: formatCurrency(total),
      formattedSubtotal: formatCurrency(subtotal),
      formattedIgv: formatCurrency(igv)
    };
  }

  addItem(product, quantity = 1, openDrawerImmediately = true) {
    if (!product || !product.id) return;
    const qty = Math.max(1, Number(quantity) || 1);
    const cart = this.getCart();
    const existingIndex = cart.findIndex(item => String(item.id) === String(product.id));

    if (existingIndex > -1) {
      cart[existingIndex].cantidad = Number(cart[existingIndex].cantidad) + qty;
    } else {
      const primerAtributo = product.atributos ? Object.values(product.atributos)[0] : '';
      cart.push({
        id: product.id,
        fabricante: product.fabricante || 'RYA',
        categoria: product.categoria || 'Componente',
        precio: Number(product.precio) || 0,
        detalle: primerAtributo || '',
        imagen: product.imagen || null,
        cantidad: qty
      });
    }

    this.saveCart(cart);
    this.animateCartButton();

    this.showToast({
      title: '¡Producto añadido!',
      message: `${product.fabricante} ${product.categoria} (${qty} und.)`,
      actionText: 'Ver Carrito',
      onAction: () => this.openDrawer()
    });

    if (openDrawerImmediately) {
      this.openDrawer();
    }
  }

  async addItemById(productId, quantity = 1, openDrawerImmediately = true) {
    try {
      if (typeof window !== 'undefined' && window.api && window.api.obtenerProductoPorId) {
        const prod = await window.api.obtenerProductoPorId(productId);
        if (prod) {
          this.addItem(prod, quantity, openDrawerImmediately);
          return;
        }
      }

      const fetchFn = (typeof window !== 'undefined' && window.fetchProductos)
        ? window.fetchProductos
        : (typeof fetchProductos !== 'undefined' ? fetchProductos : null);
      const productos = fetchFn ? await fetchFn() : [];
      const product = productos.find(p => String(p.id) === String(productId));
      if (product) {
        this.addItem(product, quantity, openDrawerImmediately);
      } else {
        console.warn('Producto no encontrado con ID:', productId);
      }
    } catch (error) {
      console.error('Error al agregar producto por ID:', error);
    }
  }

  updateQuantity(productId, newQty) {
    const qty = Number(newQty);
    let cart = this.getCart();

    if (qty <= 0) {
      this.removeItem(productId);
      return;
    }

    cart = cart.map(item => {
      if (String(item.id) === String(productId)) {
        return { ...item, cantidad: Math.min(99, qty) };
      }
      return item;
    });

    this.saveCart(cart);
  }

  removeItem(productId) {
    const cart = this.getCart();
    const itemToRemove = cart.find(item => String(item.id) === String(productId));
    const filteredCart = cart.filter(item => String(item.id) !== String(productId));

    this.saveCart(filteredCart);

    if (itemToRemove) {
      this.showToast({
        title: 'Producto eliminado',
        message: `${itemToRemove.fabricante} ${itemToRemove.categoria} fue removido.`
      });
    }
  }

  clearCart() {
    this.saveCart([]);
    this.showToast({
      title: 'Carrito vaciado',
      message: 'Todos los productos han sido removidos.'
    });
  }

  ensureDOMStructure() {
    if (!document.getElementById('cart-drawer-overlay')) {
      this.overlayEl = document.createElement('div');
      this.overlayEl.id = 'cart-drawer-overlay';
      this.overlayEl.className = 'cart-drawer-overlay';
      document.body.appendChild(this.overlayEl);
    } else {
      this.overlayEl = document.getElementById('cart-drawer-overlay');
    }

    if (!document.getElementById('cart-drawer')) {
      this.drawerEl = document.createElement('aside');
      this.drawerEl.id = 'cart-drawer';
      this.drawerEl.className = 'cart-drawer';
      this.drawerEl.setAttribute('role', 'dialog');
      this.drawerEl.setAttribute('aria-modal', 'true');
      this.drawerEl.setAttribute('aria-label', 'Carrito de compras');
      this.drawerEl.innerHTML = `
        <div class="cart-drawer-header">
          <div class="cart-header-title">
            <span>Tu Carrito</span>
            <span class="cart-header-badge" id="cart-drawer-badge">0 items</span>
          </div>
          <button class="cart-drawer-close" id="btn-close-cart" aria-label="Cerrar carrito">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </button>
        </div>
        <div class="cart-drawer-body" id="cart-drawer-body"></div>
        <div class="cart-drawer-footer" id="cart-drawer-footer"></div>
      `;
      document.body.appendChild(this.drawerEl);
    } else {
      this.drawerEl = document.getElementById('cart-drawer');
    }

    if (!document.getElementById('toast-container')) {
      this.toastContainerEl = document.createElement('div');
      this.toastContainerEl.id = 'toast-container';
      this.toastContainerEl.className = 'toast-container';
      this.toastContainerEl.setAttribute('aria-live', 'polite');
      document.body.appendChild(this.toastContainerEl);
    } else {
      this.toastContainerEl = document.getElementById('toast-container');
    }

    if (!document.getElementById('cart-checkout-modal')) {
      this.checkoutModalEl = document.createElement('div');
      this.checkoutModalEl.id = 'cart-checkout-modal';
      this.checkoutModalEl.className = 'cart-checkout-modal-overlay';
      this.checkoutModalEl.innerHTML = `
        <div class="cart-checkout-modal" role="dialog" aria-modal="true">
          <div class="checkout-modal-header">
            <div class="checkout-header-top">
              <h3 class="checkout-modal-title">
                <span>Finalizar Pedido</span>
              </h3>
              <button class="cart-drawer-close" id="btn-close-checkout" aria-label="Cerrar ventana">✕</button>
            </div>
            <div class="checkout-stepper" id="checkout-stepper">
              <!-- Renderizado dinámico del Stepper -->
            </div>
          </div>
          <div class="checkout-modal-body" id="checkout-modal-body">
            <!-- Contenido dinámico del paso actual -->
          </div>
          <div class="checkout-modal-footer" id="checkout-modal-footer">
            <!-- Botones y totales del paso actual -->
          </div>
        </div>
      `;
      document.body.appendChild(this.checkoutModalEl);
    } else {
      this.checkoutModalEl = document.getElementById('cart-checkout-modal');
    }
  }

  renderDrawer() {
    const bodyEl = document.getElementById('cart-drawer-body');
    const footerEl = document.getElementById('cart-drawer-footer');
    const badgeEl = document.getElementById('cart-drawer-badge');
    if (!bodyEl || !footerEl) return;

    const cart = this.getCart();
    const totals = this.getTotals();

    if (badgeEl) {
      badgeEl.textContent = `${totals.count} ${totals.count === 1 ? 'item' : 'items'}`;
    }

    if (cart.length === 0) {
      bodyEl.innerHTML = `
        <div class="cart-empty-state">
          <div class="cart-empty-icon">
            <svg width="38" height="38" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="9" cy="21" r="1"></circle>
              <circle cx="20" cy="21" r="1"></circle>
              <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"></path>
            </svg>
          </div>
          <h4 class="cart-empty-title">Tu carrito está vacío</h4>
          <p class="cart-empty-desc">Explora nuestros componentes, procesadores y periféricos de alto rendimiento.</p>
          <a href="index.html" class="cart-empty-btn" id="btn-explore-from-cart">
            Explorar catálogo →
          </a>
        </div>
      `;
      footerEl.innerHTML = '';
      footerEl.style.display = 'none';
      return;
    }

    footerEl.style.display = 'flex';

    bodyEl.innerHTML = cart.map(item => {
      const itemSubtotal = formatCurrency(Number(item.precio) * Number(item.cantidad));
      const thumbnailSrc = getProductThumbnail(item);

      return `
        <article class="cart-item" data-id="${item.id}">
          <div class="cart-item-img">
            <img src="${thumbnailSrc}" alt="${item.fabricante} ${item.categoria}">
          </div>
          <div class="cart-item-info">
            <div class="cart-item-top">
              <span class="cart-item-brand">${item.fabricante}</span>
              <h5 class="cart-item-name">${item.categoria}</h5>
              ${item.detalle ? `<span class="cart-item-attr">${item.detalle}</span>` : ''}
              <span class="cart-item-unit-price">${formatCurrency(item.precio)} c/u</span>
            </div>
            <div class="cart-item-bottom">
              <div class="cart-item-qty">
                <button class="cart-qty-btn btn-qty-dec" data-id="${item.id}" aria-label="Disminuir cantidad">−</button>
                <span class="cart-qty-val">${item.cantidad}</span>
                <button class="cart-qty-btn btn-qty-inc" data-id="${item.id}" aria-label="Aumentar cantidad">+</button>
              </div>
              <span class="cart-item-subtotal">${itemSubtotal}</span>
            </div>
          </div>
          <button class="cart-item-remove btn-remove-item" data-id="${item.id}" aria-label="Eliminar producto">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="3 6 5 6 21 6"></polyline>
              <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
              <line x1="10" y1="11" x2="10" y2="17"></line>
              <line x1="14" y1="11" x2="14" y2="17"></line>
            </svg>
          </button>
        </article>
      `;
    }).join('');

    footerEl.innerHTML = `
      <div class="cart-summary-box">
        <div class="cart-summary-row">
          <span>Subtotal (Base imponible)</span>
          <span>${totals.formattedSubtotal}</span>
        </div>
        <div class="cart-summary-row">
          <span>IGV (18% incluido)</span>
          <span>${totals.formattedIgv}</span>
        </div>
        <div class="cart-summary-row total-row">
          <span>Total a pagar</span>
          <span>${totals.formattedTotal}</span>
        </div>
      </div>
      <div class="cart-actions-group">
        <button class="cart-btn-checkout" id="btn-proceed-checkout">
          Proceder al Pago
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="9 18 15 12 9 6"></polyline>
          </svg>
        </button>
        <button class="cart-btn-clear" id="btn-empty-cart">Vaciar todo el carrito</button>
      </div>
    `;

    this.bindDrawerItemEvents();
  }

  bindDrawerItemEvents() {
    const bodyEl = document.getElementById('cart-drawer-body');
    const footerEl = document.getElementById('cart-drawer-footer');

    bodyEl?.querySelectorAll('.btn-qty-inc').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        const id = btn.dataset.id;
        const item = this.getCart().find(i => String(i.id) === String(id));
        if (item) this.updateQuantity(id, item.cantidad + 1);
      });
    });

    bodyEl?.querySelectorAll('.btn-qty-dec').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        const id = btn.dataset.id;
        const item = this.getCart().find(i => String(i.id) === String(id));
        if (item) this.updateQuantity(id, item.cantidad - 1);
      });
    });

    bodyEl?.querySelectorAll('.btn-remove-item').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        const id = btn.dataset.id;
        this.removeItem(id);
      });
    });

    footerEl?.querySelector('#btn-empty-cart')?.addEventListener('click', () => {
      if (confirm('¿Estás seguro de vaciar todos los productos del carrito?')) {
        this.clearCart();
      }
    });

    footerEl?.querySelector('#btn-proceed-checkout')?.addEventListener('click', () => {
      this.closeDrawer();
      window.location.href = 'checkout.html';
    });

    bodyEl?.querySelector('#btn-explore-from-cart')?.addEventListener('click', () => {
      this.closeDrawer();
    });
  }

  openDrawer() {
    this.ensureDOMStructure();
    this.renderDrawer();
    this.overlayEl?.classList.add('is-active');
    this.drawerEl?.classList.add('is-active');
    document.body.style.overflow = 'hidden';
  }

  closeDrawer() {
    this.overlayEl?.classList.remove('is-active');
    this.drawerEl?.classList.remove('is-active');
    document.body.style.overflow = '';
  }

  updateCartBadge() {
    const { count } = this.getTotals();
    const cartButtons = document.querySelectorAll('.cart-btn');

    cartButtons.forEach(btn => {
      const svg = btn.querySelector('svg');
      if (svg) {
        btn.innerHTML = '';
        btn.appendChild(svg);
        btn.appendChild(document.createTextNode(` Carrito (${count})`));
      } else {
        btn.textContent = `Carrito (${count})`;
      }
    });
  }

  animateCartButton() {
    const cartButtons = document.querySelectorAll('.cart-btn');
    cartButtons.forEach(btn => {
      btn.classList.remove('is-bumped');
      void btn.offsetWidth;
      btn.classList.add('is-bumped');
    });
  }

  showToast({ title = 'Notificación', message = '', actionText = null, onAction = null, duration = 3500 }) {
    this.ensureDOMStructure();
    if (!this.toastContainerEl) return;

    const toast = document.createElement('div');
    toast.className = 'toast-item';
    toast.innerHTML = `
      <div class="toast-icon-badge">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="20 6 9 17 4 12"></polyline>
        </svg>
      </div>
      <div class="toast-content">
        <h6 class="toast-title">${title}</h6>
        ${message ? `<p class="toast-desc">${message}</p>` : ''}
      </div>
      ${actionText ? `<button class="toast-btn-action" id="toast-act-btn">${actionText}</button>` : ''}
      <button class="toast-close-btn" aria-label="Cerrar">✕</button>
      <div class="toast-progress" style="animation-duration: ${duration}ms;"></div>
    `;

    this.toastContainerEl.appendChild(toast);

    requestAnimationFrame(() => {
      toast.classList.add('is-visible');
    });

    const removeToast = () => {
      toast.classList.remove('is-visible');
      toast.classList.add('is-leaving');
      setTimeout(() => {
        toast.remove();
      }, 350);
    };

    const timer = setTimeout(removeToast, duration);

    toast.querySelector('.toast-close-btn')?.addEventListener('click', () => {
      clearTimeout(timer);
      removeToast();
    });

    if (actionText && onAction) {
      toast.querySelector('#toast-act-btn')?.addEventListener('click', () => {
        clearTimeout(timer);
        removeToast();
        onAction();
      });
    }
  }

  openCheckoutModal() {
    this.ensureDOMStructure();
    const cart = this.getCart();
    if (cart.length === 0) {
      this.showToast({
        title: 'Carrito vacío',
        message: 'Añade al menos un producto para proceder a la compra.'
      });
      return;
    }

    this.closeDrawer();
    this.checkoutStep = 1;
    this.orderResult = null;
    this.renderCheckoutStep();
    this.checkoutModalEl?.classList.add('is-active');
    document.body.style.overflow = 'hidden';
  }

  closeCheckoutModal() {
    this.checkoutModalEl?.classList.remove('is-active');
    document.body.style.overflow = '';
  }

  renderCheckoutStep() {
    this.renderStepper();
    const bodyEl = document.getElementById('checkout-modal-body');
    const footerEl = document.getElementById('checkout-modal-footer');
    const totals = this.getTotals();
    if (!bodyEl || !footerEl) return;

    this.checkoutModalEl.querySelector('#btn-close-checkout').onclick = () => this.closeCheckoutModal();

    switch (this.checkoutStep) {
      case 1:
        this.renderStep1Client(bodyEl, footerEl, totals);
        break;
      case 2:
        this.renderStep2Delivery(bodyEl, footerEl, totals);
        break;
      case 3:
        this.renderStep3Payment(bodyEl, footerEl, totals);
        break;
      case 4:
        this.renderStep4Confirmation(bodyEl, footerEl);
        break;
    }
  }

  renderStepper() {
    const stepperEl = document.getElementById('checkout-stepper');
    if (!stepperEl) return;

    const steps = [
      { num: 1, label: 'Cliente' },
      { num: 2, label: 'Entrega' },
      { num: 3, label: 'Pago' },
      { num: 4, label: 'Confirmación' }
    ];

    stepperEl.innerHTML = steps.map(s => {
      let statusClass = '';
      let icon = s.num;
      if (s.num === this.checkoutStep) {
        statusClass = 'is-active';
      } else if (s.num < this.checkoutStep) {
        statusClass = 'is-done';
        icon = '✓';
      }

      return `
        <div class="checkout-step-item ${statusClass}">
          <div class="checkout-step-circle">${icon}</div>
          <span class="checkout-step-label">${s.label}</span>
        </div>
      `;
    }).join('');
  }

  renderStep1Client(bodyEl, footerEl, totals) {
    const c = this.checkoutData.cliente;

    let docPlaceholder = 'Ej: 72345678';
    let docMaxLen = 8;
    if (c.tipoDoc === 'RUC') {
      docPlaceholder = 'Ej: 20608945123';
      docMaxLen = 11;
    } else if (c.tipoDoc === 'CE') {
      docPlaceholder = 'Ej: 001234567';
      docMaxLen = 12;
    }

    bodyEl.innerHTML = `
      <div>
        <h4 class="checkout-section-subtitle">
          <span>1. Información del Comprador</span>
        </h4>
        <p style="font-size:12px; color:var(--muted); margin-bottom:14px;">
          Ingresa tus datos de contacto y facturación.
        </p>
      </div>

      <div class="checkout-form-grid">
        <div class="checkout-form-group">
          <label class="checkout-label" for="inp-tipo-doc">Tipo de Documento *</label>
          <select id="inp-tipo-doc" class="checkout-select">
            <option value="DNI" ${c.tipoDoc === 'DNI' ? 'selected' : ''}>DNI (8 dígitos)</option>
            <option value="RUC" ${c.tipoDoc === 'RUC' ? 'selected' : ''}>RUC (11 dígitos)</option>
            <option value="CE" ${c.tipoDoc === 'CE' ? 'selected' : ''}>Carné de Extranjería</option>
          </select>
        </div>

        <div class="checkout-form-group">
          <label class="checkout-label" for="inp-num-doc">Número de Documento *</label>
          <input type="text" id="inp-num-doc" class="checkout-input" placeholder="${docPlaceholder}" maxlength="${docMaxLen}" value="${c.numDoc}">
          <span class="checkout-error-msg" id="err-num-doc"></span>
        </div>

        <div class="checkout-form-group full-width">
          <label class="checkout-label" for="inp-nombre">Nombre y Apellidos / Razón Social *</label>
          <input type="text" id="inp-nombre" class="checkout-input" placeholder="Ej: Juan Pérez Morales" value="${c.nombre}">
          <span class="checkout-error-msg" id="err-nombre"></span>
        </div>

        <div class="checkout-form-group">
          <label class="checkout-label" for="inp-email">Correo Electrónico *</label>
          <input type="email" id="inp-email" class="checkout-input" placeholder="correo@ejemplo.com" value="${c.email}">
          <span class="checkout-error-msg" id="err-email"></span>
        </div>

        <div class="checkout-form-group">
          <label class="checkout-label" for="inp-telefono">Teléfono / Celular (9 dígitos) *</label>
          <input type="tel" id="inp-telefono" class="checkout-input" placeholder="Ej: 987654321" maxlength="9" value="${c.telefono}">
          <span class="checkout-error-msg" id="err-telefono"></span>
        </div>
      </div>
    `;

    const tipoDocSelect = bodyEl.querySelector('#inp-tipo-doc');
    const numDocInput = bodyEl.querySelector('#inp-num-doc');
    const telInput = bodyEl.querySelector('#inp-telefono');

    tipoDocSelect?.addEventListener('change', (e) => {
      const selectedType = e.target.value;
      if (selectedType === 'DNI') {
        numDocInput.placeholder = 'Ej: 72345678';
        numDocInput.maxLength = 8;
        numDocInput.value = numDocInput.value.replace(/\D/g, '').slice(0, 8);
      } else if (selectedType === 'RUC') {
        numDocInput.placeholder = 'Ej: 20608945123';
        numDocInput.maxLength = 11;
        numDocInput.value = numDocInput.value.replace(/\D/g, '').slice(0, 11);
      } else {
        numDocInput.placeholder = 'Ej: 001234567';
        numDocInput.maxLength = 12;
        numDocInput.value = numDocInput.value.replace(/[^a-zA-Z0-9]/g, '').slice(0, 12);
      }
      const errNum = bodyEl.querySelector('#err-num-doc');
      if (errNum) errNum.textContent = '';
      numDocInput.classList.remove('has-error');
    });

    numDocInput?.addEventListener('input', (e) => {
      const selectedType = tipoDocSelect?.value || 'DNI';
      if (selectedType === 'DNI') {
        e.target.value = e.target.value.replace(/\D/g, '').slice(0, 8);
      } else if (selectedType === 'RUC') {
        e.target.value = e.target.value.replace(/\D/g, '').slice(0, 11);
      } else {
        e.target.value = e.target.value.replace(/[^a-zA-Z0-9]/g, '').slice(0, 12);
      }
    });

    telInput?.addEventListener('input', (e) => {
      e.target.value = e.target.value.replace(/\D/g, '').slice(0, 9);
    });

    footerEl.innerHTML = `
      <div class="checkout-modal-footer-totals">
        <span class="checkout-footer-subtotal">${totals.count} productos</span>
        <span class="checkout-footer-total">${totals.formattedTotal}</span>
      </div>
      <div class="checkout-modal-buttons">
        <button class="btn-checkout-back" id="btn-step1-cancel">Cancelar</button>
        <button class="btn-checkout-next" id="btn-step1-next">Continuar a Entrega →</button>
      </div>
    `;

    footerEl.querySelector('#btn-step1-cancel').onclick = () => this.closeCheckoutModal();
    footerEl.querySelector('#btn-step1-next').onclick = () => {
      if (this.validateStep1()) {
        this.checkoutStep = 2;
        this.renderCheckoutStep();
      }
    };
  }

  validateStep1() {
    let isValid = true;
    const tipoDoc = document.getElementById('inp-tipo-doc')?.value;
    const numDoc = document.getElementById('inp-num-doc')?.value.trim();
    const nombre = document.getElementById('inp-nombre')?.value.trim();
    const email = document.getElementById('inp-email')?.value.trim();
    const telefono = document.getElementById('inp-telefono')?.value.trim();

    this.checkoutData.cliente = { tipoDoc, numDoc, nombre, email, telefono };

    const errNumDoc = document.getElementById('err-num-doc');
    const errNombre = document.getElementById('err-nombre');
    const errEmail = document.getElementById('err-email');
    const errTelefono = document.getElementById('err-telefono');

    if (tipoDoc === 'DNI') {
      if (!numDoc || !/^\d{8}$/.test(numDoc)) {
        errNumDoc.textContent = 'El DNI debe contener exactamente 8 números.';
        document.getElementById('inp-num-doc')?.classList.add('has-error');
        isValid = false;
      } else {
        errNumDoc.textContent = '';
        document.getElementById('inp-num-doc')?.classList.remove('has-error');
      }
    } else if (tipoDoc === 'RUC') {
      if (!numDoc || !/^\d{11}$/.test(numDoc)) {
        errNumDoc.textContent = 'El RUC debe contener exactamente 11 números.';
        document.getElementById('inp-num-doc')?.classList.add('has-error');
        isValid = false;
      } else {
        errNumDoc.textContent = '';
        document.getElementById('inp-num-doc')?.classList.remove('has-error');
      }
    } else {
      if (!numDoc || numDoc.length < 6 || numDoc.length > 12) {
        errNumDoc.textContent = 'Ingresa un carné de extranjería válido (6 a 12 caracteres).';
        document.getElementById('inp-num-doc')?.classList.add('has-error');
        isValid = false;
      } else {
        errNumDoc.textContent = '';
        document.getElementById('inp-num-doc')?.classList.remove('has-error');
      }
    }

    if (!nombre || nombre.length < 3) {
      errNombre.textContent = 'El nombre completo o razón social es obligatorio.';
      document.getElementById('inp-nombre')?.classList.add('has-error');
      isValid = false;
    } else {
      errNombre.textContent = '';
      document.getElementById('inp-nombre')?.classList.remove('has-error');
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!email || !email.includes('@') || !emailRegex.test(email)) {
      errEmail.textContent = 'Ingresa un correo electrónico válido que incluya "@" y dominio (ej: usuario@correo.com).';
      document.getElementById('inp-email')?.classList.add('has-error');
      isValid = false;
    } else {
      errEmail.textContent = '';
      document.getElementById('inp-email')?.classList.remove('has-error');
    }

    if (!telefono || !/^\d{9}$/.test(telefono)) {
      errTelefono.textContent = 'El teléfono debe contener exactamente 9 números.';
      document.getElementById('inp-telefono')?.classList.add('has-error');
      isValid = false;
    } else {
      errTelefono.textContent = '';
      document.getElementById('inp-telefono')?.classList.remove('has-error');
    }

    return isValid;
  }

  renderStep2Delivery(bodyEl, footerEl, totals) {
    const e = this.checkoutData.entrega;

    bodyEl.innerHTML = `
      <div>
        <h4 class="checkout-section-subtitle">
          <span>2. Método de Entrega</span>
        </h4>
        <p style="font-size:12px; color:var(--muted); margin-bottom:14px;">
          Elige cómo deseas recibir tus productos.
        </p>
      </div>

      <div class="checkout-options-grid">
        <div class="checkout-option-card ${e.metodo === 'domicilio' ? 'is-selected' : ''}" data-delivery="domicilio">
          <div class="option-card-header">
            <span class="option-card-title">Despacho a Domicilio</span>
            <span class="option-card-badge">Envío Gratis</span>
          </div>
          <p class="option-card-desc">Entrega segura en Ica, Lima y provincias con embalaje reforzado.</p>
        </div>

        <div class="checkout-option-card ${e.metodo === 'tienda' ? 'is-selected' : ''}" data-delivery="tienda">
          <div class="option-card-header">
            <span class="option-card-title">Retiro en Tienda</span>
            <span class="option-card-badge">Inmediato</span>
          </div>
          <p class="option-card-desc">Sede Central RYA Tech (Av. Garcilaso de la Vega, Lima).</p>
        </div>
      </div>

      <div id="delivery-extra-fields" style="margin-top: 10px;">
        ${this.getDeliveryFieldsHTML(e)}
      </div>
    `;

    this.bindDeliveryModalEvents(bodyEl);

    footerEl.innerHTML = `
      <div class="checkout-modal-footer-totals">
        <span class="checkout-footer-subtotal">${totals.count} productos</span>
        <span class="checkout-footer-total">${totals.formattedTotal}</span>
      </div>
      <div class="checkout-modal-buttons">
        <button class="btn-checkout-back" id="btn-step2-back">← Volver</button>
        <button class="btn-checkout-next" id="btn-step2-next">Continuar a Pago →</button>
      </div>
    `;

    footerEl.querySelector('#btn-step2-back').onclick = () => {
      this.checkoutStep = 1;
      this.renderCheckoutStep();
    };

    footerEl.querySelector('#btn-step2-next').onclick = () => {
      if (this.validateStep2()) {
        this.checkoutStep = 3;
        this.renderCheckoutStep();
      }
    };
  }

  bindDeliveryModalEvents(bodyEl) {
    const cards = bodyEl.querySelectorAll('[data-delivery]');
    cards.forEach(card => {
      card.onclick = () => {
        cards.forEach(c => c.classList.remove('is-selected'));
        card.classList.add('is-selected');
        this.checkoutData.entrega.metodo = card.dataset.delivery;
        const extraContainer = document.getElementById('delivery-extra-fields');
        if (extraContainer) {
          extraContainer.innerHTML = this.getDeliveryFieldsHTML(this.checkoutData.entrega);
          this.bindDeliveryModalInputRestrictions();
        }
      };
    });
    this.bindDeliveryModalInputRestrictions();
  }

  bindDeliveryModalInputRestrictions() {
    const dniRetiro = document.getElementById('inp-retiro-dni');
    dniRetiro?.addEventListener('input', (ev) => {
      ev.target.value = ev.target.value.replace(/\D/g, '').slice(0, 8);
    });
  }

  getDeliveryFieldsHTML(e) {
    if (e.metodo === 'domicilio') {
      const dep = e.departamento || 'Ica';
      const city = e.ciudad || 'Ica';
      return `
        <div class="checkout-form-grid">
          <div class="checkout-form-group">
            <label class="checkout-label" for="inp-departamento">Departamento *</label>
            <select id="inp-departamento" class="checkout-select">
              <option value="Ica" ${dep === 'Ica' ? 'selected' : ''}>Ica</option>
              <option value="Lima" ${dep === 'Lima' ? 'selected' : ''}>Lima Metropolitana</option>
              <option value="Callao" ${dep === 'Callao' ? 'selected' : ''}>Callao</option>
              <option value="Arequipa" ${dep === 'Arequipa' ? 'selected' : ''}>Arequipa</option>
              <option value="Trujillo" ${dep === 'Trujillo' ? 'selected' : ''}>Trujillo (La Libertad)</option>
              <option value="Cusco" ${dep === 'Cusco' ? 'selected' : ''}>Cusco</option>
              <option value="Chiclayo" ${dep === 'Chiclayo' ? 'selected' : ''}>Chiclayo (Lambayeque)</option>
              <option value="Piura" ${dep === 'Piura' ? 'selected' : ''}>Piura</option>
              <option value="Ancash" ${dep === 'Ancash' ? 'selected' : ''}>Áncash</option>
              <option value="Junin" ${dep === 'Junin' ? 'selected' : ''}>Junín</option>
              <option value="Tacna" ${dep === 'Tacna' ? 'selected' : ''}>Tacna</option>
              <option value="San Martin" ${dep === 'San Martin' ? 'selected' : ''}>San Martín</option>
              <option value="Loreto" ${dep === 'Loreto' ? 'selected' : ''}>Loreto</option>
              <option value="Otros" ${dep === 'Otros' ? 'selected' : ''}>Otro Departamento</option>
            </select>
          </div>

          <div class="checkout-form-group">
            <label class="checkout-label" for="inp-ciudad">Ciudad / Provincia / Distrito *</label>
            <input type="text" id="inp-ciudad" class="checkout-input" placeholder="Ej: Ica Centro / Chincha / Pisco" value="${city}">
            <span class="checkout-error-msg" id="err-ciudad"></span>
          </div>

          <div class="checkout-form-group">
            <label class="checkout-label" for="inp-referencia">Referencia / Urbanización</label>
            <input type="text" id="inp-referencia" class="checkout-input" placeholder="Ej: Frente al parque / Dpto 402" value="${e.referencia || ''}">
          </div>

          <div class="checkout-form-group">
            <label class="checkout-label" for="inp-direccion">Dirección Exacta de Entrega *</label>
            <input type="text" id="inp-direccion" class="checkout-input" placeholder="Av. / Jr. / Calle y Número" value="${e.direccion || ''}">
            <span class="checkout-error-msg" id="err-direccion"></span>
          </div>
        </div>
      `;
    } else {
      return `
        <div class="checkout-payment-box">
          <div style="display:flex; align-items:flex-start; gap:12px;">
            <div>
              <strong style="color:#FFF;">Sede Central RYA Tech - Centro Tecnológico Lima</strong>
              <p style="font-size:12px; color:var(--muted); margin-top:2px;">Av. Garcilaso de la Vega 1348, Piso 2, Tienda 204, Cercado de Lima.</p>
              <p style="font-size:11px; color:var(--color-cyan); margin-top:4px;">Horario de atención: Lun a Sáb de 9:00 a. m. a 7:00 p. m.</p>
            </div>
          </div>
          <div class="checkout-form-grid" style="margin-top:8px;">
            <div class="checkout-form-group">
              <label class="checkout-label" for="inp-retiro-nombre">Persona autorizada (Nombre y Apellidos)</label>
              <input type="text" id="inp-retiro-nombre" class="checkout-input" placeholder="Dejar en blanco si recoge el titular" value="${e.titularRetiroNombre || ''}">
              <span class="checkout-error-msg" id="err-retiro-nombre"></span>
            </div>

            <div class="checkout-form-group">
              <label class="checkout-label" for="inp-retiro-dni">DNI de persona autorizada (8 dígitos)</label>
              <input type="text" id="inp-retiro-dni" class="checkout-input" placeholder="Ej: 72345678" maxlength="8" value="${e.titularRetiroDni || ''}">
              <span class="checkout-error-msg" id="err-retiro-dni"></span>
            </div>
          </div>
        </div>
      `;
    }
  }

  validateStep2() {
    if (this.checkoutData.entrega.metodo === 'domicilio') {
      const departamento = document.getElementById('inp-departamento')?.value || 'Ica';
      const ciudad = document.getElementById('inp-ciudad')?.value.trim();
      const direccion = document.getElementById('inp-direccion')?.value.trim();
      const referencia = document.getElementById('inp-referencia')?.value.trim() || '';

      this.checkoutData.entrega.departamento = departamento;
      this.checkoutData.entrega.ciudad = ciudad;
      this.checkoutData.entrega.direccion = direccion;
      this.checkoutData.entrega.referencia = referencia;

      let isValid = true;
      const errCiudad = document.getElementById('err-ciudad');
      const errDireccion = document.getElementById('err-direccion');

      if (!ciudad || ciudad.length < 2) {
        if (errCiudad) errCiudad.textContent = 'Por favor ingresa la ciudad o distrito de entrega.';
        document.getElementById('inp-ciudad')?.classList.add('has-error');
        isValid = false;
      } else {
        if (errCiudad) errCiudad.textContent = '';
        document.getElementById('inp-ciudad')?.classList.remove('has-error');
      }

      if (!direccion || direccion.length < 5) {
        if (errDireccion) errDireccion.textContent = 'Por favor ingresa una dirección completa de entrega.';
        document.getElementById('inp-direccion')?.classList.add('has-error');
        isValid = false;
      } else {
        if (errDireccion) errDireccion.textContent = '';
        document.getElementById('inp-direccion')?.classList.remove('has-error');
      }

      return isValid;
    } else {
      const titularRetiroNombre = document.getElementById('inp-retiro-nombre')?.value.trim() || '';
      const titularRetiroDni = document.getElementById('inp-retiro-dni')?.value.trim() || '';

      this.checkoutData.entrega.titularRetiroNombre = titularRetiroNombre;
      this.checkoutData.entrega.titularRetiroDni = titularRetiroDni;
      this.checkoutData.entrega.direccion = 'Sede Central RYA Tech (Av. Garcilaso de la Vega 1348, Lima)';

      const errRetiroDni = document.getElementById('err-retiro-dni');
      if (titularRetiroDni && !/^\d{8}$/.test(titularRetiroDni)) {
        if (errRetiroDni) errRetiroDni.textContent = 'El DNI de la persona autorizada debe tener 8 números.';
        document.getElementById('inp-retiro-dni')?.classList.add('has-error');
        return false;
      } else {
        if (errRetiroDni) errRetiroDni.textContent = '';
        document.getElementById('inp-retiro-dni')?.classList.remove('has-error');
        return true;
      }
    }
  }

  renderStep3Payment(bodyEl, footerEl, totals) {
    const p = this.checkoutData.pago;

    bodyEl.innerHTML = `
      <div>
        <h4 class="checkout-section-subtitle">
          <span>3. Método de Pago (Simulado)</span>
        </h4>
        <p style="font-size:12px; color:var(--muted); margin-bottom:14px;">
          Selecciona tu forma de pago preferida para procesar la orden.
        </p>
      </div>

      <div class="checkout-options-grid">
        <div class="checkout-option-card ${p.metodo === 'yape' ? 'is-selected' : ''}" data-payment="yape">
          <div class="option-card-header">
            <span class="option-card-title">Yape / Plin</span>
            <span class="option-card-badge">Instantáneo</span>
          </div>
          <p class="option-card-desc">Pago móvil rápido escaneando QR o a número celular.</p>
        </div>

        <div class="checkout-option-card ${p.metodo === 'tarjeta' ? 'is-selected' : ''}" data-payment="tarjeta">
          <div class="option-card-header">
            <span class="option-card-title">Tarjeta Débito / Crédito</span>
            <span class="option-card-badge">Seguro SSL</span>
          </div>
          <p class="option-card-desc">Visa, Mastercard, American Express o Diners.</p>
        </div>

        <div class="checkout-option-card ${p.metodo === 'transferencia' ? 'is-selected' : ''}" data-payment="transferencia" style="grid-column: span 2;">
          <div class="option-card-header">
            <span class="option-card-title">Transferencia Bancaria Directa</span>
            <span class="option-card-badge">BCP / BBVA / Interbank</span>
          </div>
          <p class="option-card-desc">Depósito a cuentas corrientes empresariales de RYA TECH S.A.C.</p>
        </div>
      </div>

      <div id="payment-details-box">
        ${this.getPaymentFieldsHTML(p, totals)}
      </div>
    `;

    this.bindPaymentModalEvents(bodyEl, totals);

    footerEl.innerHTML = `
      <div class="checkout-modal-footer-totals">
        <span class="checkout-footer-subtotal">Total a pagar con IGV</span>
        <span class="checkout-footer-total">${totals.formattedTotal}</span>
      </div>
      <div class="checkout-modal-buttons">
        <button class="btn-checkout-back" id="btn-step3-back">← Volver</button>
        <button class="btn-checkout-next" id="btn-step3-confirm" style="background:linear-gradient(135deg, #00F5A0 0%, #00b377 100%);">
          Confirmar y Pagar ${totals.formattedTotal}
        </button>
      </div>
    `;

    footerEl.querySelector('#btn-step3-back').onclick = () => {
      this.checkoutStep = 2;
      this.renderCheckoutStep();
    };

    footerEl.querySelector('#btn-step3-confirm').onclick = async () => {
      if (this.validateStep3()) {
        const btn = footerEl.querySelector('#btn-step3-confirm');
        if (btn) {
          btn.disabled = true;
          btn.textContent = 'Procesando pedido...';
        }
        await this.processOrder(totals);
      }
    };
  }

  bindPaymentModalEvents(bodyEl, totals) {
    const cards = bodyEl.querySelectorAll('[data-payment]');
    cards.forEach(card => {
      card.onclick = () => {
        cards.forEach(c => c.classList.remove('is-selected'));
        card.classList.add('is-selected');
        this.checkoutData.pago.metodo = card.dataset.payment;
        const detailsContainer = document.getElementById('payment-details-box');
        if (detailsContainer) {
          detailsContainer.innerHTML = this.getPaymentFieldsHTML(this.checkoutData.pago, totals);
          this.bindPaymentModalInputRestrictions();
        }
      };
    });
    this.bindPaymentModalInputRestrictions();
  }

  bindPaymentModalInputRestrictions() {
    const metodo = this.checkoutData.pago.metodo;
    if (metodo === 'tarjeta') {
      const cardNumInput = document.getElementById('inp-card-number');
      const cardExpInput = document.getElementById('inp-card-exp');
      const cardCvvInput = document.getElementById('inp-card-cvv');

      cardNumInput?.addEventListener('input', (e) => {
        e.target.value = e.target.value.replace(/\D/g, '').slice(0, 16);
      });

      cardExpInput?.addEventListener('input', (e) => {
        let val = e.target.value.replace(/\D/g, '').slice(0, 4);
        if (val.length >= 2) {
          e.target.value = val.slice(0, 2) + '/' + val.slice(2);
        } else {
          e.target.value = val;
        }
      });

      cardCvvInput?.addEventListener('input', (e) => {
        e.target.value = e.target.value.replace(/\D/g, '').slice(0, 3);
      });
    } else {
      const opInput = document.getElementById('inp-numero-op');
      opInput?.addEventListener('input', (e) => {
        e.target.value = e.target.value.replace(/\D/g, '').slice(0, 10);
      });
    }
  }

  getPaymentFieldsHTML(p, totals) {
    if (p.metodo === 'yape') {
      return `
        <div class="checkout-payment-box">
          <div class="qr-payment-container">
            <div class="qr-code-box">
              <!-- SVG Optimizado: Se usa un solo <path> en lugar de múltiples <rect> para no sobrecargar el DOM -->
              <svg width="120" height="120" viewBox="0 0 53 53" fill="none" xmlns="http://www.w3.org/2000/svg">
                <rect width="53" height="53" fill="#FFFFFF"/>
                <path d="M4 4h7v1H4zM23 4h2v1H23zM26 4h6v1H26zM33 4h3v1H33zM37 4h1v1H37zM40 4h1v1H40zM42 4h7v1H42zM4 5h1v1H4zM10 5h1v1H10zM14 5h5v1H14zM22 5h1v1H22zM25 5h1v1H25zM31 5h3v1H31zM37 5h1v1H37zM39 5h1v1H39zM42 5h1v1H42zM48 5h1v1H48zM4 6h1v1H4zM6 6h3v1H6zM10 6h1v1H10zM12 6h1v1H12zM15 6h1v1H15zM17 6h1v1H17zM19 6h1v1H19zM21 6h4v1H21zM26 6h5v1H26zM35 6h1v1H35zM39 6h1v1H39zM42 6h1v1H42zM44 6h3v1H44zM48 6h1v1H48zM4 7h1v1H4zM6 7h3v1H6zM10 7h1v1H10zM12 7h2v1H12zM15 7h2v1H15zM19 7h1v1H19zM22 7h1v1H22zM27 7h1v1H27zM30 7h1v1H30zM32 7h1v1H32zM34 7h1v1H34zM39 7h2v1H39zM42 7h1v1H42zM44 7h3v1H44zM48 7h1v1H48zM4 8h1v1H4zM6 8h3v1H6zM10 8h1v1H10zM12 8h1v1H12zM19 8h1v1H19zM21 8h1v1H21zM23 8h9v1H23zM33 8h1v1H33zM35 8h1v1H35zM37 8h4v1H37zM42 8h1v1H42zM44 8h3v1H44zM48 8h1v1H48zM4 9h1v1H4zM10 9h1v1H10zM12 9h2v1H12zM15 9h2v1H15zM22 9h3v1H22zM28 9h1v1H28zM31 9h2v1H31zM34 9h3v1H34zM42 9h1v1H42zM48 9h1v1H48zM4 10h7v1H4zM12 10h1v1H12zM14 10h1v1H14zM16 10h1v1H16zM18 10h1v1H18zM20 10h1v1H20zM22 10h1v1H22zM24 10h1v1H24zM26 10h1v1H26zM28 10h1v1H28zM30 10h1v1H30zM32 10h1v1H32zM34 10h1v1H34zM36 10h1v1H36zM38 10h1v1H38zM40 10h1v1H40zM42 10h7v1H42zM12 11h3v1H12zM16 11h1v1H16zM19 11h4v1H19zM24 11h1v1H24zM28 11h1v1H28zM33 11h3v1H33zM40 11h1v1H40zM4 12h1v1H4zM6 12h5v1H6zM14 12h2v1H14zM18 12h1v1H18zM20 12h1v1H20zM22 12h1v1H22zM24 12h6v1H24zM34 12h3v1H34zM38 12h1v1H38zM42 12h5v1H42zM4 13h1v1H4zM8 13h2v1H8zM15 13h1v1H15zM17 13h1v1H17zM19 13h1v1H19zM21 13h2v1H21zM24 13h2v1H24zM28 13h3v1H28zM33 13h1v1H33zM39 13h1v1H39zM41 13h3v1H41zM46 13h1v1H46zM4 14h1v1H4zM7 14h1v1H7zM10 14h6v1H10zM18 14h2v1H18zM22 14h1v1H22zM27 14h1v1H27zM31 14h1v1H31zM33 14h5v1H33zM40 14h1v1H40zM42 14h1v1H42zM45 14h2v1H45zM4 15h1v1H4zM6 15h1v1H6zM8 15h2v1H8zM11 15h1v1H11zM13 15h3v1H13zM18 15h2v1H18zM22 15h5v1H22zM29 15h1v1H29zM33 15h3v1H33zM37 15h1v1H37zM39 15h3v1H39zM43 15h1v1H43zM4 16h1v1H4zM6 16h2v1H6zM10 16h6v1H10zM17 16h5v1H17zM24 16h1v1H24zM26 16h2v1H26zM29 16h1v1H29zM31 16h1v1H31zM34 16h3v1H34zM40 16h1v1H40zM43 16h2v1H43zM6 17h1v1H6zM13 17h5v1H13zM22 17h4v1H22zM28 17h1v1H28zM30 17h1v1H30zM33 17h1v1H33zM37 17h1v1H37zM39 17h5v1H39zM45 17h3v1H45zM6 18h2v1H6zM9 18h2v1H9zM18 18h1v1H18zM22 18h1v1H22zM27 18h2v1H27zM31 18h1v1H31zM34 18h2v1H34zM39 18h2v1H39zM42 18h1v1H42zM44 18h1v1H44zM46 18h3v1H46zM4 19h2v1H4zM11 19h1v1H11zM14 19h2v1H14zM17 19h1v1H17zM20 19h3v1H20zM24 19h2v1H24zM27 19h2v1H27zM31 19h1v1H31zM35 19h1v1H35zM37 19h1v1H37zM41 19h2v1H41zM45 19h1v1H45zM47 19h1v1H47zM5 20h2v1H5zM8 20h3v1H8zM13 20h2v1H13zM16 20h2v1H16zM20 20h1v1H20zM31 20h4v1H31zM36 20h2v1H36zM44 20h3v1H44zM4 21h1v1H4zM7 21h2v1H7zM12 21h3v1H12zM16 21h1v1H16zM18 21h2v1H18zM22 21h1v1H22zM24 21h3v1H24zM28 21h3v1H28zM33 21h2v1H33zM38 21h1v1H38zM40 21h4v1H40zM45 21h1v1H45zM47 21h1v1H47zM6 22h2v1H6zM10 22h2v1H10zM14 22h1v1H14zM16 22h1v1H16zM27 22h1v1H27zM31 22h2v1H31zM34 22h4v1H34zM39 22h2v1H39zM45 22h4v1H45zM6 23h4v1H6zM12 23h2v1H12zM20 23h7v1H20zM28 23h1v1H28zM31 23h1v1H31zM34 23h1v1H34zM37 23h2v1H37zM40 23h1v1H40zM4 24h2v1H4zM7 24h6v1H7zM14 24h5v1H14zM20 24h1v1H20zM24 24h6v1H24zM31 24h2v1H31zM34 24h4v1H34zM40 24h6v1H40zM6 25h3v1H6zM12 25h1v1H12zM16 25h1v1H16zM20 25h3v1H20zM24 25h1v1H24zM28 25h1v1H28zM33 25h1v1H33zM38 25h1v1H38zM40 25h1v1H40zM44 25h1v1H44zM46 25h3v1H46zM6 26h1v1H6zM8 26h1v1H8zM10 26h1v1H10zM12 26h1v1H12zM14 26h1v1H14zM16 26h2v1H16zM20 26h2v1H20zM24 26h1v1H24zM26 26h1v1H26zM28 26h2v1H28zM31 26h2v1H31zM34 26h1v1H34zM37 26h1v1H37zM39 26h2v1H39zM42 26h1v1H42zM44 26h2v1H44zM4 27h2v1H4zM8 27h1v1H8zM12 27h1v1H12zM14 27h3v1H14zM18 27h1v1H18zM20 27h1v1H20zM23 27h2v1H23zM28 27h1v1H28zM30 27h2v1H30zM37 27h2v1H37zM40 27h1v1H40zM44 27h1v1H44zM46 27h2v1H46zM4 28h1v1H4zM6 28h1v1H6zM8 28h5v1H8zM15 28h3v1H15zM20 28h2v1H20zM23 28h7v1H23zM31 28h4v1H31zM36 28h1v1H36zM38 28h7v1H38zM4 29h1v1H4zM6 29h1v1H6zM8 29h2v1H8zM11 29h1v1H11zM13 29h2v1H13zM16 29h1v1H16zM19 29h2v1H19zM22 29h1v1H22zM24 29h1v1H24zM28 29h1v1H28zM30 29h2v1H30zM35 29h1v1H35zM38 29h1v1H38zM40 29h2v1H40zM44 29h3v1H44zM4 30h1v1H4zM9 30h4v1H9zM14 30h2v1H14zM18 30h1v1H18zM21 30h3v1H21zM26 30h1v1H26zM28 30h1v1H28zM31 30h1v1H31zM33 30h3v1H33zM37 30h5v1H37zM43 30h1v1H43zM45 30h4v1H45zM4 31h1v1H4zM6 31h1v1H6zM8 31h2v1H8zM14 31h1v1H14zM17 31h1v1H17zM21 31h1v1H21zM27 31h1v1H27zM29 31h1v1H29zM31 31h1v1H31zM35 31h1v1H35zM40 31h1v1H40zM47 31h2v1H47zM4 32h3v1H4zM9 32h5v1H9zM15 32h2v1H15zM18 32h1v1H18zM21 32h8v1H21zM32 32h1v1H32zM34 32h4v1H34zM39 32h1v1H39zM44 32h2v1H44zM47 32h1v1H47zM5 33h3v1H5zM9 33h1v1H9zM12 33h3v1H12zM17 33h4v1H17zM29 33h5v1H29zM38 33h1v1H38zM43 33h1v1H43zM46 33h2v1H46zM4 34h1v1H4zM8 34h1v1H8zM10 34h2v1H10zM13 34h2v1H13zM16 34h1v1H16zM19 34h3v1H19zM24 34h2v1H24zM27 34h2v1H27zM30 34h1v1H30zM32 34h1v1H32zM36 34h2v1H36zM39 34h2v1H39zM42 34h3v1H42zM46 34h3v1H46zM5 35h1v1H5zM7 35h2v1H7zM12 35h1v1H12zM16 35h1v1H16zM18 35h5v1H18zM28 35h2v1H28zM31 35h1v1H31zM34 35h4v1H34zM43 35h3v1H43zM47 35h1v1H47zM4 36h2v1H4zM7 36h6v1H7zM15 36h1v1H15zM17 36h1v1H17zM19 36h1v1H19zM21 36h1v1H21zM24 36h5v1H24zM31 36h2v1H31zM34 36h1v1H34zM39 36h2v1H39zM43 36h2v1H43zM46 36h2v1H46zM4 37h1v1H4zM6 37h1v1H6zM8 37h2v1H8zM12 37h1v1H12zM14 37h4v1H14zM20 37h2v1H20zM23 37h1v1H23zM25 37h2v1H25zM28 37h3v1H28zM33 37h1v1H33zM35 37h1v1H35zM37 37h2v1H37zM40 37h1v1H40zM43 37h3v1H43zM47 37h1v1H47zM8 38h1v1H8zM10 38h1v1H10zM13 38h1v1H13zM15 38h2v1H15zM18 38h2v1H18zM21 38h1v1H21zM23 38h2v1H23zM26 38h3v1H26zM31 38h2v1H31zM34 38h1v1H34zM36 38h2v1H36zM40 38h3v1H40zM45 38h4v1H45zM5 39h4v1H5zM11 39h1v1H11zM14 39h2v1H14zM17 39h1v1H17zM21 39h1v1H21zM24 39h4v1H24zM29 39h3v1H29zM34 39h2v1H34zM38 39h1v1H38zM40 39h1v1H40zM43 39h3v1H43zM4 40h1v1H4zM7 40h2v1H7zM10 40h1v1H10zM14 40h1v1H14zM16 40h1v1H16zM18 40h2v1H18zM21 40h1v1H21zM24 40h5v1H24zM30 40h1v1H30zM32 40h1v1H32zM34 40h4v1H34zM40 40h5v1H40zM46 40h1v1H46zM48 40h1v1H48zM12 41h1v1H12zM14 41h1v1H14zM17 41h1v1H17zM22 41h3v1H22zM28 41h4v1H28zM33 41h1v1H33zM38 41h3v1H38zM44 41h4v1H44zM4 42h7v1H4zM13 42h4v1H13zM18 42h2v1H18zM22 42h1v1H22zM24 42h1v1H24zM26 42h1v1H26zM28 42h1v1H28zM32 42h1v1H32zM34 42h1v1H34zM37 42h1v1H37zM39 42h2v1H39zM42 42h1v1H42zM44 42h1v1H44zM46 42h3v1H46zM4 43h1v1H4zM10 43h1v1H10zM12 43h1v1H12zM16 43h2v1H16zM19 43h1v1H19zM21 43h2v1H21zM24 43h1v1H24zM28 43h2v1H28zM31 43h1v1H31zM33 43h2v1H33zM39 43h2v1H39zM44 43h1v1H44zM46 43h1v1H46zM4 44h1v1H4zM6 44h3v1H6zM10 44h1v1H10zM12 44h3v1H12zM16 44h3v1H16zM20 44h2v1H20zM23 44h7v1H23zM34 44h3v1H34zM40 44h5v1H40zM46 44h1v1H46zM4 45h1v1H4zM6 45h3v1H6zM10 45h1v1H10zM12 45h1v1H12zM15 45h1v1H15zM18 45h6v1H18zM26 45h1v1H26zM28 45h1v1H28zM30 45h1v1H30zM33 45h1v1H33zM38 45h1v1H38zM40 45h1v1H40zM43 45h4v1H43zM4 46h1v1H4zM6 46h3v1H6zM10 46h1v1H10zM12 46h1v1H12zM14 46h1v1H14zM16 46h1v1H16zM20 46h1v1H20zM22 46h1v1H22zM24 46h2v1H24zM27 46h11v1H27zM39 46h1v1H39zM41 46h1v1H41zM44 46h3v1H44zM4 47h1v1H4zM10 47h1v1H10zM14 47h1v1H14zM16 47h3v1H16zM20 47h3v1H20zM31 47h1v1H31zM34 47h2v1H34zM37 47h2v1H37zM40 47h1v1H40zM42 47h2v1H42zM47 47h1v1H47zM4 48h7v1H4zM12 48h1v1H12zM15 48h2v1H15zM18 48h1v1H18zM20 48h5v1H20zM27 48h1v1H27zM31 48h1v1H31zM34 48h4v1H34zM40 48h1v1H40zM46 48h1v1H46z" fill="#742284"/>
              </svg>
            </div>
            <div class="qr-info-text">
              <p>Escanea con <strong>Yape</strong> o <strong>Plin</strong> al número:</p>
              <p style="font-size:18px; color:var(--color-cyan); font-weight:700; margin:4px 0;">987 654 321</p>
              <p>Titular: <strong>RYA TECH S.A.C.</strong></p>
              <p style="font-size:12px; color:#00F5A0; margin-top:2px;">Monto total: ${totals.formattedTotal}</p>
            </div>
          </div>
          <div class="checkout-form-group">
            <label class="checkout-label" for="inp-numero-op">Código de Operación Yape / Plin (Solo números) *</label>
            <input type="text" id="inp-numero-op" class="checkout-input" placeholder="Ej: 839201 (6 a 10 dígitos)" maxlength="10" value="${p.numeroOp || ''}">
            <span class="checkout-error-msg" id="err-numero-op"></span>
          </div>
        </div>
      `;
    } else if (p.metodo === 'tarjeta') {
      return `
        <div class="checkout-payment-box">
          <div class="checkout-form-grid">
            <div class="checkout-form-group full-width">
              <label class="checkout-label" for="inp-card-number">Número de Tarjeta (16 dígitos) *</label>
              <input type="text" id="inp-card-number" class="checkout-input" placeholder="Ej: 4557889900123456" maxlength="16" value="${p.numTarjeta || ''}">
              <span class="checkout-error-msg" id="err-card-number"></span>
            </div>
            <div class="checkout-form-group full-width">
              <label class="checkout-label" for="inp-card-name">Nombre impreso en la tarjeta *</label>
              <input type="text" id="inp-card-name" class="checkout-input" placeholder="JUAN PEREZ M" value="${p.titularTarjeta || ''}">
              <span class="checkout-error-msg" id="err-card-name"></span>
            </div>
            <div class="checkout-form-group">
              <label class="checkout-label" for="inp-card-exp">Vencimiento (MM/AA) *</label>
              <input type="text" id="inp-card-exp" class="checkout-input" placeholder="12/26" maxlength="5" value="${p.expiracion || ''}">
              <span class="checkout-error-msg" id="err-card-exp"></span>
            </div>
            <div class="checkout-form-group">
              <label class="checkout-label" for="inp-card-cvv">CVV (3 dígitos) *</label>
              <input type="password" id="inp-card-cvv" class="checkout-input" placeholder="123" maxlength="3" value="${p.cvv || ''}">
              <span class="checkout-error-msg" id="err-card-cvv"></span>
            </div>
          </div>
        </div>
      `;
    } else {
      return `
        <div class="checkout-payment-box">
          <div style="font-size:12px; line-height:1.5; color:var(--muted);">
            <p><strong style="color:#FFF;">Cuenta Corriente BCP Soles:</strong> 191-98765432-0-12</p>
            <p><strong style="color:#FFF;">Código Interbancario (CCI):</strong> 00219100987654320124</p>
            <p><strong style="color:#FFF;">Cuenta Corriente BBVA Soles:</strong> 0011-0123-0100045678</p>
            <p>Beneficiario: <strong>RYA TECH S.A.C.</strong> | RUC: 20608945123</p>
          </div>
          <div class="checkout-form-group" style="margin-top:6px;">
            <label class="checkout-label" for="inp-numero-op">Código de Operación Bancaria (Solo números) *</label>
            <input type="text" id="inp-numero-op" class="checkout-input" placeholder="Ej: 4920192 (6 a 10 dígitos)" maxlength="10" value="${p.numeroOp || ''}">
            <span class="checkout-error-msg" id="err-numero-op"></span>
          </div>
        </div>
      `;
    }
  }

  validateStep3() {
    const metodo = this.checkoutData.pago.metodo;

    if (metodo === 'tarjeta') {
      const num = document.getElementById('inp-card-number')?.value.trim();
      const name = document.getElementById('inp-card-name')?.value.trim();
      const exp = document.getElementById('inp-card-exp')?.value.trim();
      const cvv = document.getElementById('inp-card-cvv')?.value.trim();

      this.checkoutData.pago.numTarjeta = num;
      this.checkoutData.pago.titularTarjeta = name;
      this.checkoutData.pago.expiracion = exp;
      this.checkoutData.pago.cvv = cvv;

      let isValid = true;
      if (!num || !/^\d{16}$/.test(num)) {
        document.getElementById('err-card-number').textContent = 'El número de tarjeta debe tener exactamente 16 números.';
        document.getElementById('inp-card-number')?.classList.add('has-error');
        isValid = false;
      } else {
        document.getElementById('err-card-number').textContent = '';
        document.getElementById('inp-card-number')?.classList.remove('has-error');
      }

      if (!name || name.length < 3) {
        document.getElementById('err-card-name').textContent = 'Ingresa el nombre del titular impreso en la tarjeta.';
        document.getElementById('inp-card-name')?.classList.add('has-error');
        isValid = false;
      } else {
        document.getElementById('err-card-name').textContent = '';
        document.getElementById('inp-card-name')?.classList.remove('has-error');
      }

      if (!exp || !/^(0[1-9]|1[0-2])\/\d{2}$/.test(exp)) {
        document.getElementById('err-card-exp').textContent = 'Ingresa una fecha de vencimiento válida (MM/AA, ej: 12/26).';
        document.getElementById('inp-card-exp')?.classList.add('has-error');
        isValid = false;
      } else {
        document.getElementById('err-card-exp').textContent = '';
        document.getElementById('inp-card-exp')?.classList.remove('has-error');
      }

      if (!cvv || !/^\d{3}$/.test(cvv)) {
        document.getElementById('err-card-cvv').textContent = 'El CVV debe tener exactamente 3 números.';
        document.getElementById('inp-card-cvv')?.classList.add('has-error');
        isValid = false;
      } else {
        document.getElementById('err-card-cvv').textContent = '';
        document.getElementById('inp-card-cvv')?.classList.remove('has-error');
      }

      return isValid;
    } else {
      const op = document.getElementById('inp-numero-op')?.value.trim();
      this.checkoutData.pago.numeroOp = op;

      if (!op || !/^\d{6,10}$/.test(op)) {
        document.getElementById('err-numero-op').textContent = 'El código de operación debe contener entre 6 y 10 dígitos numéricos.';
        document.getElementById('inp-numero-op')?.classList.add('has-error');
        return false;
      } else {
        document.getElementById('err-numero-op').textContent = '';
        document.getElementById('inp-numero-op')?.classList.remove('has-error');
        return true;
      }
    }
  }

  async processOrder(totals) {
    let orderUUID = generateUUID();
    let orderNumber = `PED-2026-${String(Math.floor(1000 + Math.random() * 9000))}`;
    let userUUID = generateUUID();
    const now = new Date().toISOString();
    const cart = this.getCart();

    const isDomicilio = this.checkoutData.entrega.metodo === 'domicilio';
    const entregaDesc = isDomicilio
      ? `Despacho a Domicilio (${this.checkoutData.entrega.departamento}, ${this.checkoutData.entrega.ciudad} - ${this.checkoutData.entrega.direccion}${this.checkoutData.entrega.referencia ? ' | Ref: ' + this.checkoutData.entrega.referencia : ''})`
      : `Retiro en Tienda Central RYA Tech${this.checkoutData.entrega.titularRetiroNombre ? ' (Autorizado: ' + this.checkoutData.entrega.titularRetiroNombre + (this.checkoutData.entrega.titularRetiroDni ? ' - DNI: ' + this.checkoutData.entrega.titularRetiroDni : '') + ')' : ''}`;

    const isUuid = (val) => typeof val === 'string' && /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(val);
    const hasBackendApi = typeof window !== 'undefined' && window.api && typeof window.api.crearPedido === 'function';

    if (hasBackendApi) {
      try {
        const usuarioBackend = await window.api.registrarOObtenerUsuario(this.checkoutData.cliente);
        if (usuarioBackend && usuarioBackend.id) {
          userUUID = usuarioBackend.id;
          const allItemsAreUuids = cart.every(item => isUuid(item.id));
          if (allItemsAreUuids && cart.length > 0) {
            const pedidoBackend = await window.api.crearPedido({
              usuarioId: usuarioBackend.id,
              metodoPago: this.checkoutData.pago.metodo.toUpperCase(),
              detalles: cart.map(item => ({
                productoId: item.id,
                cantidad: Number(item.cantidad) || 1
              }))
            });

            if (pedidoBackend && pedidoBackend.id) {
              orderUUID = pedidoBackend.id;
              orderNumber = `PED-${pedidoBackend.id.substring(0, 8).toUpperCase()}`;
              console.log('Pedido registrado exitosamente en backend:', pedidoBackend);
            }
          }
        }
      } catch (backendErr) {
        console.warn('No se pudo completar el pedido en el backend, usando modo offline:', backendErr.message);
      }
    }

    const orderPayload = {
      id: orderUUID,
      codigoOrden: orderNumber,
      usuarioId: userUUID,
      usuarioNombre: this.checkoutData.cliente.nombre,
      usuarioEmail: this.checkoutData.cliente.email,
      usuarioTelefono: this.checkoutData.cliente.telefono,
      usuarioDocumento: `${this.checkoutData.cliente.tipoDoc}: ${this.checkoutData.cliente.numDoc}`,
      metodoEntrega: entregaDesc,
      metodoPago: this.checkoutData.pago.metodo.toUpperCase(),
      estado: 'CONFIRMADO',
      montoTotal: totals.total,
      montoSubtotal: totals.subtotal,
      montoIgv: totals.igv,
      formattedTotal: totals.formattedTotal,
      formattedSubtotal: totals.formattedSubtotal,
      formattedIgv: totals.formattedIgv,
      fechaRegistro: now,
      detalles: cart.map(item => ({
        productoId: item.id,
        fabricante: item.fabricante,
        categoria: item.categoria,
        detalle: item.detalle,
        precioUnitario: item.precio,
        cantidad: item.cantidad,
        subtotal: item.precio * item.cantidad
      }))
    };

    try {
      const orders = JSON.parse(localStorage.getItem(ORDERS_STORAGE_KEY) || '[]');
      orders.unshift(orderPayload);
      localStorage.setItem(ORDERS_STORAGE_KEY, JSON.stringify(orders));
    } catch (e) {
      console.warn('No se pudo guardar en historial de órdenes:', e);
    }

    this.orderResult = orderPayload;

    this.saveCart([]);

    this.checkoutStep = 4;
    this.renderCheckoutStep();

    this.showToast({
      title: '¡Orden Confirmada!',
      message: `Tu pedido ${orderNumber} ha sido procesado con éxito.`,
      duration: 5000
    });
  }

  renderStep4Confirmation(bodyEl, footerEl) {
    const o = this.orderResult;
    if (!o) return;

    bodyEl.innerHTML = `
      <div class="order-success-card">
        <div class="order-success-icon">
          <svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="20 6 9 17 4 12"></polyline>
          </svg>
        </div>

        <div>
          <h3 class="order-success-title">¡Pedido Registrado con Éxito!</h3>
          <p style="font-size:13px; color:var(--muted); margin-top:4px;">
            Hemos enviado el comprobante detallado a <strong>${o.usuarioEmail}</strong>.
          </p>
        </div>

        <div class="order-success-order-id">
          <span>ORDEN: ${o.codigoOrden}</span>
        </div>

        <div class="order-receipt-voucher" id="order-voucher-print-area">
          <div class="receipt-info-grid">
            <div class="receipt-field">
              <label>Cliente:</label>
              <span>${o.usuarioNombre}</span>
            </div>
            <div class="receipt-field">
              <label>Documento:</label>
              <span>${o.usuarioDocumento}</span>
            </div>
            <div class="receipt-field">
              <label>Teléfono:</label>
              <span>${o.usuarioTelefono}</span>
            </div>
            <div class="receipt-field">
              <label>Método de Pago:</label>
              <span>${o.metodoPago}</span>
            </div>
            <div class="receipt-field" style="grid-column: span 2;">
              <label>Entrega:</label>
              <span style="font-size:11px;">${o.metodoEntrega}</span>
            </div>
          </div>

          <div style="font-weight:700; font-size:12px; color:#FFF; margin-top:4px;">Productos Comprados:</div>
          <div class="receipt-items-list">
            ${o.detalles.map(d => `
              <div class="receipt-item-row">
                <div>
                  <strong style="color:var(--color-cyan);">${d.cantidad}x</strong>
                  <span class="item-name">${d.fabricante} ${d.categoria}</span>
                </div>
                <span>${formatCurrency(d.subtotal)}</span>
              </div>
            `).join('')}
          </div>

          <div class="receipt-totals-box">
            <div class="receipt-total-row">
              <span>Subtotal (Base imponible):</span>
              <span>${o.formattedSubtotal}</span>
            </div>
            <div class="receipt-total-row">
              <span>IGV (18% incluido):</span>
              <span>${o.formattedIgv}</span>
            </div>
            <div class="receipt-total-row grand-total">
              <span>Total Pagado:</span>
              <span>${o.formattedTotal}</span>
            </div>
          </div>
        </div>

        <div class="order-voucher-actions">
          <button class="btn-voucher-print" id="btn-print-order">
            Imprimir Resumen
          </button>
          <a class="btn-voucher-whatsapp" id="btn-whatsapp-order" target="_blank" rel="noopener">
            Asesor WhatsApp
          </a>
          <button class="btn-voucher-continue" id="btn-finish-shopping">
            Seguir Comprando
          </button>
        </div>
      </div>
    `;

    footerEl.innerHTML = '';
    footerEl.style.display = 'none';

    const waText = encodeURIComponent(
      `¡Hola RYA Tech! Acabo de registrar mi pedido *${o.codigoOrden}* por un total de *${o.formattedTotal}* a nombre de *${o.usuarioNombre}*. Deseo consultar el estado de mi despacho.`
    );
    const waBtn = bodyEl.querySelector('#btn-whatsapp-order');
    if (waBtn) waBtn.href = `https://wa.me/51987654321?text=${waText}`;

    bodyEl.querySelector('#btn-print-order').onclick = () => {
      window.print();
    };

    bodyEl.querySelector('#btn-finish-shopping').onclick = () => {
      this.closeCheckoutModal();
      window.location.href = 'index.html';
    };
  }

  bindGlobalEvents() {
    document.addEventListener('click', (e) => {
      const cartBtn = e.target.closest('.cart-btn');
      if (cartBtn) {
        e.preventDefault();
        this.openDrawer();
      }
    });

    this.overlayEl?.addEventListener('click', () => this.closeDrawer());
    document.getElementById('btn-close-cart')?.addEventListener('click', () => this.closeDrawer());

    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape') {
        this.closeDrawer();
        this.closeCheckoutModal();
      }
    });

    window.addEventListener('storage', (e) => {
      if (e.key === STORAGE_KEY) {
        this.updateCartBadge();
        if (this.drawerEl?.classList.contains('is-active')) {
          this.renderDrawer();
        }
      }
    });
  }
}

const Cart = new CartManager();

if (typeof document !== 'undefined') {
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => Cart.init());
  } else {
    Cart.init();
  }
}

if (typeof window !== 'undefined') {
  window.Cart = Cart;
  window.formatCurrency = formatCurrency;
  window.agregarAlCarrito = (id) => Cart.addItemById(id);
}

