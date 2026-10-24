-- ==============================================================================
-- RYA TECH - MÓDULO DE ASESORÍAS Y PERFILES DE USO
-- Esquema: rya
-- Tablas sin datos semilla para administración manual desde el panel
-- ==============================================================================

CREATE SCHEMA IF NOT EXISTS rya;

-- 1. Familias de Perfiles (Ej: Gaming, Creación de Contenido, Oficina, etc.)
CREATE TABLE IF NOT EXISTS rya.familias_perfil_asesoria (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo VARCHAR(60) NOT NULL UNIQUE,
    nombre VARCHAR(120) NOT NULL,
    descripcion TEXT,
    icono VARCHAR(60) DEFAULT 'tune',
    posicion INTEGER NOT NULL DEFAULT 0,
    activo BOOLEAN NOT NULL DEFAULT true,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Perfiles de Asesoría (Ej: Gaming Competitivo, Render 3D, etc.)
CREATE TABLE IF NOT EXISTS rya.perfiles_asesoria (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    familia_id UUID NOT NULL REFERENCES rya.familias_perfil_asesoria(id) ON DELETE RESTRICT,
    codigo VARCHAR(60) NOT NULL UNIQUE,
    nombre VARCHAR(140) NOT NULL,
    descripcion TEXT,
    prioridad_cpu INTEGER NOT NULL DEFAULT 5, -- 1 a 10
    prioridad_gpu INTEGER NOT NULL DEFAULT 5, -- 1 a 10
    ram_minima_gb INTEGER NOT NULL DEFAULT 8,
    requiere_gpu_dedicada BOOLEAN NOT NULL DEFAULT false,
    presupuesto_sugerido_min NUMERIC(14,2) DEFAULT 0,
    presupuesto_sugerido_max NUMERIC(14,2) DEFAULT 0,
    activo BOOLEAN NOT NULL DEFAULT true,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. Necesidades específicas dentro de un perfil (Opcional por perfil)
CREATE TABLE IF NOT EXISTS rya.necesidades_perfil (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    perfil_id UUID NOT NULL REFERENCES rya.perfiles_asesoria(id) ON DELETE CASCADE,
    codigo VARCHAR(60),
    nombre VARCHAR(140) NOT NULL,
    descripcion TEXT,
    posicion INTEGER NOT NULL DEFAULT 0,
    activo BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 4. Reglas de distribución porcentual de presupuesto por tipo de componente
CREATE TABLE IF NOT EXISTS rya.reglas_presupuesto_perfil (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    perfil_id UUID NOT NULL REFERENCES rya.perfiles_asesoria(id) ON DELETE CASCADE,
    tipo_componente_codigo VARCHAR(50) NOT NULL, -- CPU, PLACA, RAM, GPU, FUENTE, GABINETE, etc.
    porcentaje_min NUMERIC(5,2) NOT NULL DEFAULT 0, -- Ej: 20.00 (%)
    porcentaje_max NUMERIC(5,2) NOT NULL DEFAULT 0, -- Ej: 35.00 (%)
    obligatorio BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_perfiles_asesoria_familia ON rya.perfiles_asesoria(familia_id);
CREATE INDEX IF NOT EXISTS idx_perfiles_asesoria_activo ON rya.perfiles_asesoria(activo, is_deleted);
CREATE INDEX IF NOT EXISTS idx_necesidades_perfil ON rya.necesidades_perfil(perfil_id);
CREATE INDEX IF NOT EXISTS idx_reglas_presupuesto_perfil ON rya.reglas_presupuesto_perfil(perfil_id);
