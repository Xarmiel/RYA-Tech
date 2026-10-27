# Recuperación de contraseña

## Flujo

1. `/recuperar-clave` solicita un enlace por correo, con retorno a `/restablecer-clave`.
2. Supabase valida el enlace y crea una sesión de recuperación. Esta sesión por sí sola no cambia la contraseña.
3. Al recibir `PASSWORD_RECOVERY`, la aplicación navega a `/restablecer-clave`, incluso si Supabase devolvió al inicio por su configuración anterior. La navegación sale del callback de Supabase para no bloquear su sesión.
4. Se solicitan nueva contraseña (mínimo ocho caracteres) y confirmación. Solo se habilita el envío con una sesión de recuperación válida.
5. `updateUser({password})` cambia la contraseña. La página muestra confirmación, limpia los campos y ofrece continuar a Mi cuenta.

Un marcador temporal en sessionStorage, vinculado al usuario y con duración máxima de una hora, permite recargar la pantalla sin perder este paso. No contiene tokens ni contraseñas y no sustituye la autorización de Supabase. Se elimina al cambiar la contraseña, iniciar otra sesión o salir.

## Configuración de Supabase

En Authentication → URL Configuration:

- Site URL: `https://rya-tech.vercel.app`
- Redirect URLs: incluir exactamente `https://rya-tech.vercel.app/restablecer-clave` y `http://localhost:4200/restablecer-clave` para las pruebas locales. Conservar las URLs de registro/login ya existentes.
- No se necesitan claves adicionales, SQL ni modificar usuarios.

Después de desplegar el frontend, solicitar un correo nuevo. Los enlaces son de uso limitado y pueden expirar; un enlace usado antes no sirve para verificar de nuevo el flujo completo.

## Pruebas

Las pruebas automatizadas simulan el evento de recuperación y la respuesta de Supabase; no envían correos ni cambian contraseñas reales. El usuario debe completar personalmente el cambio con su enlace de correo.

Documentación: https://supabase.com/docs/reference/javascript/auth-resetpasswordforemail

Configuración verificada el 27/10/2026: Site URL actualizado a Vercel y ambas rutas de restablecimiento añadidas en Supabase; se conservaron las tres URLs anteriores. Validación automatizada: 405 pruebas frontend y compilación de producción aprobadas. No se enviaron correos ni se cambiaron contraseñas reales durante las pruebas.

## 404 al abrir el correo directamente

`/restablecer-clave` existe en Angular. Vercel debe servir `index.html` al acceder directamente a esa ruta, igual que a `/login` o `/producto/...`. La configuración anterior combinaba `cleanUrls: true` con un rewrite a `/index.html`, combinación incompatible según Vercel. Se establece `cleanUrls: false` y se conserva el rewrite SPA y la exclusión de `/api/`. Esto requiere un nuevo despliegue de Vercel; no se arregla solo modificando Supabase.

Validación: antes del ajuste publicado, `/` responde 200, pero `/login` y `/restablecer-clave` devuelven 404. Se verificó que la configuración local apunta al `index.html` real generado por Angular. La comprobación HTTP de producción debe repetirse después de desplegar. No se usan enlaces con tokens en estas comprobaciones.

Fuente: https://vercel.com/docs/project-configuration/vercel-json#rewrites
