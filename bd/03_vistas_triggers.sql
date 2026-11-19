-- =====================================================================
-- 03_vistas_triggers.sql  -  Gestion de Ventas Sileliz.mas
-- Plan v3.1, seccion 4.4 y tarea 12 de Rafael.
--
-- Se ejecuta DESPUES de 00_roles, 01_crear_tablas y 02_datos_prueba,
-- conectado a ventas_db como postgres (crea objetos: necesita DDL).
--
-- Contenido:
--   1. trg_detalle_venta_inmutable: un detalle emitido no se edita ni se borra.
--   2. trg_venta_protegida: una venta no se borra; solo se permite pasar
--      de EMITIDA a ANULADA (con motivo, lo exige el CHECK de 01).
--   3. Vistas v_stock_bajo, v_ventas_resumen y v_mas_vendidos.
--
-- Regla unica del stock (4.4): NINGUN trigger toca el stock. El stock solo
-- cambia en VarianteDao.descontarStock / reponerStock dentro de la venta.
--
-- Se puede ejecutar varias veces (CREATE OR REPLACE / DROP IF EXISTS).
-- Los mensajes de error llevan el nombre del trigger entre comillas para que
-- ErrorSql muestre un mensaje claro (SQLState 23514 -> HTTP 400).
-- =====================================================================


-- =====================================================
-- BLOQUE 1: DETALLE DE VENTA INMUTABLE (RN-09)
-- =====================================================
CREATE OR REPLACE FUNCTION fn_detalle_venta_inmutable()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'Operacion no permitida por "trg_detalle_venta_inmutable": el detalle de una venta no se modifica ni se elimina.'
        USING ERRCODE = 'check_violation';
END;
$$;

DROP TRIGGER IF EXISTS trg_detalle_venta_inmutable ON detalle_venta;
CREATE TRIGGER trg_detalle_venta_inmutable
    BEFORE UPDATE OR DELETE ON detalle_venta
    FOR EACH ROW
    EXECUTE FUNCTION fn_detalle_venta_inmutable();


-- =====================================================
-- BLOQUE 2: VENTA PROTEGIDA (RN-05 y RN-08)
-- =====================================================
-- DELETE: nunca. UPDATE: solo EMITIDA -> ANULADA, sin tocar ninguna otra
-- columna (totales, fecha, cliente, vendedor, serie y numero quedan igual).
CREATE OR REPLACE FUNCTION fn_venta_protegida()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'Operacion no permitida por "trg_venta_protegida": una venta no se elimina, se anula.'
            USING ERRCODE = 'check_violation';
    END IF;

    -- TG_OP = 'UPDATE'
    IF OLD.estado = 'EMITIDA'
       AND NEW.estado = 'ANULADA'
       AND (NEW.serie, NEW.numero, NEW.fecha_hora, NEW.id_cliente, NEW.id_usuario,
            NEW.base_imponible, NEW.descuento, NEW.igv, NEW.total, NEW.metodo_pago)
           IS NOT DISTINCT FROM
           (OLD.serie, OLD.numero, OLD.fecha_hora, OLD.id_cliente, OLD.id_usuario,
            OLD.base_imponible, OLD.descuento, OLD.igv, OLD.total, OLD.metodo_pago)
    THEN
        RETURN NEW;     -- anulacion valida (el CHECK de 01 exige el motivo)
    END IF;

    RAISE EXCEPTION 'Operacion no permitida por "trg_venta_protegida": una venta emitida solo puede anularse.'
        USING ERRCODE = 'check_violation';
END;
$$;

DROP TRIGGER IF EXISTS trg_venta_protegida ON venta;
CREATE TRIGGER trg_venta_protegida
    BEFORE UPDATE OR DELETE ON venta
    FOR EACH ROW
    EXECUTE FUNCTION fn_venta_protegida();

-- Nota: TRUNCATE no dispara triggers FOR EACH ROW, por eso 02_datos_prueba.sql
-- se puede volver a ejecutar para limpiar la base de desarrollo.


-- =====================================================
-- BLOQUE 3: VISTAS (las usan Reportes e Inicio)
-- =====================================================

-- 3.1 Variantes activas con stock en el minimo o por debajo (RN-10)
CREATE OR REPLACE VIEW v_stock_bajo AS
SELECT v.id_variante,
       v.sku,
       p.id_producto,
       p.codigo,
       p.nombre       AS producto,
       c.nombre       AS categoria,
       v.talla,
       v.color,
       v.stock,
       v.stock_minimo
FROM variante_producto v
JOIN producto  p ON p.id_producto  = v.id_producto
JOIN categoria c ON c.id_categoria = p.id_categoria
WHERE v.activo
  AND p.activo
  AND v.stock <= v.stock_minimo;

-- 3.2 Una fila por venta con cliente y vendedor (historial, reportes y comprobante)
--     Sin cliente = consumidor final -> "Varios" (RN-06)
CREATE OR REPLACE VIEW v_ventas_resumen AS
SELECT ve.id_venta,
       ve.serie,
       ve.numero,
       ve.serie || '-' || lpad(ve.numero::text, 6, '0')                  AS comprobante,
       ve.fecha_hora,
       ve.id_cliente,
       COALESCE(pc.nombres || ' ' || pc.apellidos, 'Varios')             AS cliente,
       pc.dni                                                            AS dni_cliente,
       ve.id_usuario,
       pu.nombres || ' ' || pu.apellidos                                 AS vendedor,
       ve.metodo_pago,
       ve.base_imponible,
       ve.descuento,
       ve.igv,
       ve.total,
       ve.estado,
       ve.motivo_anulacion
FROM venta ve
JOIN persona pu      ON pu.id_persona = ve.id_usuario
LEFT JOIN persona pc ON pc.id_persona = ve.id_cliente;

-- 3.3 Unidades e importe vendidos por variante, sin contar ventas anuladas
--     (acumulado historico; ReporteDao filtra por fechas con su propia consulta)
CREATE OR REPLACE VIEW v_mas_vendidos AS
SELECT d.id_variante,
       v.sku,
       p.nombre                 AS producto,
       v.talla,
       v.color,
       SUM(d.cantidad)          AS unidades,
       SUM(d.subtotal)          AS importe
FROM detalle_venta d
JOIN venta             ve ON ve.id_venta   = d.id_venta
JOIN variante_producto v  ON v.id_variante = d.id_variante
JOIN producto          p  ON p.id_producto = v.id_producto
WHERE ve.estado = 'EMITIDA'
GROUP BY d.id_variante, v.sku, p.nombre, v.talla, v.color;

-- La aplicacion solo LEE las vistas
GRANT SELECT ON v_stock_bajo, v_ventas_resumen, v_mas_vendidos TO ventas_app;


-- =====================================================
-- BLOQUE 4: COMPROBACION
-- =====================================================
-- Debe listar 2 triggers y 3 vistas
SELECT 'trigger' AS tipo, tgname AS nombre
FROM pg_trigger
WHERE tgname IN ('trg_detalle_venta_inmutable', 'trg_venta_protegida')
UNION ALL
SELECT 'vista', table_name
FROM information_schema.views
WHERE table_schema = 'public'
  AND table_name IN ('v_stock_bajo', 'v_ventas_resumen', 'v_mas_vendidos')
ORDER BY 1, 2;
