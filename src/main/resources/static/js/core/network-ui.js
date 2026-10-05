

function inicializarMonitorDeRed() {
  if (typeof window === 'undefined' || window.__networkMonitorInit) return;
  window.__networkMonitorInit = true;

  let toastEl = document.getElementById('network-status-toast');
  if (!toastEl && document.body) {
    toastEl = document.createElement('div');
    toastEl.id = 'network-status-toast';
    toastEl.className = 'network-status-toast';
    toastEl.setAttribute('role', 'status');
    toastEl.setAttribute('aria-live', 'polite');
    document.body.appendChild(toastEl);
  }

  let toastTimeout = null;

  const showToast = (isOnline) => {
    if (!toastEl) return;
    clearTimeout(toastTimeout);
    toastEl.className = `network-status-toast is-visible ${isOnline ? 'is-online' : 'is-offline'}`;
    toastEl.innerHTML = isOnline
      ? `<span>⚡</span> <span>Conexión restablecida. Sincronizando catálogo...</span>`
      : `<span>⚠️</span> <span>Sin conexión a internet. Mostrando datos locales.</span>`;

    toastTimeout = setTimeout(() => {
      toastEl.classList.remove('is-visible');
    }, 4500);
  };

  window.addEventListener('offline', () => showToast(false));
  window.addEventListener('online', () => showToast(true));
}

function crearSkeletonCard() {
  const card = document.createElement('div');
  card.className = 'skeleton-card';
  card.innerHTML = `
    <div class="skeleton-img skeleton-shimmer"></div>
    <div class="skeleton-brand skeleton-shimmer"></div>
    <div class="skeleton-title skeleton-shimmer"></div>
    <div class="skeleton-spec skeleton-shimmer"></div>
    <div class="skeleton-price skeleton-shimmer"></div>
    <div class="skeleton-btn skeleton-shimmer"></div>
  `;
  return card;
}

function renderizarSkeletonsCatalogo(contenedor, numSecciones = 3, numCards = 4) {
  if (!contenedor) return;
  contenedor.innerHTML = '';

  for (let s = 0; s < numSecciones; s++) {
    const section = document.createElement('section');
    section.className = 'skeleton-carousel-section';
    section.innerHTML = `
      <div class="skeleton-header-row">
        <div class="skeleton-section-title skeleton-shimmer"></div>
        <div class="skeleton-controls">
          <div class="skeleton-circle-btn skeleton-shimmer"></div>
          <div class="skeleton-circle-btn skeleton-shimmer"></div>
        </div>
      </div>
      <div class="skeleton-track-row"></div>
    `;

    const track = section.querySelector('.skeleton-track-row');
    for (let c = 0; c < numCards; c++) {
      track.appendChild(crearSkeletonCard());
    }

    contenedor.appendChild(section);
  }
}

function renderizarSkeletonDetalle(contenedor) {
  if (!contenedor) return;
  contenedor.innerHTML = `
    <div class="skeleton-product-hero">
      <div class="skeleton-gallery-img skeleton-shimmer"></div>
      <div class="skeleton-detail-info">
        <div class="skeleton-detail-brand skeleton-shimmer"></div>
        <div class="skeleton-detail-title skeleton-shimmer"></div>
        <div class="skeleton-detail-price skeleton-shimmer"></div>
        <div class="skeleton-detail-desc skeleton-shimmer"></div>
        <div class="skeleton-detail-actions">
          <div class="skeleton-detail-qty skeleton-shimmer"></div>
          <div class="skeleton-detail-btn skeleton-shimmer"></div>
        </div>
      </div>
    </div>
  `;
}

function crearTarjetaErrorRed({
  titulo = 'Error de Conexión con el Catálogo',
  mensaje = 'No pudimos obtener la información del servidor de RYA Tech. Verifica tu conexión a internet o reintenta.',
  onRetry = null,
  linkSecundario = null
}) {
  const card = document.createElement('div');
  card.className = 'network-error-card';
  card.setAttribute('role', 'alert');

  card.innerHTML = `
    <div class="network-error-icon">
      <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
        <line x1="1" y1="1" x2="23" y2="23"></line>
        <path d="M16.72 11.06A10.94 10.94 0 0 1 19 12.55"></path>
        <path d="M5 12.55a10.94 10.94 0 0 1 5.17-2.39"></path>
        <path d="M10.71 5.05A16 16 0 0 1 22.58 9"></path>
        <path d="M1.42 9a15.91 15.91 0 0 1 4.7-2.88"></path>
        <path d="M8.53 16.11a6 6 0 0 1 6.95 0"></path>
        <line x1="12" y1="20" x2="12.01" y2="20"></line>
      </svg>
    </div>
    <h3>${titulo}</h3>
    <p>${mensaje}</p>
    <div class="network-error-actions">
      ${onRetry ? `
        <button type="button" class="btn-network-retry" id="btn-retry-network-action">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="23 4 23 10 17 10"></polyline>
            <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"></path>
          </svg>
          Reintentar Conexión
        </button>
      ` : ''}
      ${linkSecundario ? `
        <a href="${linkSecundario.href}" class="btn-network-secondary">${linkSecundario.texto}</a>
      ` : ''}
    </div>
  `;

  if (onRetry) {
    card.querySelector('#btn-retry-network-action')?.addEventListener('click', () => {
      onRetry();
    });
  }

  return card;
}

function verificarAccesoAdmin() {
  if (typeof window === 'undefined') return;
  try {
    const raw = sessionStorage.getItem('rya_usuario');
    if (!raw) return;
    const user = JSON.parse(raw);
    const esAdmin = user && (user.rol === 'ADMIN' || (user.rol && user.rol.nombre === 'ADMIN'));
    if (!esAdmin) return;

    const navUl = document.querySelector('header nav ul');
    if (navUl && !document.getElementById('nav-admin-link')) {
      const li = document.createElement('li');
      li.id = 'nav-admin-link';
      const actualPath = window.location.pathname;
      const isAdminPage = actualPath.endsWith('admin.html');
      li.innerHTML = `<a href="admin.html" ${isAdminPage ? 'class="is-active"' : ''} style="color:var(--color-cyan); font-weight:600;">Administración</a>`;
      navUl.appendChild(li);
    }
  } catch (e) {
    console.warn('Error verificando rol de administrador:', e);
  }
}

if (typeof window !== 'undefined') {
  window.inicializarMonitorDeRed = inicializarMonitorDeRed;
  window.crearSkeletonCard = crearSkeletonCard;
  window.renderizarSkeletonsCatalogo = renderizarSkeletonsCatalogo;
  window.renderizarSkeletonDetalle = renderizarSkeletonDetalle;
  window.crearTarjetaErrorRed = crearTarjetaErrorRed;
  window.verificarAccesoAdmin = verificarAccesoAdmin;

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', verificarAccesoAdmin);
  } else {
    verificarAccesoAdmin();
  }
}
