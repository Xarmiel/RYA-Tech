-- Compatibilidad con Orders sin eliminar los controles de pago confirmado.
-- Aplicar después de mercadopago-orders.sql. No cambia pedidos ni pagos existentes.
BEGIN;
CREATE OR REPLACE FUNCTION rya.validar_estado_pedido()
RETURNS trigger LANGUAGE plpgsql AS $function$
BEGIN
 IF TG_OP='INSERT' THEN
  IF NEW.estado <> 'PENDIENTE' THEN RAISE EXCEPTION 'Pedido debe iniciar PENDIENTE'; END IF;
 ELSIF OLD.estado IS DISTINCT FROM NEW.estado THEN
  IF NOT (
   (OLD.estado IN ('PENDIENTE','PENDIENTE_PAGO') AND NEW.estado IN ('PAGADO','CANCELADO')) OR
   (OLD.estado='PAGADO' AND NEW.estado IN ('EN_PREPARACION','CANCELADO','REEMBOLSADO')) OR
   (OLD.estado='EN_PREPARACION' AND NEW.estado IN ('ENVIADO','CANCELADO')) OR
   (OLD.estado='ENVIADO' AND NEW.estado='ENTREGADO')
  ) THEN RAISE EXCEPTION 'Transición de pedido no permitida: % -> %',OLD.estado,NEW.estado; END IF;
  IF NEW.estado='PAGADO' AND NOT (
   EXISTS(SELECT 1 FROM rya.intentos_pago i WHERE i.pedido_id=NEW.id AND i.estado='CONFIRMADO') OR
   EXISTS(SELECT 1 FROM rya.pagos p WHERE p.pedido_id=NEW.id AND p.pasarela='MERCADO_PAGO_ORDERS'
    AND p.estado='processed' AND p.status_detail='accredited' AND NULLIF(btrim(p.mp_order_id),'') IS NOT NULL
    AND p.monto=NEW.monto_total AND p.moneda=NEW.moneda)
  ) THEN RAISE EXCEPTION 'No se puede marcar PAGADO sin pago confirmado'; END IF;
  IF NEW.estado='REEMBOLSADO' AND NOT EXISTS(
   SELECT 1 FROM rya.pagos p WHERE p.pedido_id=NEW.id AND p.pasarela='MERCADO_PAGO_ORDERS'
    AND p.estado='refunded' AND NULLIF(btrim(p.mp_order_id),'') IS NOT NULL AND p.monto=NEW.monto_total AND p.moneda=NEW.moneda
  ) THEN RAISE EXCEPTION 'No se puede marcar REEMBOLSADO sin reembolso confirmado'; END IF;
  IF NEW.estado='CANCELADO' AND OLD.estado NOT IN ('PENDIENTE','PENDIENTE_PAGO') THEN
   RAISE EXCEPTION 'Cancelar pedido pagado exige tratamiento financiero explícito'; END IF;
 END IF;
 RETURN NEW;
END $function$;
COMMIT;
