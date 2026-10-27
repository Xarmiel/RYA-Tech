-- Aplicar tras envios-y-recojo.sql. No agrega datos de demostración.
BEGIN;
CREATE TABLE IF NOT EXISTS rya.historial_administrativo (
 id uuid PRIMARY KEY,actor_id uuid NOT NULL,modulo varchar(40) NOT NULL,registro varchar(100) NOT NULL,
 accion varchar(40) NOT NULL,motivo text NOT NULL,created_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE IF NOT EXISTS rya.solicitudes_postventa (
 id uuid PRIMARY KEY,pedido_id uuid NOT NULL REFERENCES rya.pedidos(id),tipo varchar(30) NOT NULL,
 motivo text NOT NULL,evidencia_url text,estado varchar(30) NOT NULL DEFAULT 'ABIERTA',resolucion text,
 creado_por uuid NOT NULL,version bigint NOT NULL DEFAULT 0,created_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE IF NOT EXISTS rya.contenido_editorial (
 id uuid PRIMARY KEY,tipo varchar(30) NOT NULL,titulo varchar(200) NOT NULL,texto text NOT NULL,
 ubicacion varchar(30) NOT NULL,enlace text,imagen_url text,posicion integer NOT NULL DEFAULT 0,
 publicada boolean NOT NULL DEFAULT false,version bigint NOT NULL DEFAULT 0,created_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE IF NOT EXISTS rya.contacto_tienda (
 id integer PRIMARY KEY,email varchar(254),telefono varchar(30),whatsapp varchar(20),horario varchar(200),version bigint NOT NULL DEFAULT 0);
ALTER TABLE rya.preguntas_frecuentes ADD COLUMN IF NOT EXISTS revision_admin bigint NOT NULL DEFAULT 0;
ALTER TABLE rya.preguntas_frecuentes ADD COLUMN IF NOT EXISTS created_admin_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE rya.resenas_producto ADD COLUMN IF NOT EXISTS estado_moderacion varchar(30) NOT NULL DEFAULT 'PENDIENTE';
ALTER TABLE rya.resenas_producto ADD COLUMN IF NOT EXISTS motivo_moderacion text;
ALTER TABLE rya.resenas_producto ADD COLUMN IF NOT EXISTS moderado_por uuid;
ALTER TABLE rya.resenas_producto ADD COLUMN IF NOT EXISTS moderado_at timestamp with time zone;
ALTER TABLE rya.resenas_producto ADD COLUMN IF NOT EXISTS revision_admin bigint NOT NULL DEFAULT 0;
CREATE INDEX IF NOT EXISTS idx_postventa_pedido ON rya.solicitudes_postventa(pedido_id);
CREATE INDEX IF NOT EXISTS idx_historial_registro ON rya.historial_administrativo(modulo,registro);

INSERT INTO rya.contacto_tienda(id) VALUES (1) ON CONFLICT (id) DO NOTHING;
UPDATE rya.resenas_producto SET estado_moderacion='APROBADA' WHERE publicada=true AND estado_moderacion='PENDIENTE' AND moderado_at IS NULL;
ALTER TABLE rya.historial_administrativo ENABLE ROW LEVEL SECURITY;
ALTER TABLE rya.solicitudes_postventa ENABLE ROW LEVEL SECURITY;
ALTER TABLE rya.contenido_editorial ENABLE ROW LEVEL SECURITY;
ALTER TABLE rya.contacto_tienda ENABLE ROW LEVEL SECURITY;
COMMIT;
