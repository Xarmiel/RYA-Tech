# RYA Tech · Frontend Angular

Aplicación Angular de RYA Tech. El prototipo visual del design system se encuentra archivado como referencia en `RYA_System_Archivo/disenos/design-system` y no se utiliza como fuente de datos en ejecución.

## Desarrollo

```bash
npm install
npm start
```

La aplicación queda disponible en `http://localhost:4200`. Las solicitudes `/api` se redirigen a `http://localhost:8080` mediante `proxy.conf.json`.

El catálogo usa `GET /api/v1/productos` para el listado y `GET /api/v1/productos/filtros` para construir el panel de facetas dinámicas. Cada filtro aplicado se envía como `filtro.{codigo}=valor`.

## Política de datos

- No incluir productos, precios, stock, ofertas ni configuraciones demo en componentes o servicios.
- Toda información comercial y técnica llega desde Spring Boot mediante `/api/v1`.
- Mientras un endpoint no exista, mostrar un estado de carga, vacío o error.
- `localStorage` se limita a preferencias versionadas y borradores no autoritativos.
- Precios, stock, compatibilidad, permisos, compras y cotizaciones siempre se revalidan en backend.

## Contrato inicial de Asesorías

- `GET /api/v1/pc-builder/inicial`: familias de uso, perfiles, resoluciones y tipos de componente habilitados.
- `GET /api/v1/pc-builder/opciones?tipo={codigo}`: productos reales compatibles para la categoría seleccionada.
- `POST /api/v1/pc-builder/validar-cambio`: revalida la selección manual, consumo, fuente y total.
- `POST /api/v1/pc-builder/recomendar`: genera los resultados de los modos por presupuesto y componente ancla.

Mientras estos endpoints no estén disponibles, la interfaz presenta un estado de carga, error o vacío; nunca lo sustituye por productos simulados. La clave `rya.builder.ui.v1` conserva únicamente modalidad, identificadores seleccionados y el presupuesto escrito. El backend mantiene la autoridad sobre precio, stock, compatibilidad, consumo y resultado.

## Comandos

```bash
npm run build
npm test
```

## Estructura inicial

```text
src/app/
  core/       preferencias, API e infraestructura
  features/   páginas organizadas por funcionalidad
  shared/     componentes reutilizables
```
