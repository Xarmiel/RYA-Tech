CREATE TABLE IF NOT EXISTS rya.borradores_tecnicos (
 id uuid PRIMARY KEY, version bigint NOT NULL, created_by uuid NOT NULL, updated_by uuid NOT NULL,
 created_at timestamp with time zone NOT NULL, updated_at timestamp with time zone NOT NULL,
 sku varchar(80) NOT NULL, nombre varchar(220) NOT NULL, tipo varchar(30) NOT NULL,
 estado varchar(20) NOT NULL, document_json text NOT NULL
);
CREATE TABLE IF NOT EXISTS rya.borradores_tecnicos_eventos (
 id uuid PRIMARY KEY, borrador_id uuid NOT NULL REFERENCES rya.borradores_tecnicos(id), version bigint NOT NULL,
 actor uuid NOT NULL, accion varchar(30) NOT NULL, estado varchar(20) NOT NULL,
 comentario varchar(2000) NOT NULL, created_at timestamp with time zone NOT NULL, document_json text NOT NULL,
 UNIQUE (borrador_id,version)
);
