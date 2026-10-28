-- Aplicar antes de iniciar/desplegar el backend con campos personalizados.
BEGIN;
CREATE TABLE IF NOT EXISTS rya.campos_ficha_categoria (
 id uuid PRIMARY KEY,categoria_id uuid NOT NULL REFERENCES rya.categorias(id),
 clave varchar(40) NOT NULL UNIQUE,nombre varchar(100) NOT NULL,tipo_dato varchar(10) NOT NULL,
 unidad varchar(20),ejemplo varchar(120),solo_enteros boolean NOT NULL DEFAULT false,
 version bigint NOT NULL DEFAULT 0,creado_por uuid NOT NULL,actualizado_por uuid NOT NULL,
 created_at timestamp with time zone NOT NULL,updated_at timestamp with time zone NOT NULL,
 CONSTRAINT ck_campo_ficha_tipo CHECK(tipo_dato IN ('text','number','boolean')),
 CONSTRAINT ck_campo_ficha_numero CHECK(tipo_dato='number' OR (solo_enteros=false AND unidad IS NULL))
);
CREATE INDEX IF NOT EXISTS idx_campo_ficha_categoria ON rya.campos_ficha_categoria(categoria_id);
CREATE UNIQUE INDEX IF NOT EXISTS ux_campo_ficha_nombre ON rya.campos_ficha_categoria(categoria_id,lower(nombre));
ALTER TABLE rya.campos_ficha_categoria ENABLE ROW LEVEL SECURITY;
COMMIT;
