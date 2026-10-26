# Manual de Integración: Mercado Pago (Perú)

Este documento detalla cómo crear tu cuenta de desarrollador, obtener tus credenciales de prueba (**sin necesidad de RUC**), vincularlas a **RYA-Tech** y realizar pagos de prueba utilizando tarjetas Visa y Mastercard.

---

## 1. Obtención de Credenciales de Prueba (Paso a Paso)

> [!NOTE]  
> Para el entorno de desarrollo y pruebas **NO es necesario RUC ni empresa constituida**. Puedes usar tu cuenta personal de Mercado Libre o crear una gratis con tu DNI / correo Gmail.

1. **Ingresar al portal de desarrolladores:**
   * Entra a [https://www.mercadopago.com.pe/developers](https://www.mercadopago.com.pe/developers).
   * Inicia sesión con tu cuenta de Mercado Libre / Mercado Pago.

2. **Crear una Aplicación:**
   * Haz clic en tu perfil o en **"Tus integraciones"**.
   * Presiona el botón **"Crear aplicación"**.
   * Completa los campos:
     * **Nombre de la app:** `RYA-Tech` (o el que desees).
     * **Solución de pago:** Selecciona **"Checkout Pro y Bricks"** (o "Pagos online").
     * **¿Qué plataforma estás integrando?:** Selecciona "Desarrollo propio".
   * Acepta los términos y haz clic en **Crear aplicación**.

3. **Copiar las Credenciales de Prueba:**
   * Dentro de tu aplicación recién creada, en el menú lateral izquierdo haz clic en **"Credenciales de prueba"**.
   * Encontrarás dos valores clave:
     * **Public Key:** Inicia con `TEST-...`
     * **Access Token:** Inicia con `TEST-...` (haz clic en el ícono de ojo para revelarlo o en copiar).

---

## 2. Vinculación en RYA-Tech

Abre tu archivo local de variables de entorno:
* Ubicación: `backend/.env` (o `.env` en la raíz del repositorio).

Pega tus credenciales obtenidas:

```env
# ==========================================
# Pasarela de Pagos: Mercado Pago (Perú)
# ==========================================
MERCADOPAGO_PUBLIC_KEY=TEST-tu_public_key_aqui
MERCADOPAGO_ACCESS_TOKEN=TEST-tu_access_token_aqui
MERCADOPAGO_WEBHOOK_SECRET=tu_webhook_secret_opcional
```

> [!TIP]  
> Si dejas los valores por defecto (`TEST-placeholder`), el backend entrará en **modo de simulación local segura**, permitiéndote probar la interfaz del checkout sin fallar incluso antes de poner tus claves reales.

---

## 3. Preparación de la Base de Datos

Antes de realizar compras en local, ejecuta el script de base de datos en tu panel SQL de **Supabase**:

* Archivo: `backend/db/manual/pedidos-y-pagos-mercadopago.sql`

Este script crea:
* Las tablas `rya.pedidos`, `rya.pedido_items` y `rya.pagos`.
* Los métodos de pago `MP_CARDS`, `MP_YAPE` y `MP_PAGOEFECTIVO` en la tabla `rya.metodos_pago`.

---

## 4. Tarjetas de Prueba Oficiales (Visa y Mastercard)

Para completar compras de prueba en el componente embebido (**Payment Brick**) de RYA-Tech, utiliza los siguientes datos de tarjeta de prueba.

* **Fecha de vencimiento:** Cualquier mes y año futuro (ej. `12/30`).
* **Código de seguridad (CVV):** Cualquier número de 3 dígitos (ej. `123`).
* **Titular de la tarjeta:** `APRO` (o cualquier nombre para compras aprobadas).

### Tarjetas para Pagos Aprobados

| Marca | Número de Tarjeta | Estado Esperado |
| :--- | :--- | :--- |
| **Visa (Crédito/Débito)** | `4242 4242 4242 4242` | **Aprobado (approved)** |
| **Mastercard (Crédito/Débito)** | `5252 5252 5252 5252` | **Aprobado (approved)** |

### Tarjetas para Pruebas de Error / Rechazo

Si deseas comprobar cómo responde el sistema ante rechazos o bloqueos bancarios:

| Motivo de Prueba | Número de Tarjeta | Mensaje en Pantalla |
| :--- | :--- | :--- |
| **Fondos insuficientes** | `4023 2323 2323 2323` | Rechazo por falta de saldo en la cuenta. |
| **Rechazo general / Seguridad** | `4060 6060 6060 6060` | Tarjeta no procesable, permite reintentar. |
| **Rechazo por código de seguridad incorrecto** | `4044 4444 4444 4444` | Valida CVV inválido. |

---

## 5. Pruebas de Billetera Digital (Yape / Transferencia)

Dentro del Brick de Mercado Pago en Perú:
* En el paso 4 del checkout, selecciona la opción de **Billeteras / Yape** o transferencia bancaria.
* Puedes ingresar cualquier número celular de 9 dígitos (ej. `987654321`) y el código de aprobación de prueba (`123456`).
