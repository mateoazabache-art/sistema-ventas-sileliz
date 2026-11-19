-- =====================================================
-- 00_practica.sql
-- Tabla de practica para la semana de nivelacion (26 al 28 de octubre)
-- Base de datos: ventas_db
-- Autor: Rafael
-- Se puede ejecutar completo las veces que haga falta.
-- =====================================================

-- 1. Crear la tabla ------------------------------------
DROP TABLE IF EXISTS practica_producto;

CREATE TABLE practica_producto (
    id_producto  INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo       VARCHAR(20)   NOT NULL UNIQUE,
    nombre       VARCHAR(100)  NOT NULL,
    talla        VARCHAR(10)   NOT NULL,
    color        VARCHAR(30)   NOT NULL,
    precio       NUMERIC(10,2) NOT NULL CHECK (precio > 0),
    stock        INT           NOT NULL CHECK (stock >= 0)
);

-- 2. Datos de ejemplo (incluye tildes y ñ para probar UTF8) ------
INSERT INTO practica_producto (codigo, nombre, talla, color, precio, stock) VALUES
    ('PR-001', 'Blusa Niña',       'L',   'Rosado', 59.90,  10),
    ('PR-002', 'Short',            'XL',  'Negro',  79.90,   0),
    ('PR-003', 'Top',              '2XL', 'Beige',  34.90,  15),
    ('PR-004', 'Jean',             '3XL', 'Azul',  139.90,  24),
    ('PR-005', 'Pantalón Palazo',  '2XL', 'Marrón',129.90,  16),
    ('PR-006', 'Body fajero',      'XL',  'Gris',   69.90,  16),
    ('PR-007', 'Braga de Algodón', 'L',   'Rojo',   29.90,  70),
    ('PR-008', 'Bikini',           'L',   'Verde',  89.90,  29),
    ('PR-009', 'Blusa básica',     'M',   'Morado', 39.90,   5),
    ('PR-010', 'Short Enterizo',   'S',   'Rosa',   99.90,   3),
    ('PR-011', 'Jean Básico',      'L',   'Negro', 199.90,   2),
    ('PR-012', 'Pantalón Jogger',  'XL',  'Negro',  89.90,   8);

-- =====================================================
-- BUENA PRACTICA: no usar SELECT * en el codigo real
-- =====================================================
-- En las pruebas en pgAdmin, SELECT * esta bien.
-- En el codigo de Java (DAO), conviene listar las columnas.
--
--   Evitar:    SELECT * FROM practica_producto;
--   Preferir:  SELECT id_producto, nombre, precio FROM practica_producto;
--
-- Por que:
--   1. Si alguien agrega una columna a la tabla mas adelante,
--      el codigo no se rompe.
--   2. No trae datos de mas: es mas rapido y mas seguro,
--      porque solo salen las columnas que la pantalla necesita.
--   3. Se ve de un vistazo que datos usa cada consulta.
-- =====================================================

-- 3. Consultas de practica ---------------------------

-- 3.1 Prendas de talla L (comparacion exacta con =)
SELECT id_producto, codigo, nombre, talla, color, precio, stock
FROM practica_producto
WHERE talla = 'L';

-- 3.2 Prendas de mas de S/ 100, de la mas cara a la mas barata
SELECT id_producto, codigo, nombre, precio
FROM practica_producto
WHERE precio > 100
ORDER BY precio DESC;

-- 3.3 Buscador por nombre: ILIKE ignora mayusculas y minusculas
--     (pero NO ignora las tildes: '%pantalon%' no encuentra 'Pantalón'
--      y '%basico%' no encuentra 'Jean Básico')
--     Resultado esperado: 4 filas (2 blusas y 2 pantalones)
SELECT id_producto, codigo, nombre, talla, color, precio, stock
FROM practica_producto
WHERE nombre ILIKE '%Blusa%'
   OR nombre ILIKE '%Pantalón%';

-- 4. Pruebas que DEBEN FALLAR (CHECK, UNIQUE y NOT NULL) ------
--    Quitar los guiones y ejecutar cada linea por separado.
--    Cada prueba rompe UNA sola regla; lo demas es valido.

-- 4.1 Precio 0 -> 23514 (viola CHECK de precio)
-- INSERT INTO practica_producto (codigo, nombre, talla, color, precio, stock) VALUES ('PR-900', 'Prueba', 'M', 'Negro', 0, 5);

-- 4.2 Stock negativo -> 23514 (viola CHECK de stock)
-- INSERT INTO practica_producto (codigo, nombre, talla, color, precio, stock) VALUES ('PR-900', 'Prueba', 'M', 'Negro', 50, -5);

-- 4.3 Codigo repetido -> 23505 (viola UNIQUE de codigo; PR-001 ya existe)
--     En la API se responde con HTTP 409
-- INSERT INTO practica_producto (codigo, nombre, talla, color, precio, stock) VALUES ('PR-001', 'Prueba', 'M', 'Negro', 59.90, 5);

-- 4.4 Nombre vacio -> 23502 (viola NOT NULL de nombre)
--     En la API se responde con HTTP 400
-- INSERT INTO practica_producto (codigo, nombre, talla, color, precio, stock) VALUES ('PR-902', NULL, 'M', 'Negro', 69.90, 5);