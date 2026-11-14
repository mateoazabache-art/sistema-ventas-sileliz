-- =====================================================================
-- ventas_demo_reportes.sql  -  Gestion de Ventas Sileliz.mas
--
-- SOLO PARA DESARROLLO. Crea 14 ventas de ejemplo (una anulada) para ver
-- reportes.html y el Inicio con datos mientras no existe VentaService.
--
-- Se ejecuta en pgAdmin, conectado a ventas_db, DESPUES de 00, 01, 02 y 03.
-- Las fechas son relativas a HOY: 12 ventas en los ultimos 10 dias
-- (3 de ellas hoy) y 2 de hace 40 y 45 dias (para probar el filtro).
--
-- Igual que una venta real: descuenta el stock y deja el movimiento en
-- movimiento_stock. Los importes siguen la RN-01 (precio con IGV incluido).
--
-- No estorba a las pruebas JUnit: usa solo variantes con stock de sobra,
-- no toca las que usan las pruebas (P-001-XL-BLA, P-003-XL-CEL) y ninguna
-- baja al stock minimo (v_stock_bajo sigue en 13).
--
-- PARA BORRARLAS: las ventas no se eliminan (trg_venta_protegida).
-- Se limpia volviendo a ejecutar 02_datos_prueba.sql (usa TRUNCATE).
-- =====================================================================

BEGIN;

-- Seguro: si ya se ejecuto antes, se detiene para no duplicar las ventas.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM movimiento_stock WHERE motivo = 'Venta de ejemplo') THEN
        RAISE EXCEPTION 'Las ventas de ejemplo ya estan cargadas. Para empezar de cero ejecute 02_datos_prueba.sql.';
    END IF;
END
$$;

-- Ayudante temporal: desaparece al cerrar la conexion.
CREATE FUNCTION pg_temp.vender(
    p_fecha      timestamp,
    p_usuario    text,          -- 'admin' o 'vendedor'
    p_dni        text,          -- NULL = consumidor final ("Varios")
    p_pago       text,
    p_desc_pct   numeric,       -- 0 o 10
    p_variantes  int[],
    p_cantidades int[]
) RETURNS int
LANGUAGE plpgsql
AS $$
DECLARE
    v_usuario int;
    v_cliente int;
    v_bruto   numeric(10,2) := 0;
    v_desc    numeric(10,2);
    v_total   numeric(10,2);
    v_base    numeric(10,2);
    v_venta   int;
    v_precio  numeric(10,2);
    v_texto   text;
    i         int;
BEGIN
    SELECT id_persona INTO STRICT v_usuario FROM usuario WHERE nombre_usuario = p_usuario;
    IF p_dni IS NOT NULL THEN
        SELECT id_persona INTO STRICT v_cliente FROM persona WHERE dni = p_dni;
    END IF;

    FOR i IN 1 .. array_length(p_variantes, 1) LOOP
        SELECT p.precio_venta INTO STRICT v_precio
        FROM variante_producto v JOIN producto p ON p.id_producto = v.id_producto
        WHERE v.id_variante = p_variantes[i];
        v_bruto := v_bruto + v_precio * p_cantidades[i];
    END LOOP;

    v_desc  := round(v_bruto * p_desc_pct / 100, 2);
    v_total := v_bruto - v_desc;
    v_base  := round(v_total / 1.18, 2);

    INSERT INTO venta (fecha_hora, id_cliente, id_usuario, base_imponible, descuento, igv, total, metodo_pago)
    VALUES (p_fecha, v_cliente, v_usuario, v_base, v_desc, v_total - v_base, v_total, p_pago)
    RETURNING id_venta INTO v_venta;

    FOR i IN 1 .. array_length(p_variantes, 1) LOOP
        SELECT p.precio_venta, p.nombre || ' ' || v.talla || ' ' || v.color
        INTO STRICT v_precio, v_texto
        FROM variante_producto v JOIN producto p ON p.id_producto = v.id_producto
        WHERE v.id_variante = p_variantes[i];

        UPDATE variante_producto SET stock = stock - p_cantidades[i]
        WHERE id_variante = p_variantes[i] AND stock >= p_cantidades[i];
        IF NOT FOUND THEN
            RAISE EXCEPTION 'Sin stock suficiente en la variante % (%). No se creo ninguna venta.',
                p_variantes[i], v_texto;
        END IF;

        INSERT INTO detalle_venta (id_venta, id_variante, descripcion, cantidad, precio_unitario, subtotal)
        VALUES (v_venta, p_variantes[i], v_texto, p_cantidades[i], v_precio, v_precio * p_cantidades[i]);

        INSERT INTO movimiento_stock (id_variante, id_venta, id_usuario, tipo, cantidad, fecha_hora, motivo)
        VALUES (p_variantes[i], v_venta, v_usuario, 'VENTA', -p_cantidades[i], p_fecha, 'Venta de ejemplo');
    END LOOP;

    RETURN v_venta;
END;
$$;

-- Ventas (fecha, usuario, DNI del cliente, pago, % descuento, variantes, cantidades)
SELECT pg_temp.vender(current_date - 45 + time '11:20', 'vendedor', '70000002', 'EFECTIVO',      0, ARRAY[4],          ARRAY[1]);
SELECT pg_temp.vender(current_date - 40 + time '17:05', 'admin',    NULL,       'YAPE',          0, ARRAY[15, 22],     ARRAY[1, 1]);
SELECT pg_temp.vender(current_date - 10 + time '10:15', 'vendedor', '70000001', 'EFECTIVO',      0, ARRAY[11],         ARRAY[2]);
SELECT pg_temp.vender(current_date -  9 + time '12:40', 'vendedor', NULL,       'YAPE',          0, ARRAY[9, 16],      ARRAY[1, 1]);
SELECT pg_temp.vender(current_date -  8 + time '16:30', 'admin',    '70000003', 'TARJETA',      10, ARRAY[11, 26, 32], ARRAY[1, 1, 1]);
SELECT pg_temp.vender(current_date -  7 + time '11:05', 'vendedor', '70000005', 'PLIN',          0, ARRAY[10],         ARRAY[1]);
SELECT pg_temp.vender(current_date -  6 + time '18:20', 'vendedor', NULL,       'EFECTIVO',      0, ARRAY[21],         ARRAY[2]);
SELECT pg_temp.vender(current_date -  4 + time '10:50', 'admin',    '70000007', 'TRANSFERENCIA', 0, ARRAY[34, 40],     ARRAY[1, 1]);
SELECT pg_temp.vender(current_date -  3 + time '15:10', 'vendedor', '70000008', 'YAPE',         10, ARRAY[11, 9],      ARRAY[2, 1]);
SELECT pg_temp.vender(current_date -  2 + time '13:25', 'vendedor', NULL,       'EFECTIVO',      0, ARRAY[43],         ARRAY[1]);
SELECT pg_temp.vender(current_date -  1 + time '17:45', 'admin',    '70000004', 'TARJETA',       0, ARRAY[48, 54],     ARRAY[1, 1]);
SELECT pg_temp.vender(current_date      + time '09:10', 'vendedor', '70000006', 'YAPE',          0, ARRAY[9],          ARRAY[1]);
SELECT pg_temp.vender(current_date      + time '09:40', 'vendedor', NULL,       'EFECTIVO',      0, ARRAY[55],         ARRAY[1]);
SELECT pg_temp.vender(current_date      + time '10:05', 'admin',    '70000001', 'PLIN',          0, ARRAY[28, 60],     ARRAY[1, 1]);

-- Anular la venta de hace 6 dias (RN-08): cambia el estado, devuelve el stock
-- y deja el movimiento ANULACION.
WITH anulada AS (
    UPDATE venta
    SET estado = 'ANULADA', motivo_anulacion = 'Ejemplo: la clienta cambió de opinión'
    WHERE fecha_hora = current_date - 6 + time '18:20' AND estado = 'EMITIDA'
    RETURNING id_venta
), repuesto AS (
    UPDATE variante_producto v
    SET stock = v.stock + d.cantidad
    FROM detalle_venta d JOIN anulada a ON a.id_venta = d.id_venta
    WHERE v.id_variante = d.id_variante
    RETURNING v.id_variante, d.id_venta, d.cantidad
)
INSERT INTO movimiento_stock (id_variante, id_venta, id_usuario, tipo, cantidad, motivo)
SELECT r.id_variante, r.id_venta,
       (SELECT id_persona FROM usuario WHERE nombre_usuario = 'admin'),
       'ANULACION', r.cantidad, 'Anulación de ejemplo'
FROM repuesto r;

COMMIT;

-- Comprobacion: debe salir EMITIDA 13 y ANULADA 1.
SELECT estado, count(*) AS ventas, sum(total) AS total FROM venta GROUP BY estado ORDER BY estado;
