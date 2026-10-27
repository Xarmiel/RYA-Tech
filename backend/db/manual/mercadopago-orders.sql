-- Orders API. Aplicar manualmente en Supabase antes de desplegar el backend.
-- No contiene credenciales y no se ejecuta automáticamente.
BEGIN;
INSERT INTO rya.metodos_pago(id,codigo,nombre,activo,is_deleted) VALUES
 (gen_random_uuid(),'MP_CARDS','Tarjeta de Débito / Crédito (Mercado Pago)',true,false),
 (gen_random_uuid(),'MP_YAPE','Yape (Mercado Pago)',true,false),
 (gen_random_uuid(),'MP_PAGOEFECTIVO','PagoEfectivo (Mercado Pago)',true,false)
ON CONFLICT(codigo) DO NOTHING;
-- El proyecto ya usa pedidos.codigo/envio/monto_total y detalle_pedido para Mis compras.
-- Agrega los datos de checkout que faltaban al contrato anterior, sin borrar pedidos.
ALTER TABLE rya.pedidos
  ADD COLUMN IF NOT EXISTS codigo varchar(35),
  ADD COLUMN IF NOT EXISTS envio numeric(14,2) NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS monto_total numeric(14,2),
  ADD COLUMN IF NOT EXISTS descuento numeric(14,2) NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS is_deleted boolean NOT NULL DEFAULT false,
  ADD COLUMN IF NOT EXISTS session_key varchar(128),
  ADD COLUMN IF NOT EXISTS tipo_documento varchar(10),
  ADD COLUMN IF NOT EXISTS numero_documento varchar(20),
  ADD COLUMN IF NOT EXISTS nombre_completo varchar(255),
  ADD COLUMN IF NOT EXISTS email varchar(255),
  ADD COLUMN IF NOT EXISTS telefono varchar(20),
  ADD COLUMN IF NOT EXISTS tarifa_envio_id uuid,
  ADD COLUMN IF NOT EXISTS direccion_envio text,
  ADD COLUMN IF NOT EXISTS referencia_envio text,
  ADD COLUMN IF NOT EXISTS preference_id varchar(128),
  ADD COLUMN IF NOT EXISTS metodo_pago_codigo varchar(40),
  ADD COLUMN IF NOT EXISTS metodo_pago_snapshot varchar(65),
  ADD COLUMN IF NOT EXISTS updated_at timestamptz NOT NULL DEFAULT now();

-- Compatibilidad si se aplicó primero el script antiguo con nombres alternativos.
DO $$ BEGIN
  IF EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema='rya' AND table_name='pedidos' AND column_name='numero_pedido') THEN
    EXECUTE 'UPDATE rya.pedidos SET codigo=numero_pedido WHERE codigo IS NULL';
    EXECUTE 'ALTER TABLE rya.pedidos ALTER COLUMN numero_pedido DROP NOT NULL';
  END IF;
  IF EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema='rya' AND table_name='pedidos' AND column_name='total') THEN
    EXECUTE 'UPDATE rya.pedidos SET monto_total=total WHERE monto_total IS NULL';
    EXECUTE 'ALTER TABLE rya.pedidos ALTER COLUMN total DROP NOT NULL';
  END IF;
  IF EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema='rya' AND table_name='pedidos' AND column_name='costo_envio') THEN
    EXECUTE 'UPDATE rya.pedidos SET envio=costo_envio WHERE envio=0 AND costo_envio IS NOT NULL';
  END IF;
END $$;
CREATE UNIQUE INDEX IF NOT EXISTS ux_pedidos_codigo_orders ON rya.pedidos(codigo);

CREATE TABLE IF NOT EXISTS rya.detalle_pedido (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(),pedido_id uuid NOT NULL REFERENCES rya.pedidos(id),producto_id uuid NOT NULL REFERENCES rya.productos(id),
 descripcion_snapshot varchar(260) NOT NULL,sku_snapshot varchar(85),cantidad integer NOT NULL,
 precio_unitario numeric(14,2) NOT NULL,descuento_unitario numeric(14,2) NOT NULL DEFAULT 0,
 subtotal numeric(14,2) GENERATED ALWAYS AS (cantidad*(precio_unitario-descuento_unitario)) STORED,
 created_at timestamptz NOT NULL DEFAULT now(),is_deleted boolean NOT NULL DEFAULT false
);
-- Conserva el historial si el checkout anterior creó ítems en una tabla separada.
DO $$ BEGIN
 IF to_regclass('rya.pedido_items') IS NOT NULL THEN
   EXECUTE 'INSERT INTO rya.detalle_pedido(id,pedido_id,producto_id,descripcion_snapshot,sku_snapshot,cantidad,precio_unitario)
     SELECT i.id,i.pedido_id,i.producto_id,i.nombre_producto,i.sku,i.cantidad,i.precio_unitario FROM rya.pedido_items i
     WHERE NOT EXISTS(SELECT 1 FROM rya.detalle_pedido d WHERE d.id=i.id)';
 END IF;
END $$;
CREATE TABLE IF NOT EXISTS rya.pagos (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(),pedido_id uuid NOT NULL REFERENCES rya.pedidos(id),pasarela varchar(30) NOT NULL DEFAULT 'MERCADO_PAGO_ORDERS',
 mp_payment_id varchar(64),payment_method_id varchar(64),payment_type_id varchar(30),ultimos_digitos varchar(4),
 monto numeric(12,2) NOT NULL,moneda varchar(3) NOT NULL DEFAULT 'PEN',estado varchar(30) NOT NULL,status_detail varchar(100),
 codigo_respuesta varchar(100),mensaje_respuesta text,raw_response jsonb,created_at timestamptz NOT NULL DEFAULT now()
);
ALTER TABLE rya.pagos ADD COLUMN IF NOT EXISTS mp_order_id varchar(64),ADD COLUMN IF NOT EXISTS instrucciones_url text,ADD COLUMN IF NOT EXISTS referencia_pago varchar(100);
CREATE UNIQUE INDEX IF NOT EXISTS ux_pagos_mp_order_id ON rya.pagos(mp_order_id) WHERE mp_order_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_pagos_pedido_id ON rya.pagos(pedido_id);
COMMIT;
