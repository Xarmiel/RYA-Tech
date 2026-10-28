# Pedidos, reseñas, contenido y operación administrativa

## Activación

1. Aplicar primero `backend/db/manual/envios-y-recojo.sql` si aún no fue aplicado.
2. Aplicar `backend/db/manual/modulos-administrativos.sql` con el usuario de administración de Supabase, antes de reiniciar/desplegar este backend. El script conserva las reseñas publicadas y no genera datos de demostración.
3. Reiniciar backend/frontend e ingresar con una cuenta cuyo `app_metadata.role` sea `admin`.
4. Las tablas nuevas tienen RLS habilitado sin políticas públicas: los accesos pasan por el backend. No se requieren API keys adicionales.

## Secciones

- `/admin/pedidos`: consulta por código/cliente y estado, detalle de productos comprados, importes originales, intentos de pago y decisiones registradas. Permite cancelar solamente pedidos pendientes **sin ningún intento de pago**. No marca pagos como aprobados ni cancela cobros inciertos. El despacho se administra desde Envíos.
- `/admin/resenas`: moderación de reseñas existentes: pendiente, aprobada, oculta o rechazada. Exige motivo y versión vigente. Conserva autor, texto y calificación. Solo las aprobadas se publican. No incorpora un formulario público nuevo para enviar reseñas.
- `/admin/contenido`: crear/editar/despublicar FAQ, banners y artículos de Experiencias; editar email, teléfono, WhatsApp y horario. Vista previa en texto. Los banners y artículos aparecen en Inicio o Experiencias según su ubicación. No se interpreta HTML. Las categorías de ayuda se seleccionan de las existentes. Los campos de contacto nulos conservan el valor de configuración; un texto vacío permite quitarlo.
- `/admin/compatibilidad`: registrar o actualizar una relación entre una CPU normalizada y una revisión concreta de placa. Selecciona BIOS y fuente técnica existentes, con confirmación explícita de la evidencia oficial. No crea fuentes, BIOS o revisiones a partir de nombres comerciales. Una ficha comercial pendiente no sustituye estos registros. Sockets diferentes no pueden certificarse como soportados y una BIOS de otra revisión se rechaza. La relación guardada es la que consulta el asesor existente.
- `/admin/calidad`: consulta paginada de productos con ausencia de imágenes, marca, fuente o ficha y avisos disponibles. Abre el editor de ficha del producto. Es una detección de faltantes básicos, no una certificación automática de autenticidad ni una verificación exhaustiva de todos los campos opcionales.
- `/admin/importacion`: acceso dedicado al importador real de JSON/CSV existente, con previsualización, marcas/categorías, SKU exactos, resolución de duplicados y confirmación de cada guardado. No agrega soporte para archivos Excel ni crea una historia de lotes persistida en el servidor. Advierte al salir con productos revisados sin guardar.
- `/admin/postventa`: registra una solicitud asociada a un pedido pagado, con tipo, motivo y enlace HTTPS de evidencia opcional. Flujo: ABIERTA → EN_REVISION → APROBADA/RECHAZADA → CERRADA. Exige resolución y controla versiones. **Aprobar no ejecuta un reembolso, una devolución de stock ni un trámite automático con el fabricante.**
- `/admin/reportes`: ventas por día (hora Perú), productos más vendidos y stock bajo actual. Incluye solo pedidos cuyo estado actual sea PAGADO; los totales de ventas incluyen envío. Los pendientes y el stock se consultan en su estado actual, no como reconstrucción histórica. Periodo máximo: 367 días inclusivos; top 20 productos y hasta 100 filas de stock bajo. Exportación CSV de la tabla mostrada con protección frente a fórmulas de hoja de cálculo.

## Integridad y revisión

- Todos los endpoints administrativos usan el guard del servidor existente. Los roles enviados en `user_metadata` no otorgan acceso.
- La cancelación, moderación, cambios de contenido, decisiones de compatibilidad y postventa dejan una entrada con actor, acción, motivo y fecha en `historial_administrativo`.
- Los conflictos de versión requieren actualizar y revisar de nuevo; no se fuerzan sobrescrituras silenciosas.
- Las pruebas usan H2 y respuestas HTTP controladas. No cargan datos en Supabase ni realizan cobros reales. La migración debe verificarse/aplicarse por separado en PostgreSQL.
