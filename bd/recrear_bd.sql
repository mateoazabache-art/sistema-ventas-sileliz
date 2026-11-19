-- =====================================================================
-- recrear_bd.sql  -  Gestion de Ventas Sileliz.mas  (tarea 14 de Rafael)
--
-- NO se ejecuta a mano: lo llaman recrear_bd.cmd (Windows) y recrear_bd.sh (Mac).
-- Borra ventas_db y la vuelve a crear DESDE CERO con los scripts en orden:
--     00_roles -> clave de ventas_app -> 01_crear_tablas -> 02_datos_prueba
--     -> 03_vistas_triggers -> (opcional) ventas_demo_reportes
--
-- Variables que recibe de recrear_bd.cmd / .sh:
--     clave  = la clave de ventas_app, leida de config.properties (db.clave)
--     demo   = true para cargar tambien las 14 ventas de ejemplo
--
-- Plan 4.5: los scripts ya ejecutados no se editan; este archivo solo los llama.
-- =====================================================================

\set ON_ERROR_STOP on
\set QUIET on

\echo ''
\echo '== 1/6 Borrando y creando ventas_db (UTF-8) =='
DROP DATABASE IF EXISTS ventas_db WITH (FORCE);
CREATE DATABASE ventas_db ENCODING 'UTF8' TEMPLATE template0;

\connect ventas_db

\echo '== 2/6 00_roles.sql (usuario ventas_app y permisos) =='
\ir 00_roles.sql
ALTER ROLE ventas_app PASSWORD :'clave';
\echo '   Clave de ventas_app = la de config.properties'

\echo '== 3/6 01_crear_tablas.sql (9 tablas y seq_correlativo) =='
\ir 01_crear_tablas.sql

\echo '== 4/6 02_datos_prueba.sql (debe salir 5 | 20 | 60 | 10 | 2) =='
\ir 02_datos_prueba.sql

\echo '== 5/6 03_vistas_triggers.sql (2 triggers y 3 vistas) =='
\ir 03_vistas_triggers.sql

\if :demo
    \echo '== 6/6 ventas_demo_reportes.sql (14 ventas de ejemplo) =='
    \ir ventas_demo_reportes.sql
\else
    \echo '== 6/6 Sin ventas de ejemplo (para cargarlas: recrear_bd demo) =='
\endif

\unset QUIET
\echo ''
\echo '================ COMPROBACION FINAL ================'
SELECT (SELECT count(*) FROM categoria)         AS categorias,
       (SELECT count(*) FROM producto)          AS productos,
       (SELECT count(*) FROM variante_producto) AS variantes,
       (SELECT count(*) FROM cliente)           AS clientes,
       (SELECT count(*) FROM usuario)           AS usuarios,
       (SELECT count(*) FROM venta)             AS ventas,
       (SELECT count(*) FROM pg_trigger
          WHERE tgname IN ('trg_detalle_venta_inmutable', 'trg_venta_protegida')) AS triggers,
       (SELECT count(*) FROM information_schema.views
          WHERE table_schema = 'public'
            AND table_name IN ('v_stock_bajo', 'v_ventas_resumen', 'v_mas_vendidos')) AS vistas;
\echo 'Esperado: 5 | 20 | 60 | 10 | 2 | 0 (o 14 con demo) | 2 | 3'
