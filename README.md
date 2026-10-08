# RYA System — Tienda de tecnología y asesoría de ensamblaje

Proyecto **e-commerce de hardware y periféricos** con asesoría automática de ensamblado de PCs.
Es una única aplicación **Spring Boot** que sirve el **frontend estático (HTML/CSS/JS vanilla)** y la **API REST** desde el mismo puerto (`8080`).

| | |
|---|---|
| **Backend** | Java 25 · Spring Boot 4.1.0 (Web MVC, Security, Data JPA, Validation) · PostgreSQL (Supabase) · Lombok |
| **Frontend** | HTML5 + CSS3 + JavaScript (sin frameworks, sin bundler, sin build step) |
| **Infraestructura** | Maven Wrapper (`mvnw`) · Docker (imagen multi-etapa) |
| **Idioma de la UI y la API** | Español |

---

## Arranque rápido

```bash
# Requisitos: JDK 25 (o Docker) y, si no usas Docker, acceso a una base PostgreSQL
./mvnw spring-boot:run        # Linux/macOS
mvnw.cmd spring-boot:run      # Windows
```

La aplicación queda en **http://localhost:8080/** y redirige al login.

Con Docker:

```bash
docker build -t rya-system .
docker run -p 8080:8080 -e DB_URL=... -e DB_USERNAME=... -e DB_PASSWORD=... rya-system
```

**Cuentas demo** (creadas automáticamente en cada arranque):

| Rol | Email | Contraseña |
|---|---|---|
| Cliente | `demo@ryatech.pe` | `Password123!` |
| Administrador | `admin@ryatech.pe` | `Admin123!` |

> El panel de administración es `http://localhost:8080/html/admin.html` y solo la protege el frontend.

---

## Documentación

| Documento | Contenido |
|---|---|
| 📖 **[Documentación general](docs/DOCUMENTACION_GENERAL.md)** | Visión global, arquitectura, estructura del repositorio, datos, seguridad, despliegue, pendientes conocidos. |
| 🎨 **[Documentación de frontend](docs/DOCUMENTACION_FRONTEND.md)** | Páginas, carga de scripts, módulos JS, flujos (catálogo, carrito, checkout, admin), CSS y accesibilidad. |
| ⚙️ **[Documentación de backend](docs/DOCUMENTACION_BACKEND.md)** | API REST completa, capas (controller/service/repository), entidades, seguridad, migraciones y siembra de datos. |

> 📄 Versión en Word (.docx) disponible en `C:\Users\ramir\OneDrive\Escritorio\Documentacion RYA System\`.

---

## Estructura del repositorio

```
RYA_System/
├── README.md                     ← este archivo
├── docs/                         ← documentación + scripts Python de mantenimiento y backups
├── pom.xml · mvnw · mvnw.cmd     ← build Maven
├── Dockerfile                    ← build y ejecución en contenedor
├── index.html                    ← redirect a src/main/resources/static/html/login.html
└── src/
    ├── main/
    │   ├── java/com/ecommerce/backend/
    │   │   ├── config/           ← seguridad, migración y siembra de datos
    │   │   ├── controller/       ← endpoints REST
    │   │   ├── service/ (+implement/)
    │   │   ├── repository/
    │   │   ├── model/ (+enums/)
    │   │   ├── dto/ (request/ y response/)
    │   │   ├── mapper/ · exception/
    │   │   └── EcommerceBackendApplication.java
    │   └── resources/
    │       ├── application.properties
    │       └── static/           ← frontend completo (html/, css/, js/, images/)
    └── test/                     ← test de arranque de contexto
```
