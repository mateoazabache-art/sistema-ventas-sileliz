-- =====================================================
-- 00_roles.sql
-- Usuario de base de datos para la aplicacion (ventas_app)
-- Base de datos: ventas_db
-- Autor: Rafael
-- Ejecutar conectado como postgres, ANTES de 01_crear_tablas.sql.
-- Se puede ejecutar completo las veces que haga falta.
-- =====================================================
-- Por que un usuario aparte (seccion 6 del plan, "Privilegios excesivos"):
-- la aplicacion Java NO se conecta como postgres (superusuario).
-- ventas_app solo puede leer y escribir datos: no puede crear,
-- borrar ni modificar tablas. Si alguien encuentra una falla en el
-- sistema, el dano posible es mucho menor.
-- =====================================================

-- 1. Crear el usuario solo si no existe ---------------
--    (los roles son de todo el servidor, no de una base;
--     CREATE ROLE fallaria si ya existe)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'ventas_app') THEN
        CREATE ROLE ventas_app LOGIN;
    END IF;
END
$$;

-- 2. Permiso para conectarse a la base y usar el esquema --
GRANT CONNECT ON DATABASE ventas_db TO ventas_app;
GRANT USAGE ON SCHEMA public TO ventas_app;

-- 3. Permisos sobre las tablas y secuencias que YA existen --
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO ventas_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO ventas_app;

-- 4. Permisos automaticos para las tablas y secuencias que se creen
--    DESPUES (01_crear_tablas.sql y los scripts siguientes).
--    Aplica a lo que cree el usuario que ejecuta este script (postgres).
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO ventas_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO ventas_app;

-- =====================================================
-- 5. CLAVE DEL USUARIO (NO va en este archivo)
-- =====================================================
-- Cada integrante pone su propia clave local ejecutando
-- en pgAdmin, una sola vez, esta linea (sin los guiones):
--
--   ALTER ROLE ventas_app PASSWORD 'escribe_aqui_tu_clave';
--
-- Esa misma clave va en tu config.properties (que NO se sube a Git).
-- Nunca escribas la clave real en este archivo.
-- =====================================================

-- 6. Comprobacion -------------------------------------
SELECT rolname, rolcanlogin, rolsuper, rolcreatedb, rolcreaterole
FROM pg_roles
WHERE rolname = 'ventas_app';
-- Esperado: ventas_app | true | false | false | false
