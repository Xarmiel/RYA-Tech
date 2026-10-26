-- ============================================================================
-- RYA-Tech: Estructura de Pedidos y Transacciones de Pago (Mercado Pago)
-- Esquema: rya
-- ============================================================================

-- 1. Métodos de pago Mercado Pago
INSERT INTO rya.metodos_pago (id, codigo, nombre, activo, is_deleted)
VALUES 
    (gen_random_uuid(), 'MP_CARDS', 'Tarjeta de Débito / Crédito (Mercado Pago)', true, false),
    (gen_random_uuid(), 'MP_YAPE', 'Yape / Billetera Digital (Mercado Pago)', true, false),
    (gen_random_uuid(), 'MP_PAGOEFECTIVO', 'PagoEfectivo (Mercado Pago)', true, false)
ON CONFLICT (codigo) DO UPDATE 
SET activo = true, is_deleted = false;

-- 2. Tabla de Pedidos
CREATE TABLE IF NOT EXISTS rya.pedidos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    numero_pedido VARCHAR(32) NOT NULL UNIQUE,
    usuario_id UUID REFERENCES rya.usuarios(id) ON DELETE SET NULL,
    session_key VARCHAR(128),
    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE_PAGO', -- PENDIENTE_PAGO, PAGADO, CANCELADO, FALLIDO, EXPIRADO
    moneda VARCHAR(3) NOT NULL DEFAULT 'PEN',
    subtotal NUMERIC(12, 2) NOT NULL,
    costo_envio NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    total NUMERIC(12, 2) NOT NULL,
    
    -- Datos del cliente / comprobante
    tipo_documento VARCHAR(10) NOT NULL,
    numero_documento VARCHAR(20) NOT NULL,
    nombre_completo VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    telefono VARCHAR(20) NOT NULL,
    
    -- Datos de entrega
    tarifa_envio_id UUID REFERENCES rya.tarifas_envio(id),
    direccion_envio TEXT NOT NULL,
    referencia_envio TEXT,
    
    -- Referencias de Mercado Pago
    preference_id VARCHAR(128),
    
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 3. Tabla de Items del Pedido
CREATE TABLE IF NOT EXISTS rya.pedido_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pedido_id UUID NOT NULL REFERENCES rya.pedidos(id) ON DELETE CASCADE,
    producto_id UUID NOT NULL REFERENCES rya.productos(id),
    sku VARCHAR(64) NOT NULL,
    nombre_producto VARCHAR(255) NOT NULL,
    precio_unitario NUMERIC(12, 2) NOT NULL,
    cantidad INT NOT NULL,
    subtotal NUMERIC(12, 2) NOT NULL
);

-- 4. Tabla de Transacciones / Pagos
CREATE TABLE IF NOT EXISTS rya.pagos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pedido_id UUID NOT NULL REFERENCES rya.pedidos(id) ON DELETE CASCADE,
    pasarela VARCHAR(30) NOT NULL DEFAULT 'MERCADO_PAGO',
    mp_payment_id VARCHAR(64),
    payment_method_id VARCHAR(64), -- visa, master, yape, etc.
    payment_type_id VARCHAR(30),   -- credit_card, debit_card, digital_currency
    ultimos_digitos VARCHAR(4),
    monto NUMERIC(12, 2) NOT NULL,
    moneda VARCHAR(3) NOT NULL DEFAULT 'PEN',
    estado VARCHAR(30) NOT NULL,   -- approved, in_process, rejected
    status_detail VARCHAR(100),
    mensaje_respuesta TEXT,
    raw_response JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pedidos_usuario_id ON rya.pedidos(usuario_id);
CREATE INDEX IF NOT EXISTS idx_pedidos_numero_pedido ON rya.pedidos(numero_pedido);
CREATE INDEX IF NOT EXISTS idx_pedidos_mp_preference_id ON rya.pedidos(preference_id);
CREATE INDEX IF NOT EXISTS idx_pagos_pedido_id ON rya.pagos(pedido_id);
CREATE INDEX IF NOT EXISTS idx_pagos_mp_payment_id ON rya.pagos(mp_payment_id);
