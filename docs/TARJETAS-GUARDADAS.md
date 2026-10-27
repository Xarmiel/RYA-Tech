# Checkout con tarjetas guardadas

## Activación

Aplicar `backend/db/manual/tarjetas-guardadas.sql` en Supabase antes de reiniciar o desplegar el backend. La tabla vincula el usuario autenticado con su cliente de Mercado Pago y separa aplicaciones/entornos mediante un hash de la Public Key. No contiene tarjetas, CVV ni tokens de pago.

Se utilizan las credenciales existentes de Mercado Pago en el backend; la Public Key llega al navegador al preparar el pedido. No se agregan secretos en Angular ni se deben copiar al repositorio.

## Experiencia

1. Datos, entrega y selección del método de pago.
2. Al continuar con tarjeta se prepara el pedido y aparece Pago seguro. Esto no cobra.
3. Se consultan las tarjetas de la cuenta y se muestran como `XXXX-XXXX-XXXX-1234`, marca y vencimiento.
4. Agregar tarjeta abre un panel lateral con CardForm de MercadoPago.js y `iframe: true`. Número, vencimiento y CVV se capturan directamente en Mercado Pago.
5. Sin marcar Guardar, Pagar tokeniza y envía el cargo a Orders.
6. Guardar es voluntario y no cobra. Consume el token de asociación; para pagar se pide nuevamente el CVV y se genera un token nuevo. Nunca se reutiliza el token de guardado para cobrar.
7. Cada compra con una tarjeta guardada vuelve a solicitar CVV. En este flujo el pago con tarjeta guardada es en una cuota.
8. El éxito solo se muestra si el servidor verifica la acreditación. Los estados inciertos bloquean cobros repetidos.

## API y seguridad

- `GET /api/v1/pagos/tarjetas`: tarjetas del usuario autenticado, con datos mínimos de visualización.
- `POST /api/v1/pagos/tarjetas`: `{ "token": "TOKEN_EFIMERO", "consentimiento": true }`.
- `POST /api/v1/pagos/procesar`: admite `savedCardId`; el backend busca la tarjeta en el cliente del usuario y obtiene de Mercado Pago el medio y tipo correctos. No acepta un customer_id del navegador.
- Al crear un cliente se utiliza el correo del JWT firmado, no un correo editable del checkout. No se recuperan clientes de Mercado Pago por coincidencia de correo.
- Se mantienen las comprobaciones de titularidad del pedido, precios, stock, moneda, importe e idempotencia.
- Un timeout al guardar requiere actualizar el listado antes de repetir. No se simula una tarjeta guardada ni un pago.

La validación automatizada usa respuestas de prueba de la API, sin enviar tarjetas ni generar cobros externos. La prueba real queda pendiente después de aplicar SQL y reiniciar. Mercado Pago puede imponer requisitos de credenciales y cuentas de prueba para Customers/Cards; sus rechazos se muestran como errores y no se sustituyen por datos ficticios.

## Documentación oficial consultada

- https://www.mercadopago.com.pe/developers/es/docs/checkout-api-orders/payment-management/improve-payment-approval/saved-cards
- https://github.com/mercadopago/sdk-js/blob/main/docs/card-form.md
- https://github.com/mercadopago/sdk-js/blob/main/docs/fields.md

## MCP de desarrollo

Registrado globalmente como `mercadopago` en Codex, URL `https://mcp.mercadopago.com/mcp`. No es una dependencia del ecommerce. La autenticación OAuth no se completó: Codex rechazó la respuesta porque el origen del endpoint de autorización no coincidía con el del servidor OAuth. No se desactivó esa validación ni se introdujeron tokens manualmente. Se consultó la documentación oficial vía web como alternativa.

## Compatibilidad con los estados de pedido de la BD existente

El backend crea pedidos en `PENDIENTE`, estado exigido por el trigger real. Acepta también pedidos históricos `PENDIENTE_PAGO` al consultar/reconciliar. El error «Pedido debe iniciar PENDIENTE» ocurre antes de abrir el formulario y no representa un cobro.

Aplicar además `backend/db/manual/pedidos-estados-orders.sql` para que el trigger reconozca la evidencia de pago de Orders en `rya.pagos`, conservando el flujo anterior en `rya.intentos_pago`. No permite marcar PAGADO sin un registro confirmado, acreditado, con Order ID y monto/moneda coincidentes. El backend guarda esa evidencia antes de cambiar el estado del pedido. La migración no modifica pedidos ni pagos existentes.

Validación local del 27/10/2026: ajuste de estados aplicado con autorización en Supabase; apertura del formulario verificada con campos de `secure-fields.mercadopago.com` y opción voluntaria de guardado. No se introdujeron datos de tarjeta ni se realizaron cobros. La corrección del estado inicial requiere desplegar el backend actualizado.
