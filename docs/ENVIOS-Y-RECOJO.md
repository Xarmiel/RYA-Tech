# Envíos y recojo

## Activación

1. Respaldar la BD y aplicar `backend/db/manual/envios-y-recojo.sql` en el SQL Editor de Supabase **antes de arrancar o desplegar este backend**. El script no se ejecuta automáticamente y no crea tarifas ficticias.
2. Reiniciar backend y frontend. Entrar como administrador en `/admin/envios`.
3. Crear al menos una tarifa de domicilio con departamento, transportista/servicio, costo y plazo; o un punto de recojo con nombre, dirección y horario. El recojo cuesta cero.
4. Probar un carrito en `/checkout`. Las ubicaciones se escriben con sugerencias procedentes de las tarifas reales; todavía no hay catálogo nacional de ubigeos. Un campo de cobertura vacío significa toda la provincia/departamento correspondiente, no falta de cobertura.
5. Realizar una compra con credenciales de prueba de Mercado Pago. Solo los pedidos PAGADO aparecen en el listado de despacho del panel.

## Comportamiento

- Las tarifas inactivas y las vinculadas a un transportista eliminado no se ofrecen ni se aceptan al validar/crear un pedido.
- Domicilio exige departamento, provincia, distrito y dirección. La cobertura se comprueba también en backend, sin distinguir tildes o mayúsculas.
- Cambiar ubicación o modalidad invalida la tarifa seleccionada. Sin cobertura no se puede continuar.
- Recojo no exige dirección del cliente: usa dirección y horario configurados por administración.
- Se guardan modalidad, destino, dirección, referencia, nombre del servicio, plazo y costo al crear el pedido. Editar la tarifa después no cambia esos datos. Los pedidos históricos anteriores conservan los datos disponibles; no se inventan horarios o transportistas que no fueron guardados.
- El despacho tiene estado independiente del pago. Domicilio: PENDIENTE → PREPARANDO → ENVIADO → ENTREGADO. Recojo: PENDIENTE → PREPARANDO → LISTO_RECOJO → RETIRADO.
- Se avanza una etapa por vez. ENVIADO y ENTREGADO requieren transportista y código de seguimiento. No se crean etiquetas ni se consulta automáticamente a una empresa logística.
- Seguimiento usa bloqueo y versión `updated_at`: ante conflicto, cerrar el editor, actualizar pedidos y volver a abrirlo.
- Mis compras muestra únicamente los datos de entrega y seguimiento de los pedidos del usuario autenticado.

## API

Todas las rutas `/api/v1/admin/envios` requieren `app_metadata.role=admin` validado en el servidor.

- GET /api/v1/admin/envios
- POST /api/v1/admin/envios
- PUT /api/v1/admin/envios/{id} (incluye activo para desactivar/reactivar sin borrado)
- GET /api/v1/admin/envios/pedidos?page=0 (30 pedidos pagados por página)
- PUT /api/v1/admin/envios/pedidos/{id}/seguimiento (estado, transportista, codigo, version)

La selección pública se entrega en GET /api/v1/checkout/opciones y se revalida en las operaciones existentes de checkout/pago. No requiere nuevas API keys.
