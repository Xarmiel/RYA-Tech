-- Aplicar antes de desplegar el backend. No agrega tarifas ni datos de ejemplo.
BEGIN;
ALTER TABLE rya.tarifas_envio ALTER COLUMN transportista_id DROP NOT NULL;
ALTER TABLE rya.tarifas_envio ADD COLUMN IF NOT EXISTS activo boolean NOT NULL DEFAULT true;
ALTER TABLE rya.tarifas_envio ADD COLUMN IF NOT EXISTS modalidad varchar(20) NOT NULL DEFAULT 'DOMICILIO';
ALTER TABLE rya.tarifas_envio ADD COLUMN IF NOT EXISTS nombre_servicio varchar(160);
ALTER TABLE rya.tarifas_envio ADD COLUMN IF NOT EXISTS direccion_recojo varchar(300);
ALTER TABLE rya.tarifas_envio ADD COLUMN IF NOT EXISTS horario_recojo varchar(200);
UPDATE rya.tarifas_envio t SET nombre_servicio=tr.nombre FROM rya.transportistas tr WHERE t.transportista_id=tr.id AND t.nombre_servicio IS NULL;
ALTER TABLE rya.pedidos ADD COLUMN IF NOT EXISTS modalidad_entrega varchar(20);
ALTER TABLE rya.pedidos ADD COLUMN IF NOT EXISTS entrega_snapshot text;
ALTER TABLE rya.pedidos ADD COLUMN IF NOT EXISTS estado_entrega varchar(30);
ALTER TABLE rya.pedidos ADD COLUMN IF NOT EXISTS transportista_seguimiento varchar(160);
ALTER TABLE rya.pedidos ADD COLUMN IF NOT EXISTS codigo_seguimiento varchar(100);
CREATE INDEX IF NOT EXISTS idx_pedidos_entrega ON rya.pedidos(estado_entrega) WHERE is_deleted=false;
DO $$
BEGIN
 IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='ck_tarifa_modalidad_v1' AND conrelid='rya.tarifas_envio'::regclass) THEN
   ALTER TABLE rya.tarifas_envio ADD CONSTRAINT ck_tarifa_modalidad_v1 CHECK (modalidad IN ('DOMICILIO','RECOJO')) NOT VALID;
   ALTER TABLE rya.tarifas_envio ADD CONSTRAINT ck_tarifa_costo_v1 CHECK (costo>=0 AND (modalidad<>'RECOJO' OR costo=0)) NOT VALID;
   ALTER TABLE rya.tarifas_envio ADD CONSTRAINT ck_tarifa_recojo_v1 CHECK (modalidad<>'RECOJO' OR (NULLIF(btrim(direccion_recojo),'') IS NOT NULL AND NULLIF(btrim(horario_recojo),'') IS NOT NULL)) NOT VALID;
 END IF;
END $$;
COMMIT;
