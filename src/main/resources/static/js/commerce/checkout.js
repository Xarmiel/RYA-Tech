class CheckoutPageController {
  constructor() {
    this.rootEl = null;
    this.currentStep = 1;
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
    this.rootEl = document.getElementById('checkout-app-root');
    if (!this.rootEl) return;

    Cart.init();
    this.render();

    window.addEventListener('cart:updated', () => {
      if (this.currentStep !== 4) {
        this.render();
      }
    });
  }

  render() {
    const cart = Cart.getCart();

    if (cart.length === 0 && this.currentStep !== 4) {
      this.renderEmptyNotice();
      return;
    }

    if (this.currentStep === 4) {
      this.renderStep4Confirmation();
      return;
    }

    const totals = Cart.getTotals();

    this.rootEl.innerHTML = `
      <div class="checkout-grid-layout">
        <!-- Columna Izquierda: Tarjeta del Proceso Multipaso -->
        <section class="checkout-main-card">
          <div class="checkout-stepper-container" id="page-stepper">
            ${this.getStepperHTML()}
          </div>
          <div class="checkout-step-content" id="step-content-area">
            <!-- Inyectado por el paso actual -->
          </div>
        </section>

        <!-- Columna Derecha: Resumen del Pedido Lateral -->
        <aside class="checkout-summary-card">
          <div class="summary-card-title">
            <span>Resumen de Compra</span>
            <span style="font-size:12px; color:var(--color-cyan); font-weight:600;">${totals.count} items</span>
          </div>

          <div class="summary-items-list">
            ${cart.map(item => `
              <div class="summary-item-row">
                <div class="summary-item-thumb">
                  <img src="${getProductThumbnail(item)}" alt="${item.fabricante}">
                </div>
                <div class="summary-item-info">
                  <span class="summary-item-brand">${item.fabricante}</span>
                  <p class="summary-item-name">${item.categoria}</p>
                  <span class="summary-item-qty">${item.cantidad} und. × ${formatCurrency(item.precio)}</span>
                </div>
                <span class="summary-item-price">${formatCurrency(item.precio * item.cantidad)}</span>
              </div>
            `).join('')}
          </div>

          <div class="summary-totals-breakdown">
            <div class="summary-row">
              <span>Subtotal (Base imponible)</span>
              <span>${totals.formattedSubtotal}</span>
            </div>
            <div class="summary-row">
              <span>IGV (18% incluido)</span>
              <span>${totals.formattedIgv}</span>
            </div>
            <div class="summary-row total-row">
              <span>Total a pagar</span>
              <span>${totals.formattedTotal}</span>
            </div>
          </div>

          <div class="summary-guarantees">
            <div><span>✓</span> <span>Garantía oficial del fabricante</span></div>
            <div><span>✓</span> <span>Embalaje reforzado para transporte</span></div>
            <div><span>✓</span> <span>Atención y soporte postventa</span></div>
          </div>
        </aside>
      </div>
    `;

    this.renderCurrentStepContent(totals);
  }

  renderEmptyNotice() {
    this.rootEl.innerHTML = `
      <div class="checkout-main-card" style="text-align: center; padding: 60px 24px; max-width: 600px; margin: 0 auto;">
        <h2 style="font-family:'Space Grotesk', sans-serif; font-size:22px; color:#FFF;">No tienes productos en el carrito</h2>
        <p style="font-size:14px; color:var(--muted); margin: 12px 0 24px 0;">
          Para proceder con la compra, añade los componentes o periféricos que deseas adquirir desde nuestro catálogo.
        </p>
        <a href="index.html" class="btn-checkout-next" style="display:inline-flex; justify-content:center; text-decoration:none; margin: 0 auto;">
          Explorar Catálogo →
        </a>
      </div>
    `;
  }

  getStepperHTML() {
    const steps = [
      { num: 1, label: 'Cliente' },
      { num: 2, label: 'Entrega' },
      { num: 3, label: 'Pago' },
      { num: 4, label: 'Confirmación' }
    ];

    return steps.map(s => {
      let statusClass = '';
      let icon = s.num;
      if (s.num === this.currentStep) {
        statusClass = 'is-active';
      } else if (s.num < this.currentStep) {
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

  renderCurrentStepContent(totals) {
    const contentArea = document.getElementById('step-content-area');
    if (!contentArea) return;

    if (this.currentStep === 1) {
      this.renderStep1(contentArea);
    } else if (this.currentStep === 2) {
      this.renderStep2(contentArea);
    } else if (this.currentStep === 3) {
      this.renderStep3(contentArea, totals);
    }
  }

  renderStep1(container) {
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

    container.innerHTML = `
      <div>
        <h3 class="checkout-section-subtitle">1. Datos del Cliente</h3>
        <p style="font-size:13px; color:var(--muted);">Ingresa tus datos personales y de facturación.</p>
      </div>

      <div class="checkout-form-grid">
        <div class="checkout-form-group">
          <label class="checkout-label" for="step1-tipo-doc">Tipo de Documento *</label>
          <select id="step1-tipo-doc" class="checkout-select">
            <option value="DNI" ${c.tipoDoc === 'DNI' ? 'selected' : ''}>DNI (8 dígitos)</option>
            <option value="RUC" ${c.tipoDoc === 'RUC' ? 'selected' : ''}>RUC (11 dígitos)</option>
            <option value="CE" ${c.tipoDoc === 'CE' ? 'selected' : ''}>Carné de Extranjería</option>
          </select>
        </div>

        <div class="checkout-form-group">
          <label class="checkout-label" for="step1-num-doc">Número de Documento *</label>
          <input type="text" id="step1-num-doc" class="checkout-input" placeholder="${docPlaceholder}" maxlength="${docMaxLen}" value="${c.numDoc}">
          <span class="checkout-error-msg" id="err-step1-num-doc"></span>
        </div>

        <div class="checkout-form-group full-width">
          <label class="checkout-label" for="step1-nombre">Nombre y Apellidos / Razón Social *</label>
          <input type="text" id="step1-nombre" class="checkout-input" placeholder="Ej: Juan Pérez Morales" value="${c.nombre}">
          <span class="checkout-error-msg" id="err-step1-nombre"></span>
        </div>

        <div class="checkout-form-group">
          <label class="checkout-label" for="step1-email">Correo Electrónico *</label>
          <input type="email" id="step1-email" class="checkout-input" placeholder="correo@ejemplo.com" value="${c.email}">
          <span class="checkout-error-msg" id="err-step1-email"></span>
        </div>

        <div class="checkout-form-group">
          <label class="checkout-label" for="step1-telefono">Teléfono / Celular (9 dígitos) *</label>
          <input type="tel" id="step1-telefono" class="checkout-input" placeholder="Ej: 987654321" maxlength="9" value="${c.telefono}">
          <span class="checkout-error-msg" id="err-step1-telefono"></span>
        </div>
      </div>

      <div class="checkout-actions-bar">
        <a href="index.html" class="btn-checkout-back">Seguir Comprando</a>
        <button class="btn-checkout-next" id="btn-goto-step2">Continuar a Entrega →</button>
      </div>
    `;

    const tipoDocSelect = document.getElementById('step1-tipo-doc');
    const numDocInput = document.getElementById('step1-num-doc');
    const telInput = document.getElementById('step1-telefono');

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
      const errNum = document.getElementById('err-step1-num-doc');
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

    document.getElementById('btn-goto-step2').onclick = () => {
      if (this.validateStep1()) {
        this.currentStep = 2;
        this.render();
        window.scrollTo({ top: 0, behavior: 'smooth' });
      }
    };
  }

  validateStep1() {
    let isValid = true;
    const tipoDoc = document.getElementById('step1-tipo-doc')?.value;
    const numDoc = document.getElementById('step1-num-doc')?.value.trim();
    const nombre = document.getElementById('step1-nombre')?.value.trim();
    const email = document.getElementById('step1-email')?.value.trim();
    const telefono = document.getElementById('step1-telefono')?.value.trim();

    this.checkoutData.cliente = { tipoDoc, numDoc, nombre, email, telefono };

    const errNum = document.getElementById('err-step1-num-doc');
    const errNom = document.getElementById('err-step1-nombre');
    const errEma = document.getElementById('err-step1-email');
    const errTel = document.getElementById('err-step1-telefono');

    if (tipoDoc === 'DNI') {
      if (!numDoc || !/^\d{8}$/.test(numDoc)) {
        errNum.textContent = 'El DNI debe contener exactamente 8 números.';
        document.getElementById('step1-num-doc')?.classList.add('has-error');
        isValid = false;
      } else {
        errNum.textContent = '';
        document.getElementById('step1-num-doc')?.classList.remove('has-error');
      }
    } else if (tipoDoc === 'RUC') {
      if (!numDoc || !/^\d{11}$/.test(numDoc)) {
        errNum.textContent = 'El RUC debe contener exactamente 11 números.';
        document.getElementById('step1-num-doc')?.classList.add('has-error');
        isValid = false;
      } else {
        errNum.textContent = '';
        document.getElementById('step1-num-doc')?.classList.remove('has-error');
      }
    } else {
      if (!numDoc || numDoc.length < 6 || numDoc.length > 12) {
        errNum.textContent = 'Ingresa un carné de extranjería válido (6 a 12 caracteres).';
        document.getElementById('step1-num-doc')?.classList.add('has-error');
        isValid = false;
      } else {
        errNum.textContent = '';
        document.getElementById('step1-num-doc')?.classList.remove('has-error');
      }
    }

    if (!nombre || nombre.length < 3) {
      errNom.textContent = 'El nombre completo o razón social es obligatorio.';
      document.getElementById('step1-nombre')?.classList.add('has-error');
      isValid = false;
    } else {
      errNom.textContent = '';
      document.getElementById('step1-nombre')?.classList.remove('has-error');
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!email || !email.includes('@') || !emailRegex.test(email)) {
      errEma.textContent = 'Ingresa un correo electrónico válido que incluya "@" y dominio (ej: usuario@correo.com).';
      document.getElementById('step1-email')?.classList.add('has-error');
      isValid = false;
    } else {
      errEma.textContent = '';
      document.getElementById('step1-email')?.classList.remove('has-error');
    }

    if (!telefono || !/^\d{9}$/.test(telefono)) {
      errTel.textContent = 'El teléfono debe contener exactamente 9 números.';
      document.getElementById('step1-telefono')?.classList.add('has-error');
      isValid = false;
    } else {
      errTel.textContent = '';
      document.getElementById('step1-telefono')?.classList.remove('has-error');
    }

    return isValid;
  }

  renderStep2(container) {
    const e = this.checkoutData.entrega;

    container.innerHTML = `
      <div>
        <h3 class="checkout-section-subtitle">2. Método de Entrega</h3>
        <p style="font-size:13px; color:var(--muted);">Selecciona la modalidad de envío o recojo.</p>
      </div>

      <div class="checkout-options-grid">
        <div class="checkout-option-card ${e.metodo === 'domicilio' ? 'is-selected' : ''}" data-delivery="domicilio">
          <div class="option-card-header">
            <span class="option-card-title">Despacho a Domicilio</span>
            <span class="option-card-badge">Envío Gratis</span>
          </div>
          <p class="option-card-desc">Entrega a domicilio en Ica, Lima y todas las provincias con embalaje de seguridad.</p>
        </div>

        <div class="checkout-option-card ${e.metodo === 'tienda' ? 'is-selected' : ''}" data-delivery="tienda">
          <div class="option-card-header">
            <span class="option-card-title">Retiro en Tienda</span>
            <span class="option-card-badge">Inmediato</span>
          </div>
          <p class="option-card-desc">Sede Central RYA Tech (Av. Garcilaso de la Vega, Lima).</p>
        </div>
      </div>

      <div id="page-delivery-extra-fields">
        ${this.getDeliveryFieldsHTML(e)}
      </div>

      <div class="checkout-actions-bar">
        <button class="btn-checkout-back" id="btn-back-to-step1">← Volver a Datos</button>
        <button class="btn-checkout-next" id="btn-goto-step3">Continuar a Pago →</button>
      </div>
    `;

    this.bindDeliveryEvents(container);

    document.getElementById('btn-back-to-step1').onclick = () => {
      this.currentStep = 1;
      this.render();
      window.scrollTo({ top: 0, behavior: 'smooth' });
    };

    document.getElementById('btn-goto-step3').onclick = () => {
      if (this.validateStep2()) {
        this.currentStep = 3;
        this.render();
        window.scrollTo({ top: 0, behavior: 'smooth' });
      }
    };
  }

  bindDeliveryEvents(container) {
    const cards = container.querySelectorAll('[data-delivery]');
    cards.forEach(card => {
      card.onclick = () => {
        cards.forEach(c => c.classList.remove('is-selected'));
        card.classList.add('is-selected');
        this.checkoutData.entrega.metodo = card.dataset.delivery;
        const extraContainer = document.getElementById('page-delivery-extra-fields');
        if (extraContainer) {
          extraContainer.innerHTML = this.getDeliveryFieldsHTML(this.checkoutData.entrega);
          this.bindDeliveryInputRestrictions();
        }
      };
    });
    this.bindDeliveryInputRestrictions();
  }

  bindDeliveryInputRestrictions() {
    const dniRetiroInput = document.getElementById('step2-retiro-dni');
    dniRetiroInput?.addEventListener('input', (ev) => {
      ev.target.value = ev.target.value.replace(/\D/g, '').slice(0, 8);
    });
  }

  getDeliveryFieldsHTML(e) {
    if (e.metodo === 'domicilio') {
      const dep = e.departamento || 'Ica';
      const city = e.ciudad || 'Ica';
      return `
        <div class="checkout-form-grid" style="margin-top: 14px;">
          <div class="checkout-form-group">
            <label class="checkout-label" for="step2-departamento">Departamento *</label>
            <select id="step2-departamento" class="checkout-select">
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
            <label class="checkout-label" for="step2-ciudad">Ciudad / Provincia / Distrito *</label>
            <input type="text" id="step2-ciudad" class="checkout-input" placeholder="Ej: Ica Centro / Chincha / Pisco" value="${city}">
            <span class="checkout-error-msg" id="err-step2-ciudad"></span>
          </div>

          <div class="checkout-form-group">
            <label class="checkout-label" for="step2-referencia">Referencia / Urbanización</label>
            <input type="text" id="step2-referencia" class="checkout-input" placeholder="Ej: Frente al parque / Dpto 402" value="${e.referencia || ''}">
          </div>

          <div class="checkout-form-group">
            <label class="checkout-label" for="step2-direccion">Dirección Exacta de Entrega *</label>
            <input type="text" id="step2-direccion" class="checkout-input" placeholder="Av. / Jr. / Calle y Número" value="${e.direccion || ''}">
            <span class="checkout-error-msg" id="err-step2-direccion"></span>
          </div>
        </div>
      `;
    } else {
      return `
        <div class="checkout-payment-box" style="margin-top: 14px;">
          <div>
            <strong style="color:#FFF; font-size:14px;">Sede Central RYA Tech - Centro Tecnológico Lima</strong>
            <p style="font-size:12px; color:var(--muted); margin-top:3px;">Av. Garcilaso de la Vega 1348, Piso 2, Tienda 204, Cercado de Lima.</p>
            <p style="font-size:12px; color:var(--color-cyan); margin-top:4px;">Horario de atención: Lun a Sáb de 9:00 a. m. a 7:00 p. m.</p>
          </div>
          
          <div class="checkout-form-grid" style="margin-top: 8px;">
            <div class="checkout-form-group">
              <label class="checkout-label" for="step2-retiro-nombre">Persona autorizada (Nombre y Apellidos)</label>
              <input type="text" id="step2-retiro-nombre" class="checkout-input" placeholder="Dejar en blanco si recoge el titular" value="${e.titularRetiroNombre || ''}">
              <span class="checkout-error-msg" id="err-step2-retiro-nombre"></span>
            </div>

            <div class="checkout-form-group">
              <label class="checkout-label" for="step2-retiro-dni">DNI de persona autorizada (8 dígitos)</label>
              <input type="text" id="step2-retiro-dni" class="checkout-input" placeholder="Ej: 72345678" maxlength="8" value="${e.titularRetiroDni || ''}">
              <span class="checkout-error-msg" id="err-step2-retiro-dni"></span>
            </div>
          </div>
        </div>
      `;
    }
  }

  validateStep2() {
    if (this.checkoutData.entrega.metodo === 'domicilio') {
      const departamento = document.getElementById('step2-departamento')?.value || 'Ica';
      const ciudad = document.getElementById('step2-ciudad')?.value.trim();
      const direccion = document.getElementById('step2-direccion')?.value.trim();
      const referencia = document.getElementById('step2-referencia')?.value.trim() || '';

      this.checkoutData.entrega.departamento = departamento;
      this.checkoutData.entrega.ciudad = ciudad;
      this.checkoutData.entrega.direccion = direccion;
      this.checkoutData.entrega.referencia = referencia;

      let isValid = true;
      const errCiudad = document.getElementById('err-step2-ciudad');
      const errDir = document.getElementById('err-step2-direccion');

      if (!ciudad || ciudad.length < 2) {
        if (errCiudad) errCiudad.textContent = 'Por favor ingresa la ciudad o distrito de entrega.';
        document.getElementById('step2-ciudad')?.classList.add('has-error');
        isValid = false;
      } else {
        if (errCiudad) errCiudad.textContent = '';
        document.getElementById('step2-ciudad')?.classList.remove('has-error');
      }

      if (!direccion || direccion.length < 5) {
        if (errDir) errDir.textContent = 'Por favor ingresa una dirección completa de entrega.';
        document.getElementById('step2-direccion')?.classList.add('has-error');
        isValid = false;
      } else {
        if (errDir) errDir.textContent = '';
        document.getElementById('step2-direccion')?.classList.remove('has-error');
      }

      return isValid;
    } else {
      const titularRetiroNombre = document.getElementById('step2-retiro-nombre')?.value.trim() || '';
      const titularRetiroDni = document.getElementById('step2-retiro-dni')?.value.trim() || '';
      
      this.checkoutData.entrega.titularRetiroNombre = titularRetiroNombre;
      this.checkoutData.entrega.titularRetiroDni = titularRetiroDni;
      this.checkoutData.entrega.direccion = 'Sede Central RYA Tech (Av. Garcilaso de la Vega 1348, Lima)';

      const errRetiroDni = document.getElementById('err-step2-retiro-dni');
      if (titularRetiroDni && !/^\d{8}$/.test(titularRetiroDni)) {
        if (errRetiroDni) errRetiroDni.textContent = 'El DNI de la persona autorizada debe tener 8 números.';
        document.getElementById('step2-retiro-dni')?.classList.add('has-error');
        return false;
      } else {
        if (errRetiroDni) errRetiroDni.textContent = '';
        document.getElementById('step2-retiro-dni')?.classList.remove('has-error');
        return true;
      }
    }
  }

  renderStep3(container, totals) {
    const p = this.checkoutData.pago;

    container.innerHTML = `
      <div>
        <h3 class="checkout-section-subtitle">3. Método de Pago (Simulado)</h3>
        <p style="font-size:13px; color:var(--muted);">Selecciona tu método de pago preferido.</p>
      </div>

      <div class="checkout-options-grid">
        <div class="checkout-option-card ${p.metodo === 'yape' ? 'is-selected' : ''}" data-payment="yape">
          <div class="option-card-header">
            <span class="option-card-title">Yape / Plin</span>
            <span class="option-card-badge">Instantáneo</span>
          </div>
          <p class="option-card-desc">Transferencia móvil rápida con QR o al número de celular.</p>
        </div>

        <div class="checkout-option-card ${p.metodo === 'tarjeta' ? 'is-selected' : ''}" data-payment="tarjeta">
          <div class="option-card-header">
            <span class="option-card-title">Tarjeta Débito / Crédito</span>
            <span class="option-card-badge">Seguro SSL</span>
          </div>
          <p class="option-card-desc">Aceptamos Visa, Mastercard, American Express o Diners.</p>
        </div>

        <div class="checkout-option-card ${p.metodo === 'transferencia' ? 'is-selected' : ''}" data-payment="transferencia" style="grid-column: span 2;">
          <div class="option-card-header">
            <span class="option-card-title">Transferencia Bancaria Directa</span>
            <span class="option-card-badge">BCP / BBVA</span>
          </div>
          <p class="option-card-desc">Cuentas corrientes empresariales a nombre de RYA TECH S.A.C.</p>
        </div>
      </div>

      <div id="page-payment-details-box">
        ${this.getPaymentFieldsHTML(p, totals)}
      </div>

      <div class="checkout-actions-bar">
        <button class="btn-checkout-back" id="btn-back-to-step2">← Volver a Entrega</button>
        <button class="btn-checkout-next btn-checkout-pay" id="btn-confirm-payment">
          Confirmar y Pagar ${totals.formattedTotal}
        </button>
      </div>
    `;

    this.bindPaymentEvents(container, totals);

    document.getElementById('btn-back-to-step2').onclick = () => {
      this.currentStep = 2;
      this.render();
      window.scrollTo({ top: 0, behavior: 'smooth' });
    };

    document.getElementById('btn-confirm-payment').onclick = async () => {
      if (this.validateStep3()) {
        const btn = document.getElementById('btn-confirm-payment');
        if (btn) {
          btn.disabled = true;
          btn.textContent = 'Procesando pedido...';
        }
        await this.processOrder(totals);
      }
    };
  }

  bindPaymentEvents(container, totals) {
    const cards = container.querySelectorAll('[data-payment]');
    cards.forEach(card => {
      card.onclick = () => {
        cards.forEach(c => c.classList.remove('is-selected'));
        card.classList.add('is-selected');
        this.checkoutData.pago.metodo = card.dataset.payment;
        const detailsContainer = document.getElementById('page-payment-details-box');
        if (detailsContainer) {
          detailsContainer.innerHTML = this.getPaymentFieldsHTML(this.checkoutData.pago, totals);
          this.bindPaymentInputRestrictions();
        }
      };
    });
    this.bindPaymentInputRestrictions();
  }

  bindPaymentInputRestrictions() {
    const metodo = this.checkoutData.pago.metodo;
    if (metodo === 'tarjeta') {
      const cardNumInput = document.getElementById('step3-card-number');
      const cardExpInput = document.getElementById('step3-card-exp');
      const cardCvvInput = document.getElementById('step3-card-cvv');

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
      
      const opInput = document.getElementById('step3-numero-op');
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
            <label class="checkout-label" for="step3-numero-op">Código de Operación Yape / Plin (Solo números) *</label>
            <input type="text" id="step3-numero-op" class="checkout-input" placeholder="Ej: 839201 (6 a 10 dígitos)" maxlength="10" value="${p.numeroOp || ''}">
            <span class="checkout-error-msg" id="err-step3-numero-op"></span>
          </div>
        </div>
      `;
    } else if (p.metodo === 'tarjeta') {
      return `
        <div class="checkout-payment-box">
          <div class="checkout-form-grid">
            <div class="checkout-form-group full-width">
              <label class="checkout-label" for="step3-card-number">Número de Tarjeta (16 dígitos) *</label>
              <input type="text" id="step3-card-number" class="checkout-input" placeholder="Ej: 4557889900123456" maxlength="16" value="${p.numTarjeta || ''}">
              <span class="checkout-error-msg" id="err-step3-card-number"></span>
            </div>
            <div class="checkout-form-group full-width">
              <label class="checkout-label" for="step3-card-name">Nombre impreso en la tarjeta *</label>
              <input type="text" id="step3-card-name" class="checkout-input" placeholder="JUAN PEREZ M" value="${p.titularTarjeta || ''}">
              <span class="checkout-error-msg" id="err-step3-card-name"></span>
            </div>
            <div class="checkout-form-group">
              <label class="checkout-label" for="step3-card-exp">Vencimiento (MM/AA) *</label>
              <input type="text" id="step3-card-exp" class="checkout-input" placeholder="12/26" maxlength="5" value="${p.expiracion || ''}">
              <span class="checkout-error-msg" id="err-step3-card-exp"></span>
            </div>
            <div class="checkout-form-group">
              <label class="checkout-label" for="step3-card-cvv">CVV (3 dígitos) *</label>
              <input type="password" id="step3-card-cvv" class="checkout-input" placeholder="123" maxlength="3" value="${p.cvv || ''}">
              <span class="checkout-error-msg" id="err-step3-card-cvv"></span>
            </div>
          </div>
        </div>
      `;
    } else {
      return `
        <div class="checkout-payment-box">
          <div style="font-size:12px; line-height:1.6; color:var(--muted);">
            <p><strong style="color:#FFF;">Cuenta Corriente BCP Soles:</strong> 191-98765432-0-12</p>
            <p><strong style="color:#FFF;">Código Interbancario (CCI):</strong> 00219100987654320124</p>
            <p><strong style="color:#FFF;">Cuenta Corriente BBVA Soles:</strong> 0011-0123-0100045678</p>
            <p>Beneficiario: <strong>RYA TECH S.A.C.</strong> | RUC: 20608945123</p>
          </div>
          <div class="checkout-form-group" style="margin-top:6px;">
            <label class="checkout-label" for="step3-numero-op">Código de Operación Bancaria (Solo números) *</label>
            <input type="text" id="step3-numero-op" class="checkout-input" placeholder="Ej: 4920192 (6 a 10 dígitos)" maxlength="10" value="${p.numeroOp || ''}">
            <span class="checkout-error-msg" id="err-step3-numero-op"></span>
          </div>
        </div>
      `;
    }
  }

  validateStep3() {
    const metodo = this.checkoutData.pago.metodo;

    if (metodo === 'tarjeta') {
      const num = document.getElementById('step3-card-number')?.value.trim();
      const name = document.getElementById('step3-card-name')?.value.trim();
      const exp = document.getElementById('step3-card-exp')?.value.trim();
      const cvv = document.getElementById('step3-card-cvv')?.value.trim();

      this.checkoutData.pago.numTarjeta = num;
      this.checkoutData.pago.titularTarjeta = name;
      this.checkoutData.pago.expiracion = exp;
      this.checkoutData.pago.cvv = cvv;

      let isValid = true;
      if (!num || !/^\d{16}$/.test(num)) {
        document.getElementById('err-step3-card-number').textContent = 'El número de tarjeta debe tener exactamente 16 números.';
        document.getElementById('step3-card-number')?.classList.add('has-error');
        isValid = false;
      } else {
        document.getElementById('err-step3-card-number').textContent = '';
        document.getElementById('step3-card-number')?.classList.remove('has-error');
      }

      if (!name || name.length < 3) {
        document.getElementById('err-step3-card-name').textContent = 'Ingresa el nombre del titular impreso en la tarjeta.';
        document.getElementById('step3-card-name')?.classList.add('has-error');
        isValid = false;
      } else {
        document.getElementById('err-step3-card-name').textContent = '';
        document.getElementById('step3-card-name')?.classList.remove('has-error');
      }

      if (!exp || !/^(0[1-9]|1[0-2])\/\d{2}$/.test(exp)) {
        document.getElementById('err-step3-card-exp').textContent = 'Ingresa una fecha de vencimiento válida (MM/AA, ej: 12/26).';
        document.getElementById('step3-card-exp')?.classList.add('has-error');
        isValid = false;
      } else {
        document.getElementById('err-step3-card-exp').textContent = '';
        document.getElementById('step3-card-exp')?.classList.remove('has-error');
      }

      if (!cvv || !/^\d{3}$/.test(cvv)) {
        document.getElementById('err-step3-card-cvv').textContent = 'El CVV debe tener exactamente 3 números.';
        document.getElementById('step3-card-cvv')?.classList.add('has-error');
        isValid = false;
      } else {
        document.getElementById('err-step3-card-cvv').textContent = '';
        document.getElementById('step3-card-cvv')?.classList.remove('has-error');
      }

      return isValid;
    } else {
      const op = document.getElementById('step3-numero-op')?.value.trim();
      this.checkoutData.pago.numeroOp = op;

      if (!op || !/^\d{6,10}$/.test(op)) {
        document.getElementById('err-step3-numero-op').textContent = 'El código de operación debe contener entre 6 y 10 dígitos numéricos.';
        document.getElementById('step3-numero-op')?.classList.add('has-error');
        return false;
      } else {
        document.getElementById('err-step3-numero-op').textContent = '';
        document.getElementById('step3-numero-op')?.classList.remove('has-error');
        return true;
      }
    }
  }

  async processOrder(totals) {
    let orderUUID = generateUUID();
    let orderNumber = `PED-2026-${String(Math.floor(1000 + Math.random() * 9000))}`;
    let userUUID = generateUUID();
    const now = new Date().toISOString();
    const cart = Cart.getCart();

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

    Cart.saveCart([]);

    this.currentStep = 4;
    this.render();
    window.scrollTo({ top: 0, behavior: 'smooth' });

    Cart.showToast({
      title: '¡Orden Confirmada!',
      message: `Tu pedido ${orderNumber} ha sido procesado con éxito.`,
      duration: 5000
    });
  }

  renderStep4Confirmation() {
    const o = this.orderResult;
    if (!o) return;

    this.rootEl.innerHTML = `
      <div class="checkout-main-card" style="max-width: 760px; margin: 0 auto;">
        <div class="order-success-container">
          <div class="order-success-icon">
            <svg width="38" height="38" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="20 6 9 17 4 12"></polyline>
            </svg>
          </div>

          <div>
            <h2 class="order-success-title">¡Pedido Registrado con Éxito!</h2>
            <p style="font-size:14px; color:var(--muted); margin-top:6px;">
              Hemos generado tu orden y enviado el comprobante a <strong>${o.usuarioEmail}</strong>.
            </p>
          </div>

          <div class="order-success-order-id">
            <span>ORDEN: ${o.codigoOrden}</span>
          </div>

          <div class="order-receipt-voucher">
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
                <span style="font-size:12px;">${o.metodoEntrega}</span>
              </div>
            </div>

            <div style="font-weight:700; font-size:13px; color:#FFF; margin-top:6px;">Detalle de Productos:</div>
            <div class="receipt-items-list">
              ${o.detalles.map(d => `
                <div class="receipt-item-row">
                  <div>
                    <strong style="color:var(--color-cyan);">${d.cantidad}x</strong>
                    <span style="color:var(--muted);">${d.fabricante} ${d.categoria}</span>
                  </div>
                  <span style="font-weight:600;">${formatCurrency(d.subtotal)}</span>
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
            <button class="btn-voucher-print" id="btn-page-print">
              Imprimir Resumen
            </button>
            <a class="btn-voucher-whatsapp" id="btn-page-whatsapp" target="_blank" rel="noopener">
              Asesor WhatsApp
            </a>
            <a href="index.html" class="btn-voucher-continue">
              Seguir Comprando
            </a>
          </div>
        </div>
      </div>
    `;

    const waText = encodeURIComponent(
      `¡Hola RYA Tech! Acabo de registrar mi pedido *${o.codigoOrden}* por un total de *${o.formattedTotal}* a nombre de *${o.usuarioNombre}*. Deseo consultar el estado de mi despacho.`
    );
    const waBtn = document.getElementById('btn-page-whatsapp');
    if (waBtn) waBtn.href = `https://wa.me/51987654321?text=${waText}`;

    const printBtn = document.getElementById('btn-page-print');
    if (printBtn) printBtn.onclick = () => window.print();
  }
}

const CheckoutController = new CheckoutPageController();

function inicializarAppCheckout() {
  if (typeof inicializarMonitorDeRed === 'function') inicializarMonitorDeRed();
  if (window.Cart && window.Cart.init) window.Cart.init();
  
  const searchInput = document.getElementById('headerSearchInput');
  searchInput?.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && searchInput.value.trim()) {
      window.location.href = `index.html?search=${encodeURIComponent(searchInput.value.trim())}`;
    }
  });

  CheckoutController.init();
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', inicializarAppCheckout);
} else {
  inicializarAppCheckout();
}

if (typeof window !== 'undefined') {
  window.CheckoutController = CheckoutController;
}

