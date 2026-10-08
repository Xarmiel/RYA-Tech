package com.ecommerce.backend.config;

import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Arrays;

@Slf4j
@Configuration(proxyBeanMethods = false)
public class DatabaseMigrationConfig {

    public static class DatabaseMigrationRunner {
        public DatabaseMigrationRunner(DataSource dataSource) {
            log.info("Iniciando migración automática de base de datos desde el backend...");
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS public.rol (
                        id SERIAL PRIMARY KEY,
                        nombre VARCHAR(20) NOT NULL UNIQUE
                    );
                """);
                log.info("Verificación/Creación de tabla 'rol' completada.");

                stmt.execute("""
                    INSERT INTO public.rol (nombre) VALUES ('CLIENT') ON CONFLICT (nombre) DO NOTHING;
                    INSERT INTO public.rol (nombre) VALUES ('ADMIN') ON CONFLICT (nombre) DO NOTHING;
                """);
                log.info("Roles 'CLIENT' y 'ADMIN' asegurados en la tabla 'rol'.");

                stmt.execute("""
                    DO $$ 
                    BEGIN 
                        -- Eliminar la columna 'rol' tipo varchar si existe
                        IF EXISTS (
                            SELECT 1 FROM information_schema.columns 
                            WHERE table_schema = 'public' AND table_name = 'usuarios' AND column_name = 'rol'
                        ) THEN 
                            ALTER TABLE public.usuarios DROP COLUMN rol; 
                        END IF; 

                        -- Crear columna 'rol_id' si no existe
                        IF NOT EXISTS (
                            SELECT 1 FROM information_schema.columns 
                            WHERE table_schema = 'public' AND table_name = 'usuarios' AND column_name = 'rol_id'
                        ) THEN 
                            ALTER TABLE public.usuarios ADD COLUMN rol_id INT; 
                        END IF; 

                        -- Agregar FK hacia rol(id) si no existe
                        IF NOT EXISTS (
                            SELECT 1 FROM information_schema.table_constraints 
                            WHERE constraint_schema = 'public' AND table_name = 'usuarios' AND constraint_name = 'fk_usuarios_rol'
                        ) THEN 
                            ALTER TABLE public.usuarios 
                            ADD CONSTRAINT fk_usuarios_rol 
                            FOREIGN KEY (rol_id) REFERENCES public.rol(id); 
                        END IF;

                        -- Asegurar NOT NULL en rol_id
                        ALTER TABLE public.usuarios ALTER COLUMN rol_id SET NOT NULL;
                    END $$;
                """);
                log.info("Migración de esquema completada: columna 'rol_id' con FK hacia tabla 'rol' lista.");

                stmt.execute("""
                    DO $$ 
                    BEGIN
                        -- Si existe la categoría 'Hardware' y no existe todavía 'Componente'
                        IF EXISTS (SELECT 1 FROM public.categorias WHERE nombre = 'Hardware' OR slug = 'hardware') THEN
                            IF NOT EXISTS (SELECT 1 FROM public.categorias WHERE slug = 'componente') THEN
                                UPDATE public.categorias SET nombre = 'Componente', slug = 'componente' WHERE nombre = 'Hardware' OR slug = 'hardware';
                            ELSE
                                -- Reasignar hijos de 'Hardware' a 'Componente' y eliminar 'Hardware'
                                UPDATE public.categorias SET parent_id = (SELECT id FROM public.categorias WHERE slug = 'componente' LIMIT 1)
                                WHERE parent_id IN (SELECT id FROM public.categorias WHERE nombre = 'Hardware' OR slug = 'hardware');
                                
                                UPDATE public.productos SET categoria_id = (SELECT id FROM public.categorias WHERE slug = 'componente' LIMIT 1)
                                WHERE categoria_id IN (SELECT id FROM public.categorias WHERE nombre = 'Hardware' OR slug = 'hardware');

                                DELETE FROM public.categorias WHERE nombre = 'Hardware' OR slug = 'hardware';
                            END IF;
                        END IF;

                        -- Si existe la categoría 'Periféricos y Accesorios' y no existe todavía 'Periféricos'
                        IF EXISTS (SELECT 1 FROM public.categorias WHERE nombre = 'Periféricos y Accesorios' OR slug = 'perifericos-y-accesorios') THEN
                            IF NOT EXISTS (SELECT 1 FROM public.categorias WHERE slug = 'perifericos') THEN
                                UPDATE public.categorias SET nombre = 'Periféricos', slug = 'perifericos' WHERE nombre = 'Periféricos y Accesorios' OR slug = 'perifericos-y-accesorios';
                            ELSE
                                UPDATE public.categorias SET parent_id = (SELECT id FROM public.categorias WHERE slug = 'perifericos' LIMIT 1)
                                WHERE parent_id IN (SELECT id FROM public.categorias WHERE nombre = 'Periféricos y Accesorios' OR slug = 'perifericos-y-accesorios');

                                UPDATE public.productos SET categoria_id = (SELECT id FROM public.categorias WHERE slug = 'perifericos' LIMIT 1)
                                WHERE categoria_id IN (SELECT id FROM public.categorias WHERE nombre = 'Periféricos y Accesorios' OR slug = 'perifericos-y-accesorios');

                                DELETE FROM public.categorias WHERE nombre = 'Periféricos y Accesorios' OR slug = 'perifericos-y-accesorios';
                            END IF;
                        END IF;
                    END $$;
                """);
                log.info("Actualización de categorías 'Componente' y 'Periféricos' realizada con éxito.");

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS public.especialidades (
                        id SERIAL PRIMARY KEY,
                        nombre VARCHAR(100) NOT NULL UNIQUE,
                        campo VARCHAR(100) NOT NULL
                    );

                    CREATE TABLE IF NOT EXISTS public.perfiles_hardware (
                        id SERIAL PRIMARY KEY,
                        especialidad_id INT NOT NULL UNIQUE REFERENCES public.especialidades(id) ON DELETE CASCADE,
                        prio_cpu SMALLINT NOT NULL,
                        prio_gpu SMALLINT NOT NULL,
                        prio_disco SMALLINT NOT NULL,
                        prio_ram SMALLINT NOT NULL,
                        prio_fuente SMALLINT NOT NULL,
                        prio_placa SMALLINT NOT NULL,
                        prio_case SMALLINT NOT NULL,
                        requisitos_tecnicos JSONB,
                        descripcion TEXT
                    );
                """);
                log.info("Tablas 'especialidades' y 'perfiles_hardware' aseguradas en base de datos.");

            } catch (Exception e) {
                log.error("Error durante la migración de base de datos desde el backend: {}", e.getMessage(), e);
                throw new RuntimeException("Error en la migración de base de datos", e);
            }
        }
    }

    @Bean
    public DatabaseMigrationRunner databaseMigrationRunner(DataSource dataSource) {
        return new DatabaseMigrationRunner(dataSource);
    }

    @Bean
    public static BeanFactoryPostProcessor entityManagerDependsOnMigrationPostProcessor() {
        return (ConfigurableListableBeanFactory beanFactory) -> {
            String[] entityManagerFactoryNames = beanFactory.getBeanNamesForType(EntityManagerFactory.class, true, false);
            for (String emName : entityManagerFactoryNames) {
                BeanDefinition bd = beanFactory.getBeanDefinition(emName);
                String[] dependsOn = bd.getDependsOn();
                if (dependsOn == null || dependsOn.length == 0) {
                    bd.setDependsOn("databaseMigrationRunner");
                } else {
                    String[] newDependsOn = Arrays.copyOf(dependsOn, dependsOn.length + 1);
                    newDependsOn[dependsOn.length] = "databaseMigrationRunner";
                    bd.setDependsOn(newDependsOn);
                }
            }
        };
    }
}
