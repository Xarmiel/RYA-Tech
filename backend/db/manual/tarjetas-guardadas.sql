-- Aplicar antes de arrancar/desplegar el backend. No guarda PAN, CVV ni tokens.
BEGIN;
CREATE TABLE IF NOT EXISTS rya.clientes_pago (
 id uuid PRIMARY KEY,
 usuario_id uuid NOT NULL,
 entorno varchar(80) NOT NULL,
 customer_id varchar(100),
 CONSTRAINT ux_clientes_pago_usuario_entorno UNIQUE(usuario_id,entorno)
);
ALTER TABLE rya.clientes_pago ENABLE ROW LEVEL SECURITY;
COMMIT;
