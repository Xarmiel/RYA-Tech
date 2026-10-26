CREATE SCHEMA IF NOT EXISTS rya;
CREATE SCHEMA IF NOT EXISTS auth;

CREATE TABLE IF NOT EXISTS auth.users (
  id uuid PRIMARY KEY,
  instance_id uuid,
  aud varchar(255),
  role varchar(255),
  email varchar(255) NOT NULL,
  encrypted_password varchar(255),
  email_confirmed_at timestamp with time zone,
  raw_app_meta_data json,
  raw_user_meta_data json,
  is_sso_user boolean DEFAULT false,
  is_anonymous boolean DEFAULT false,
  created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS rya.usuarios (
  id uuid PRIMARY KEY,
  nombre varchar(140) NOT NULL,
  email varchar(254) NOT NULL,
  telefono varchar(30),
  estado varchar(24) NOT NULL DEFAULT 'ACTIVO',
  email_verificado_at timestamp with time zone,
  is_deleted boolean NOT NULL DEFAULT false,
  created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);

-- Normalized technical records are read by the administrative product detail.
CREATE TABLE IF NOT EXISTS rya.fuentes_tecnicas (
 id uuid PRIMARY KEY, titulo text, url text
);
CREATE TABLE IF NOT EXISTS rya.especificaciones_cpu (
 producto_id uuid PRIMARY KEY, modelo text, socket text, familia text, generacion text,
 nucleos integer, hilos integer, frecuencia_base_mhz integer, frecuencia_maxima_mhz integer,
 cache_l1_kb numeric, cache_l2_kb numeric, cache_l3_kb numeric,
 potencia_base_w numeric, potencia_maxima_w numeric, tiene_graficos_integrados boolean,
 modelo_graficos text, fuente_id uuid, verificada_at timestamp with time zone,
 updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS rya.revisiones_placa (
 id uuid PRIMARY KEY, producto_id uuid, modelo text, revision text, socket text, chipset text,
 fuente_id uuid, verificada_at timestamp with time zone
);

CREATE TABLE IF NOT EXISTS rya.categorias_soporte (
  id uuid PRIMARY KEY, nombre varchar(120) NOT NULL, descripcion text,
  is_deleted boolean NOT NULL DEFAULT false
);
CREATE TABLE IF NOT EXISTS rya.preguntas_frecuentes (
  id uuid PRIMARY KEY, categoria_id uuid, pregunta text NOT NULL, respuesta text NOT NULL,
  posicion integer NOT NULL DEFAULT 0, publicada boolean NOT NULL DEFAULT true,
  is_deleted boolean NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS rya.pedidos (
  id uuid PRIMARY KEY, usuario_id uuid NOT NULL, codigo varchar(35) NOT NULL UNIQUE,
  estado varchar(35) NOT NULL DEFAULT 'PENDIENTE', moneda char(3) NOT NULL DEFAULT 'PEN',
  subtotal numeric(14,2) NOT NULL DEFAULT 0, descuento numeric(14,2) NOT NULL DEFAULT 0,
  envio numeric(14,2) NOT NULL DEFAULT 0, monto_total numeric(14,2) NOT NULL,
  metodo_pago_snapshot varchar(65), created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
  is_deleted boolean NOT NULL DEFAULT false
);
CREATE TABLE IF NOT EXISTS rya.detalle_pedido (
  id uuid PRIMARY KEY, pedido_id uuid NOT NULL, producto_id uuid,
  descripcion_snapshot varchar(260) NOT NULL, sku_snapshot varchar(85), cantidad integer NOT NULL,
  precio_unitario numeric(14,2) NOT NULL, descuento_unitario numeric(14,2) NOT NULL DEFAULT 0,
  subtotal numeric(14,2) GENERATED ALWAYS AS (cantidad*(precio_unitario-descuento_unitario)),
  created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP, is_deleted boolean NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS rya.promociones (
  id uuid PRIMARY KEY, nombre varchar(180) NOT NULL, inicio_at timestamp with time zone,
  fin_at timestamp with time zone, descuento_porcentaje numeric(5,2),
  activa boolean NOT NULL DEFAULT true, is_deleted boolean NOT NULL DEFAULT false
);
CREATE TABLE IF NOT EXISTS rya.promociones_productos (
  id uuid PRIMARY KEY, promocion_id uuid NOT NULL, producto_id uuid NOT NULL,
  precio_anterior numeric(14,2) NOT NULL, is_deleted boolean NOT NULL DEFAULT false,
  created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(promocion_id,producto_id)
);

CREATE TABLE IF NOT EXISTS rya.marcas (
  id uuid PRIMARY KEY,
  nombre varchar(130) NOT NULL,
  slug varchar(150) NOT NULL,
  logo_url text,
  descripcion text,
  created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
  is_deleted boolean NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS rya.categorias (
  id uuid PRIMARY KEY,
  parent_id uuid,
  nombre varchar(120) NOT NULL,
  slug varchar(150) NOT NULL,
  acronimo varchar(20),
  posicion integer NOT NULL DEFAULT 0,
  created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
  is_deleted boolean NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS rya.tipos_producto (
  id uuid PRIMARY KEY,
  codigo varchar(50) NOT NULL,
  descripcion text,
  created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
  is_deleted boolean NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS rya.productos (
  id uuid PRIMARY KEY,
  categoria_id uuid NOT NULL,
  marca_id uuid,
  tipo_id uuid NOT NULL,
  sku varchar(80) NOT NULL,
  nombre varchar(220) NOT NULL,
  slug varchar(240) NOT NULL,
  descripcion text,
  precio_base numeric(14,2) NOT NULL,
  costo_referencia numeric(14,2),
  activo boolean NOT NULL DEFAULT true,
  vendible boolean NOT NULL DEFAULT true,
  requiere_serie boolean NOT NULL DEFAULT false,
  requiere_envio boolean NOT NULL DEFAULT true,
  created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
  is_deleted boolean NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS rya.imagenes_producto (
  id uuid PRIMARY KEY,
  producto_id uuid NOT NULL,
  url text NOT NULL,
  texto_alternativo varchar(220),
  posicion integer NOT NULL DEFAULT 0,
  principal boolean NOT NULL DEFAULT false,
  created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
  is_deleted boolean NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS rya.almacenes (
  id uuid PRIMARY KEY,
  nombre varchar(150) NOT NULL,
  codigo varchar(40) NOT NULL UNIQUE,
  direccion text,
  activo boolean NOT NULL DEFAULT true,
  created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
  is_deleted boolean NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS rya.existencias (
  id uuid PRIMARY KEY,
  almacen_id uuid NOT NULL,
  producto_id uuid NOT NULL,
  variante_id uuid,
  cantidad integer NOT NULL DEFAULT 0,
  reservado integer NOT NULL DEFAULT 0,
  version integer NOT NULL DEFAULT 0,
  created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
  is_deleted boolean NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS rya.movimientos_inventario (
  id uuid PRIMARY KEY,
  existencia_id uuid NOT NULL,
  tipo varchar(30) NOT NULL,
  cantidad integer NOT NULL,
  referencia_tipo varchar(60),
  referencia_id uuid,
  nota text,
  registrado_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS rya.atributos_tecnicos (
  id uuid PRIMARY KEY,
  codigo varchar(90) NOT NULL UNIQUE,
  nombre varchar(120) NOT NULL,
  tipo_dato varchar(20) NOT NULL,
  unidad varchar(30),
  filtrable boolean NOT NULL DEFAULT true,
  is_deleted boolean NOT NULL DEFAULT false,
  created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS rya.valores_atributo_producto (
  id uuid PRIMARY KEY,
  producto_id uuid NOT NULL,
  atributo_id uuid NOT NULL,
  valor_texto varchar(250),
  valor_numero numeric(16,4),
  valor_booleano boolean,
  is_deleted boolean NOT NULL DEFAULT false,
  created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS rya.familias_perfil_asesoria (
  id uuid PRIMARY KEY,
  codigo varchar(60) NOT NULL UNIQUE,
  nombre varchar(120) NOT NULL,
  descripcion text,
  icono varchar(60) DEFAULT 'tune',
  posicion integer NOT NULL DEFAULT 0,
  activo boolean NOT NULL DEFAULT true,
  is_deleted boolean NOT NULL DEFAULT false,
  created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS rya.perfiles_asesoria (
  id uuid PRIMARY KEY,
  familia_id uuid NOT NULL,
  codigo varchar(60) NOT NULL UNIQUE,
  nombre varchar(140) NOT NULL,
  descripcion text,
  prioridad_cpu integer NOT NULL DEFAULT 5,
  prioridad_gpu integer NOT NULL DEFAULT 5,
  ram_minima_gb integer NOT NULL DEFAULT 8,
  requiere_gpu_dedicada boolean NOT NULL DEFAULT false,
  presupuesto_sugerido_min numeric(14,2) DEFAULT 0,
  presupuesto_sugerido_max numeric(14,2) DEFAULT 0,
  activo boolean NOT NULL DEFAULT true,
  is_deleted boolean NOT NULL DEFAULT false,
  created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS rya.necesidades_perfil (
  id uuid PRIMARY KEY,
  perfil_id uuid NOT NULL,
  codigo varchar(60),
  nombre varchar(140) NOT NULL,
  descripcion text,
  posicion integer NOT NULL DEFAULT 0,
  activo boolean NOT NULL DEFAULT true,
  created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS rya.reglas_presupuesto_perfil (
  id uuid PRIMARY KEY,
  perfil_id uuid NOT NULL,
  tipo_componente_codigo varchar(50) NOT NULL,
  porcentaje_min numeric(5,2) NOT NULL DEFAULT 0,
  porcentaje_max numeric(5,2) NOT NULL DEFAULT 0,
  obligatorio boolean NOT NULL DEFAULT true,
  created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);

