# Mercado Pago Perú — Checkout API con Orders

Esta integración utiliza Orders API, no Payments API ni Preferences. Las tarjetas usan Card Payment Brick; Yape genera su token con MercadoPago.js y PagoEfectivo muestra instrucciones hasta que Mercado Pago confirme la acreditación.

## Para los compañeros: ¿necesito credenciales?

| Qué vas a probar | ¿Necesitas claves de Mercado Pago en tu computadora? | Qué preparar |
| --- | --- | --- |
| La página publicada en Vercel | No | Tu cuenta de usuario de RYA Tech. El responsable del despliegue configura Render. Usa exclusivamente datos de pago de prueba mientras la integración esté en Prueba. |
| Pruebas automatizadas de frontend y backend | No | Ejecuta `npm run test:ci` en el frontend y `mvnw test` en el backend. La suite usa HTTP simulado y H2; no cobra dinero ni se conecta a Mercado Pago. |
| Catálogo, administración u otras funciones con backend local, sin iniciar cobros | No, para Mercado Pago | Configura BD y autenticación como indica el README. Las variables de pago pueden quedar vacías. |
| Checkout completo contra Mercado Pago desde tu propio backend | Sí, de prueba | Configura las variables siguientes en tu `backend/.env` y usa una BD de pruebas con la migración aplicada. Para recibir notificaciones necesitas además un webhook HTTPS que apunte a tu backend. |

No hay que crear una integración por cada compañero si todos prueban la misma página desplegada. Tampoco deben volver a ejecutar las migraciones sobre la BD compartida si el responsable ya las aplicó. Las compras realizadas contra esa BD sí crean pedidos de prueba en el proyecto, aunque Mercado Pago no cobre dinero real.

Para trabajar por separado con un backend propio, usa una aplicación/cuenta de prueba propia o credenciales de prueba autorizadas por el responsable del equipo. No cambies el webhook de la aplicación compartida para apuntarlo a tu computadora: dejarías de enviar sus notificaciones al backend de Render. Coordina una integración de prueba separada para esa tarea.

Las plantillas compartidas son `.env.example` y `backend/.env.example`; contienen nombres de variables, no valores privados. Copia la plantilla elegida a su `.env` local y completa los valores solo en tu equipo. El `.env` privado no se incorpora a GitHub. No hace falta solicitar claves privadas para ejecutar la suite automatizada.

## Crear la aplicación y obtener credenciales
1. En Mercado Pago Developers, crea la aplicación con **Checkout API → API de Orders**.
2. Configura una cuenta de prueba siguiendo las instrucciones actuales del portal. No determines el entorno solamente por el prefijo de las claves.
3. Copia la Public Key y el Access Token de la integración de prueba. No publiques el token ni lo pongas en Vercel o en el código.

### De dónde sale cada valor

| Variable | Dónde obtenerla | Dónde colocarla |
| --- | --- | --- |
| `MERCADOPAGO_PUBLIC_KEY` | [Mercado Pago Developers](https://www.mercadopago.com.pe/developers) → tu aplicación → **Prueba → Credenciales** | Backend local o variables de Render. El frontend la recibe desde el backend. |
| `MERCADOPAGO_ACCESS_TOKEN` | Misma aplicación y sección **Prueba → Credenciales**, campo **Access Token** | Solo backend local o Render. |
| `MERCADOPAGO_WEBHOOK_SECRET` | Misma aplicación → **Configuración de webhooks → Prueba**, seleccionar **Order (Mercado Pago)** y guardar; copiar **Firma secreta** | En el backend que recibe esas notificaciones. |
| `MERCADOPAGO_LIVE_MODE` | Es una configuración del proyecto, no una clave del portal | `false` para las pruebas. |

La firma secreta no es el Access Token: son valores distintos. Para probar la página pública, las cuatro variables las administra el responsable de Render; los compañeros no necesitan copiarlas en sus equipos.

## Variables locales y Render
Configura en backend/.env para trabajar localmente; Spring también admite el .env de la raíz al arrancar desde ella. En Render usa las mismas variables en Environment:
~~~env
MERCADOPAGO_PUBLIC_KEY=
MERCADOPAGO_ACCESS_TOKEN=
MERCADOPAGO_WEBHOOK_SECRET=
MERCADOPAGO_LIVE_MODE=false
~~~
La URL de la API predeterminada es https://api.mercadopago.com. La variable opcional MERCADOPAGO_API_URL solo debe cambiarse para pruebas controladas.

MERCADOPAGO_LIVE_MODE valida el entorno declarado por la respuesta. **No convierte credenciales de producción en credenciales de prueba**. Usa las credenciales de la cuenta de prueba. Para pagos reales se requieren las credenciales correctas y LIVE_MODE=true.

El frontend recibe la clave pública del backend. Reinicia el backend después de cambiar sus variables. Si faltan claves, se rechaza el inicio del pago: **no se simulan pedidos pagados ni se fabrican IDs**.

## Preparación de la base de datos
Conserva el esquema principal del proyecto y aplica, tras revisión y respaldo, **backend/db/manual/mercadopago-orders.sql**. Este archivo no se ejecuta automáticamente.

La migración agrega los datos de checkout a las tablas existentes de Mis compras, conserva pedidos e ítems anteriores y agrega la referencia de Orders a los pagos. El antiguo script pedidos-y-pagos-mercadopago.sql por sí solo no adapta una tabla de pedidos que ya existe: CREATE TABLE IF NOT EXISTS no agrega las columnas faltantes.

La migración registra MP_CARDS, MP_YAPE y MP_PAGOEFECTIVO si no existen; no reactiva referencias eliminadas. Comprueba que los métodos y las tarifas de envío estén activos antes de probar.

No se ejecutó esta migración automáticamente sobre Supabase.

## Webhook de Orders
En la aplicación de Mercado Pago, configura el evento **Order (Mercado Pago)** y la URL HTTPS:
~~~text
https://rya-tech-backend.onrender.com/api/v1/pagos/webhook
~~~
Configura la sección de pruebas para las credenciales de prueba y la sección de producción para las de producción. Copia la firma secreta generada en MERCADOPAGO_WEBHOOK_SECRET.

El backend exige x-signature, x-request-id y data.id, valida HMAC-SHA256 y consulta GET /v1/orders/{id} con el Access Token. No acepta como confirmación los importes o estados que vengan solamente en el cuerpo del webhook.

Para pruebas locales, el webhook necesita una URL HTTPS pública que llegue al backend local; localhost no es accesible desde Mercado Pago. También puedes comprobar la sincronización en el servicio desplegado.

## Flujo y controles
- POST /api/v1/pagos/iniciar requiere sesión, revalida el carrito ABIERTO y crea un pedido local con importes calculados por el servidor.
- POST /api/v1/pagos/procesar exige que el pedido pertenezca al usuario autenticado.
- Cada intento se registra antes de llamar a POST /v1/orders. Su UUID se usa como X-Idempotency-Key y external_reference.
- Importe y monto de la transacción se envían como strings con dos decimales. El navegador no decide el total.
- Solo se marca PAGADO si los estados de order y transacción indican processed/accredited y coinciden referencia, moneda, entorno e importes.
- PagoEfectivo action_required/waiting_payment y otros estados intermedios se muestran como pendientes.
- Ante timeout o resultado incierto, el intento queda bloqueado para evitar otro cobro. Consulta el estado y revisa Mercado Pago antes de abrir otro intento.
- GET /api/v1/pagos/pedidos/{id} permite al propietario consultar y sincronizar el resultado.
- Una notificación repetida no vuelve a limpiar el carrito. La limpieza conserva productos nuevos o cantidades que cambiaron.
- Un reembolso completo confirmado actualiza el pedido a REEMBOLSADO. Los contracargos y reembolsos parciales requieren el flujo administrativo correspondiente.
- Las credenciales y los datos de tarjeta no se guardan en el repositorio. El token y el OTP no se registran en los logs ni en la respuesta del pago.

## Pruebas antes de usar pagos reales
Usa exclusivamente cuentas y datos de prueba indicados por Mercado Pago para tu entorno:
1. Tarjeta aprobada y rechazada.
2. Yape: celular y código de aprobación de prueba; el OTP solo se entrega al SDK.
3. PagoEfectivo pendiente y posterior confirmación por webhook.
4. Consulta del pedido con otra cuenta: debe rechazarse.
5. Notificación sin firma o modificada: debe rechazarse.
6. Dos envíos simultáneos y repetición del webhook: no deben generar otro cobro.
7. Timeout: debe mostrar pendiente de confirmación.
8. Confirma que el pedido y sus productos aparezcan en Mis compras.

Las pruebas automatizadas usan HTTP simulado y H2. No sustituyen una compra de prueba real contra Mercado Pago y no verifican la configuración de la cuenta o de Supabase remota.

## Documentación oficial
- [Migración de Payments a Orders](https://www.mercadopago.com.pe/developers/es/docs/checkout-api-orders/resources/migrate-payments-to-orders)
- [Tarjetas y Card Payment Brick](https://www.mercadopago.com.pe/developers/es/docs/checkout-api-orders/payment-integration/websites/cards)
- [Yape](https://www.mercadopago.com.pe/developers/es/docs/checkout-api-orders/payment-integration/websites/yape)
- [PagoEfectivo ATM](https://www.mercadopago.com.pe/developers/es/docs/checkout-api-orders/payment-integration/websites/pagoefectivo-atm)
- [Notificaciones de Orders](https://www.mercadopago.com.pe/developers/es/docs/checkout-api-orders/notifications)
