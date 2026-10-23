# RYA System — E-commerce y Asesoría Técnica de Ensamblaje

Plataforma integral de comercio electrónico de componentes de computación, periféricos y sistema interactivo de asesoría de ensamblado de PCs (PC Builder).

Un solo repositorio GitHub contiene el backend **Spring Boot** y el frontend **Angular**. Son dos aplicaciones desplegables del mismo proyecto; no se requieren dos repositorios ni unir sus dependencias.

| Componente | Tecnologías | Directorio |
|---|---|---|
| **Backend** | Java 21 · Spring Boot 3.4.3 · Spring Data JPA · Spring Security (JWT / Supabase Auth) · PostgreSQL · scripts SQL manuales · Maven Wrapper | `backend/` |
| **Frontend** | Angular 22 (Standalone Components, Signals) · SCSS · Vitest · Supabase Client JS | `frontend-angular/app/` |
| **Base de Datos & Auth** | PostgreSQL, Supabase Auth y Storage alojados externamente | Scripts en `backend/db/manual/` |

---

## Estructura del Repositorio

```text
RYA_System/
├── backend/                         # Backend Spring Boot (API REST)
│   ├── src/main/java/               # Controladores, servicios, modelos, DTOs, seguridad
│   ├── src/main/resources/          # application.properties (perfiles dev y prod)
│   ├── src/test/                    # Suite de pruebas unitarias y de integración (H2)
│   ├── db/manual/                   # Scripts SQL complementarios de estructura y almacenamiento
│   ├── pom.xml                      # Configuración de dependencias Maven
│   └── mvnw / mvnw.cmd              # Maven Wrapper para Linux/macOS y Windows
├── frontend-angular/
│   └── app/                         # Frontend Angular (SPA)
│       ├── src/app/
│       │   ├── core/                # Autenticación, guards, interceptores, servicios API
│       │   ├── features/            # Módulos: admin, catálogo, pc-builder, cart, etc.
│       │   └── shared/              # Componentes de UI, directivas y pipes reutilizables
│       ├── public/                  # Recursos e imágenes públicas de marca
│       ├── angular.json             # Configuración del workspace Angular
│       ├── package.json             # Dependencias npm del frontend
│       └── proxy.conf.json          # Proxy de desarrollo hacia http://localhost:8080
├── render.yaml                      # Render: rootDir backend, Docker y variables privadas externas
├── .github/workflows/               # Verificación backend y frontend; no contienen credenciales
├── .env.example                     # Plantilla de variables de entorno (sin secretos)
├── package.json                     # Herramientas auxiliares de entorno raíz
└── README.md                        # Documentación principal del proyecto
```

> **Nota:** La documentación histórica, kits de migración previa y referencias de diseño estático se conservan archivados de forma externa en `RYA_System_Archivo/` para mantener limpio el repositorio ejecutable.

## Un GitHub, dos despliegues

Repositorio actual: https://github.com/Xarmiel/RYA_System · rama local actual: `main`.

| Plataforma | Repositorio | Raíz de aplicación | Configuración |
|---|---|---|---|
| Render | El mismo RYA_System | `backend` | Blueprint `render.yaml` en la raíz Git; ya declara `rootDir: backend` |
| Vercel | El mismo RYA_System | `frontend-angular/app` | Elegir esa Root Directory al importar; usa el `vercel.json` dentro de esa carpeta, Node 24, `npm ci` y `npm run build` |

No ejecutar `git init` dentro de backend o frontend ni crear otros repositorios. Los dos servicios tienen su propio build y sus variables, pero se versionan juntos. Vercel sirve Angular y Render sirve la API; compartir GitHub no hace que una plataforma ejecute ambos automáticamente.

Para una clonación nueva, instalar dependencias Angular en `frontend-angular/app`, no en la raíz. El `package.json` raíz existente se conserva como herramienta auxiliar; no construye el sitio. `environment.prod.ts` se genera con `npm run build` y no se sube a Git; la configuración de desarrollo y el script generador sí se versionan.

Antes del primer commit de esta reorganización, revisar altas y eliminaciones juntas: la versión antigua estaba en la raíz, mientras que el código actual está en backend y frontend-angular/app. Revisar `git status` y el conjunto de cambios antes de confirmar. No subir `.env`, claves privadas, configuración MCP, `.vercel`, cachés Supabase, dependencias, logs ni compilados; las plantillas `.env.example` sí se incluyen.

GitHub Actions verificará cada aplicación según los archivos modificados. Estos workflows no contienen secretos ni publican por sí solos. Después de conectar las plataformas, un push/merge a la rama vinculada puede activar despliegues; revisar su política antes de subir cambios.

Variables separadas: DB/JWT/CORS solo en Render; `API_URL`, `SUPABASE_URL` y `SUPABASE_ANON_KEY` públicas de compilación en Vercel. Definir `API_URL` con la URL HTTPS real obtenida de Render; no subir contraseñas al frontend. `.env.example` no se carga automáticamente por el script Angular.

---

## Requisitos Previos

- **Java Development Kit (JDK):** Versión 21 o superior instalada y en `PATH`.
- **Node.js y npm:** Usar Node 24 (al menos 24.15.0) y npm compatible. Angular también admite las versiones declaradas en `frontend-angular/app/package.json`; `.nvmrc` selecciona Node 24.
- **Conectividad a Base de Datos:** Proyecto en Supabase (PostgreSQL, Supabase Auth y Storage) o ejecución con perfil de pruebas in-memory (`H2`).

---

## Variables de Entorno

Copia el archivo `.env.example` en la raíz como `.env` para desarrollo local:

```bash
cp .env.example .env
```

Las variables principales requeridas (nunca compartir valores de producción ni secretos):

| Variable | Propósito | Ámbito |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Perfil de Spring Boot (`dev` o `prod`). | Backend |
| `PORT` | Puerto HTTP del backend (por defecto `8080`). | Backend |
| `DB_URL` | URL JDBC de conexión PostgreSQL (ej. pooler de Supabase con `sslmode=require`). | Backend |
| `DB_USERNAME` | Usuario de base de datos. | Backend |
| `DB_PASSWORD` | Contraseña de conexión a la base de datos. | Backend |
| `DB_SCHEMA` | Esquema relacional (`rya` o `public`). | Backend |
| `CORS_ALLOWED_ORIGINS` | Orígenes permitidos (ej. `http://localhost:4200`). | Backend |
| `SUPABASE_JWT_ISSUER` | URL emisora de tokens de Supabase Auth (`https://<PROJECT_REF>.supabase.co/auth/v1`). | Backend |
| `SUPABASE_JWKS_URI` | Endpoint JWKS para validación pública de firmas JWT sin secreto compartido. | Backend |
| `SUPABASE_URL` | Endpoint público del proyecto Supabase. | Frontend / Auth |
| `SUPABASE_ANON_KEY` | Llave anónima pública de Supabase para el cliente web. | Frontend / Auth |

---

## Backend — Spring Boot

Ubicado en `backend/`. Proporciona la API REST bajo el prefijo `/api/v1`.

### Comandos Comprobados

```bash
# Cambiar al directorio del backend
cd backend

# Ejecutar la suite de pruebas con H2
# Windows:
.\mvnw.cmd test
# Linux/macOS:
./mvnw test

# Iniciar la aplicación en modo desarrollo (http://localhost:8080)
# Windows:
.\mvnw.cmd spring-boot:run
# Linux/macOS:
./mvnw spring-boot:run

# Compilar empaquetado de producción (JAR ejecutable)
# Windows:
.\mvnw.cmd clean package -DskipTests
# Linux/macOS:
./mvnw clean package -DskipTests
```

---

## Frontend — Angular

Ubicado en `frontend-angular/app/`. Proporciona la interfaz de usuario interactiva y consume la API REST.

### Comandos Comprobados

```bash
# Cambiar al directorio del frontend
cd frontend-angular/app

# Instalar dependencias
npm install

# Ejecutar la suite de pruebas automatizadas (Vitest)
npm run test:ci

# Iniciar servidor de desarrollo (http://localhost:4200)
# Las peticiones /api se redirigen a http://localhost:8080 mediante proxy.conf.json
npm start

# Compilar para producción (genera bundle optimizado en dist/)
npm run build
```

---

## Seguridad y Acceso Administrativo

El sistema utiliza **Supabase Auth** como proveedor de identidad y tokens JWT para la autenticación:

1. **Sin credenciales ni contraseñas demo quemadas:** No existen cuentas demo codificadas en duro en el repositorio.
2. **Sesión real vía Supabase:** El inicio de sesión emite un JWT firmado con identidad y metadatos (`app_metadata`), nunca la contraseña del usuario.
3. **Rol de Administrador:**
   - Para acceder al panel de administración (`/admin`), la cuenta debe tener asignado el rol `admin` en `app_metadata.role` dentro de Supabase Auth.
   - **Frontend:** `AdminGuard` intercepta la navegación a rutas administrativas y deniega el acceso si el usuario no cuenta con una sesión activa con rol `admin`.
   - **Backend:** `SecurityConfig` y los filtros JWT validan la autenticidad de la firma mediante JWKS y restringen las operaciones de modificación (`/api/v1/admin/**`, `/api/v1/productos/**`, etc.) a solicitudes con autorización administrativa válida, respondiendo con `401 Unauthorized` o `403 Forbidden` ante accesos no autorizados.

---

## Despliegue en Producción (Vercel & Render)

El sistema está configurado y optimizado para una arquitectura desacoplada:

### Frontend en Vercel (`frontend-angular/app/`)
- **Directorio raíz en Vercel:** `frontend-angular/app`
- **Framework Preset:** Angular (utiliza `dist/rya-tech/browser` configurado en `vercel.json`).
- **Comando de compilación:** `npm run build` (ejecuta `node scripts/configure-env.js && ng build`).
- **Variables de entorno en Vercel:**
  - `API_URL`: URL absoluta del backend en Render (ej. `https://rya-tech-backend.onrender.com`).
  - `SUPABASE_URL`: Endpoint de Supabase.
  - `SUPABASE_ANON_KEY`: Llave anónima pública de Supabase.
- **Rutas y caché:** SPA fallback seguro (excluye `/api/` para evitar respuestas HTML ante fallos de API) y cabeceras de caché que no congelan activos sin hash.

### Backend en Render (`backend/`)
- **Tipo de servicio:** Web Service con Docker (`eclipse-temurin:21`).
- **Blueprint:** Compatible con `render.yaml` en la raíz (`rootDir: backend`).
- **Directorio del servicio:** `backend`
- **Health Check Path:** `/api/v1/health` (ligero, sin carga a la BD ni exposición de datos sensibles).
- **Memoria y conexiones:** Heap limitado al 50% del contenedor, dejando margen para memoria nativa; pool de producción de hasta 2 conexiones y mínimo inactivo 0. Son valores iniciales, no una garantía de capacidad o ausencia de OOM.
- **Variables de entorno en Render:**
  - `SPRING_PROFILES_ACTIVE=prod`
  - `DB_SCHEMA=rya`
  - `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`: Credenciales de PostgreSQL en Supabase.
  - `CORS_ALLOWED_ORIGINS`: Dominio exacto autorizado de Vercel (ej. `https://tu-proyecto.vercel.app`, sin comodín `*.vercel.app`).
  - `SUPABASE_JWT_ISSUER` y `SUPABASE_JWKS_URI`: Claves públicas de verificación JWT.
  - `APP_TECHNICAL_DRAFTS_ENABLED=false` (solo activar si se aplicó el script de borradores técnicos).
- **Actualizaciones:** `.github/workflows/backend.yml` prepara pruebas, empaquetado y construcción Docker con Java 21. El Blueprint usa `autoDeployTrigger: checksPass`; solo desplegará actualizaciones de la rama enlazada cuando GitHub ejecute y apruebe sus comprobaciones. La configuración local no significa que GitHub o Render ya lo hayan probado.
- **Validación del 23/10/2026:** 146 pruebas backend y empaquetado correctos. Imagen Docker `rya-tech-backend:presentacion` construida con Java 21; contenedor con perfil prod, límite de 512 MiB y 1 CPU verificado: salud 200, catálogo real 200 (18 productos), admin sin sesión 401, CORS autorizado 200 y origen ajeno 403. Consumo observado ~329 MiB, sin prueba de carga. Se usaron variables privadas solo en ejecución; la imagen no las incorpora. El contenedor temporal se retiró después de verificarlo; los otros contenedores no se tocaron. Despliegue público y CI en GitHub siguen pendientes.

> Para el paso a paso detallado de aprovisionamiento, consulta la guía en:
> `RYA_System_Archivo/documentacion/despliegue/VERCEL-RENDER.md`.
