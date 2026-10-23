-- MANUAL ONLY. Review and approve before applying to a remote database.
-- No automatic migration runner references this file. No product or inventory tables are changed.
BEGIN;
CREATE TABLE rya.borradores_tecnicos (
  id uuid PRIMARY KEY,
  version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
  created_by uuid NOT NULL,
  updated_by uuid NOT NULL,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  sku varchar(80) NOT NULL,
  nombre varchar(220) NOT NULL,
  tipo varchar(30) NOT NULL CHECK (tipo IN ('cpu','board','ram','gpu','cooling','psu','case','storage','peripheral','other')),
  estado varchar(20) NOT NULL CHECK (estado IN ('DRAFT','IN_REVIEW','APPROVED','REJECTED')),
  document_json text NOT NULL CHECK (octet_length(document_json) <= 131072 AND jsonb_typeof(document_json::jsonb) = 'object')
);
CREATE INDEX ix_borradores_tecnicos_estado_fecha ON rya.borradores_tecnicos(estado,updated_at DESC,id DESC);
CREATE INDEX ix_borradores_tecnicos_autor ON rya.borradores_tecnicos(created_by);
CREATE TABLE rya.borradores_tecnicos_eventos (
  id uuid PRIMARY KEY,
  borrador_id uuid NOT NULL REFERENCES rya.borradores_tecnicos(id) ON DELETE RESTRICT,
  version bigint NOT NULL,
  actor uuid NOT NULL,
  accion varchar(30) NOT NULL CHECK (accion IN ('CREATED','UPDATED','SUBMITTED','REVIEWED','REOPENED')),
  estado varchar(20) NOT NULL CHECK (estado IN ('DRAFT','IN_REVIEW','APPROVED','REJECTED')),
  comentario varchar(2000) NOT NULL,
  document_json text NOT NULL CHECK (octet_length(document_json) <= 131072 AND jsonb_typeof(document_json::jsonb) = 'object'),
  created_at timestamptz NOT NULL,
  UNIQUE (borrador_id,version)
);
-- No direct access from browser clients, including authenticated Supabase users.
ALTER TABLE rya.borradores_tecnicos ENABLE ROW LEVEL SECURITY;
ALTER TABLE rya.borradores_tecnicos_eventos ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON rya.borradores_tecnicos, rya.borradores_tecnicos_eventos FROM PUBLIC,anon,authenticated;
CREATE FUNCTION rya.proteger_eventos_borrador_tecnico() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  RAISE EXCEPTION 'El historial de borradores técnicos es de solo anexado';
END;
$$;
CREATE TRIGGER proteger_eventos_borrador_tecnico BEFORE UPDATE OR DELETE ON rya.borradores_tecnicos_eventos
  FOR EACH ROW EXECUTE FUNCTION rya.proteger_eventos_borrador_tecnico();
COMMENT ON TABLE rya.borradores_tecnicos IS 'Unpublished technical drafts; approval does not publish or certify compatibility';
-- Author identities are signed JWT subjects. They need not already exist in the public user profile table.
COMMIT;
-- Operator must grant the backend SQL role only the required SELECT/INSERT/UPDATE permissions
-- on drafts and SELECT/INSERT on events. Never grant UPDATE/DELETE on the event history.
-- Enable app.technical-drafts.enabled=true only after the approved migration and role checks.
