-- PENDIENTE DE APROBACION: no se ha aplicado a la BD remota.
-- Solo restaura valores CATALOG_TECH_*; no restaura productos, marcas o categorias.
-- Mantiene rya.auditar_fila() intacta para todas las demas tablas.
-- Ejecutar como propietario de las tablas en una sola transaccion.
BEGIN;

CREATE TABLE IF NOT EXISTS rya.restauraciones_atributo_catalogo (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    valor_id uuid NOT NULL REFERENCES rya.valores_atributo_producto(id),
    producto_id uuid NOT NULL REFERENCES rya.productos(id),
    actor_id uuid NOT NULL,
    motivo varchar(500) NOT NULL,
    antes jsonb NOT NULL,
    despues jsonb,
    solicitado_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    restaurado_at timestamptz,
    backend_pid integer NOT NULL,
    transaction_id bigint NOT NULL,
    consumido boolean NOT NULL DEFAULT false
);
ALTER TABLE rya.restauraciones_atributo_catalogo ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON TABLE rya.restauraciones_atributo_catalogo FROM PUBLIC, anon, authenticated;

CREATE OR REPLACE FUNCTION rya.auditar_atributo_catalogo()
RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER SET search_path = pg_catalog
AS $function$
DECLARE
    v_actor uuid := rya.actor_actual();
    v_evento rya.restauraciones_atributo_catalogo%ROWTYPE;
BEGIN
    IF TG_OP = 'INSERT' THEN
        NEW.created_at := coalesce(NEW.created_at, now());
        NEW.updated_at := coalesce(NEW.updated_at, NEW.created_at);
        IF v_actor IS NOT NULL THEN NEW.created_by := v_actor; NEW.updated_by := v_actor; END IF;
    ELSIF TG_OP = 'UPDATE' THEN
        NEW.created_at := OLD.created_at;
        NEW.created_by := OLD.created_by;
        NEW.updated_at := clock_timestamp();
        IF v_actor IS NOT NULL THEN NEW.updated_by := v_actor; END IF;
        IF NOT OLD.is_deleted AND NEW.is_deleted THEN
            NEW.deleted_at := coalesce(NEW.deleted_at, clock_timestamp());
            IF v_actor IS NOT NULL THEN NEW.deleted_by := v_actor; END IF;
        ELSIF OLD.is_deleted AND NOT NEW.is_deleted THEN
            -- An arbitrary UPDATE or session variable cannot authorize restoration.
            -- Only a private, unconsumed event created by the server function below can.
            SELECT * INTO v_evento FROM rya.restauraciones_atributo_catalogo
            WHERE valor_id = OLD.id AND backend_pid = pg_backend_pid()
              AND transaction_id = txid_current() AND NOT consumido
            ORDER BY solicitado_at DESC LIMIT 1 FOR UPDATE;
            IF NOT FOUND THEN
                RAISE EXCEPTION 'No se puede reactivar un registro eliminado sin proceso de restauración';
            END IF;
            IF NEW.producto_id IS DISTINCT FROM OLD.producto_id OR NEW.atributo_id IS DISTINCT FROM OLD.atributo_id THEN
                RAISE EXCEPTION 'La restauración no puede cambiar el producto ni el atributo';
            END IF;
            NEW.deleted_at := NULL;
            NEW.deleted_by := NULL;
            NEW.updated_by := v_evento.actor_id;
            UPDATE rya.restauraciones_atributo_catalogo
               SET consumido = true, restaurado_at = clock_timestamp(), despues = to_jsonb(NEW)
             WHERE id = v_evento.id;
        END IF;
    END IF;
    RETURN NEW;
END;
$function$;
REVOKE ALL ON FUNCTION rya.auditar_atributo_catalogo() FROM PUBLIC, anon, authenticated;

CREATE OR REPLACE FUNCTION rya.restaurar_atributo_catalogo(p_valor_id uuid, p_actor_id uuid, p_motivo text)
RETURNS boolean LANGUAGE plpgsql SECURITY DEFINER SET search_path = pg_catalog
AS $function$
DECLARE
    v_fila rya.valores_atributo_producto%ROWTYPE;
BEGIN
    IF p_actor_id IS NULL OR NOT EXISTS (
        SELECT 1 FROM auth.users u WHERE u.id = p_actor_id AND u.raw_app_meta_data ->> 'role' = 'admin'
    ) THEN
        RAISE EXCEPTION 'Se requiere un administrador vigente para restaurar campos' USING ERRCODE = '42501';
    END IF;
    IF p_motivo IS NULL OR length(btrim(p_motivo)) = 0 OR length(p_motivo) > 500 THEN
        RAISE EXCEPTION 'La restauración requiere un motivo válido';
    END IF;
    SELECT * INTO v_fila FROM rya.valores_atributo_producto WHERE id = p_valor_id FOR UPDATE;
    IF NOT FOUND THEN RAISE EXCEPTION 'No existe el valor técnico solicitado'; END IF;
    IF NOT EXISTS (
        SELECT 1 FROM rya.atributos_tecnicos a JOIN rya.productos p ON p.id = v_fila.producto_id
        WHERE a.id = v_fila.atributo_id AND left(a.codigo, 13) = 'CATALOG_TECH_'
          AND NOT a.is_deleted AND NOT p.is_deleted
    ) THEN RAISE EXCEPTION 'Solo se pueden restaurar campos comerciales de productos vigentes'; END IF;
    IF NOT v_fila.is_deleted THEN RETURN true; END IF;
    INSERT INTO rya.restauraciones_atributo_catalogo(valor_id, producto_id, actor_id, motivo, antes, backend_pid, transaction_id)
    VALUES (v_fila.id, v_fila.producto_id, p_actor_id, btrim(p_motivo), to_jsonb(v_fila), pg_backend_pid(), txid_current());
    UPDATE rya.valores_atributo_producto SET is_deleted = false WHERE id = v_fila.id;
    RETURN true;
END;
$function$;
-- Not callable from the browser / PostgREST. Only the trusted JDBC backend role.
REVOKE ALL ON FUNCTION rya.restaurar_atributo_catalogo(uuid,uuid,text) FROM PUBLIC, anon, authenticated;
GRANT EXECUTE ON FUNCTION rya.restaurar_atributo_catalogo(uuid,uuid,text) TO postgres;

-- Refuse unexpected trigger configurations instead of replacing unrelated auditing.
DO $verify$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgrelid = 'rya.valores_atributo_producto'::regclass
          AND tgname = 'trg_valores_atributo_producto_audit_v2' AND NOT tgisinternal
          AND tgenabled = 'O' AND tgtype = 23
          AND tgfoid IN ('rya.auditar_fila()'::regprocedure, 'rya.auditar_atributo_catalogo()'::regprocedure)
    ) THEN RAISE EXCEPTION 'El trigger de auditoría cambió; revisar antes de continuar'; END IF;
END;
$verify$;
DROP TRIGGER trg_valores_atributo_producto_audit_v2 ON rya.valores_atributo_producto;
CREATE TRIGGER trg_valores_atributo_producto_audit_v2
BEFORE INSERT OR UPDATE ON rya.valores_atributo_producto
FOR EACH ROW EXECUTE FUNCTION rya.auditar_atributo_catalogo();

COMMIT;

-- This migration does not restore any data by itself.
-- Verification must include: direct UPDATE rejected; anon/authenticated cannot execute;
-- trusted admin restore succeeds, clears deletion stamps and creates a consumed audit event;
-- a later transaction rollback rolls back both restoration and its audit event.
