#!/usr/bin/env node
/**
 * configure-env.js
 * Inyecta de forma segura las variables públicas de compilación en src/environments/environment.prod.ts
 * durante el despliegue en Vercel o en compilaciones de producción locales.
 *
 * Variables públicas admitidas:
 * - API_URL o NG_APP_API_URL: Dirección base absoluta del backend en Render (ej. https://rya-tech-backend.onrender.com)
 * - SUPABASE_URL: URL pública del proyecto Supabase
 * - SUPABASE_ANON_KEY: Clave pública anónima de Supabase
 *
 * REGLA DE SEGURIDAD: Nunca inyecta contraseñas de BD, service_role o credenciales privadas en el frontend.
 */

const fs = require('fs');
const path = require('path');

// 1. Verificación de seguridad activa
const FORBIDDEN_SECRETS = [
  'DB_PASSWORD',
  'SUPABASE_SERVICE_ROLE_KEY',
  'SERVICE_ROLE',
  'DB_URL',
  'DB_USERNAME'
];

for (const secret of FORBIDDEN_SECRETS) {
  if (process.env[secret] && (process.env.API_URL === process.env[secret] || process.env.SUPABASE_ANON_KEY === process.env[secret])) {
    console.error(`[configure-env] ERROR DE SEGURIDAD: Se detectó un intento de inyectar una clave privada (${secret}) en el frontend.`);
    process.exit(1);
  }
}

// 2. Extraer variables con valores predeterminados seguros
const rawApiUrl = process.env.API_URL || process.env.NG_APP_API_URL || '';
const apiUrl = rawApiUrl.trim().replace(/\/+$/, '');

const supabaseUrl = (process.env.SUPABASE_URL || 'https://rasudbiaxwyoojxxdutz.supabase.co').trim().replace(/\/+$/, '');
const supabaseAnonKey = (process.env.SUPABASE_ANON_KEY || 'sb_publishable_Lp3yRQwKYfpaajrT_CMmew_oCBPdmFT').trim();

// 3. Generar contenido de environment.prod.ts
const targetPath = path.join(__dirname, '..', 'src', 'environments', 'environment.prod.ts');

const content = `// Archivo generado automáticamente por scripts/configure-env.js
// No incluir claves privadas ni contraseñas en este archivo.
export const environment = {
  production: true,
  // URL absoluta del backend en Render configurada vía variable de entorno API_URL:
  apiUrl: ${JSON.stringify(apiUrl)},
  // Credenciales públicas del cliente Supabase:
  supabaseUrl: ${JSON.stringify(supabaseUrl)},
  supabaseAnonKey: ${JSON.stringify(supabaseAnonKey)},
};
`;

try {
  fs.writeFileSync(targetPath, content, 'utf8');
  console.log('[configure-env] src/environments/environment.prod.ts actualizado correctamente.');
  if (apiUrl) {
    console.log(`[configure-env] Backend API URL configurada: ${apiUrl}`);
  } else {
    console.log('[configure-env] Backend API URL vacía (modo proxy o relativo). Para conectar a Render en Vercel, defina API_URL en las variables de Vercel.');
  }
  console.log(`[configure-env] Supabase URL: ${supabaseUrl}`);
} catch (err) {
  console.error('[configure-env] Error escribiendo environment.prod.ts:', err);
  process.exit(1);
}
