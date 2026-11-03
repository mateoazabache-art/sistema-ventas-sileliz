-- =====================================================
-- 02_datos_prueba.sql
-- Datos de prueba: 5 categorias, 20 productos con 3 variantes cada uno,
-- 10 clientes y 2 usuarios (seccion 4.5 del plan)
-- Base de datos: ventas_db
-- Autor: Rafael
-- Ejecutar conectado como postgres, DESPUES de 01_crear_tablas.sql.
-- Se puede ejecutar completo las veces que haga falta.
-- =====================================================
-- TODOS LOS DATOS SON FICTICIOS (plan, seccion 1.5: prohibido usar
-- datos reales de clientes). Los DNI empiezan con 7000 a proposito.
--
-- Usuarios para probar el login (SOLO para desarrollo):
--   admin     / Admin2026   (ADMINISTRADOR)
--   vendedor  / Venta2026   (VENDEDOR)
-- Los hashes se generaron con Clave.main (PBKDF2, 600 000 iteraciones).
-- =====================================================

-- 0. Vaciar todo y reiniciar los contadores -----------
--    TRUNCATE borra los datos pero deja las tablas.
--    RESTART IDENTITY hace que los id vuelvan a empezar en 1.
TRUNCATE movimiento_stock, detalle_venta, venta,
         variante_producto, producto, categoria,
         cliente, usuario, persona
         RESTART IDENTITY;
ALTER SEQUENCE seq_correlativo RESTART WITH 1;


-- =====================================================
-- 1. CATEGORIAS
-- =====================================================
INSERT INTO categoria (nombre) VALUES
    ('Blusas y polos'),         -- id 1
    ('Pantalones'),             -- id 2
    ('Jeans'),                  -- id 3
    ('Vestidos y enterizos'),   -- id 4
    ('Ropa interior y fajas');  -- id 5


-- =====================================================
-- 2. PRODUCTOS (precio incluye IGV, RN-01)
-- =====================================================
INSERT INTO producto (codigo, nombre, descripcion, id_categoria, precio_venta) VALUES
    ('P-001', 'Blusa manga larga',   'Blusa de viscosa con botones',            1,  59.90),
    ('P-002', 'Blusa de gasa',       'Blusa ligera con forro',                  1,  69.90),
    ('P-003', 'Blusa campesina',     'Blusa con elástico en hombros',           1,  54.90),
    ('P-004', 'Polo básico',         'Polo de algodón cuello redondo',          1,  34.90),
    ('P-005', 'Pantalón palazo',     'Pantalón de pierna ancha',                2, 129.90),
    ('P-006', 'Pantalón jogger',     'Pantalón con puño y pretina elástica',    2,  89.90),
    ('P-007', 'Pantalón de vestir',  'Pantalón recto para oficina',             2, 119.90),
    ('P-008', 'Short enterizo',      'Short con tirantes regulables',           2,  99.90),
    ('P-009', 'Jean clásico',        'Jean tiro alto corte recto',              3, 139.90),
    ('P-010', 'Jean mom',            'Jean holgado tiro alto',                  3, 149.90),
    ('P-011', 'Jean wide leg',       'Jean de pierna ancha',                    3, 159.90),
    ('P-012', 'Jean pitillo',        'Jean ajustado con stretch',               3, 139.90),
    ('P-013', 'Vestido casual',      'Vestido midi de algodón',                 4, 119.90),
    ('P-014', 'Vestido de noche',    'Vestido largo con escote en V',           4, 199.90),
    ('P-015', 'Vestido camisero',    'Vestido con botones y cinturón',          4, 139.90),
    ('P-016', 'Enterizo',            'Enterizo de pierna ancha',                4, 129.90),
    ('P-017', 'Body fajero',         'Body con control de abdomen',             5,  69.90),
    ('P-018', 'Faja reductora',      'Faja de compresión media',                5,  89.90),
    ('P-019', 'Brasier sin aro',     'Brasier de soporte con copa suave',       5,  49.90),
    ('P-020', 'Braga de algodón',    'Pack de bragas de algodón',               5,  29.90);


-- =====================================================
-- 3. VARIANTES: 3 tallas por producto (60 variantes)
-- =====================================================
-- Cada producto tiene un color y se ofrece en XL, 2XL y 3XL.
-- SKU = codigo-talla-3 letras del color (sin tildes), ej: P-001-XL-NEG
-- El stock varia para que haya agotados (0) y stock bajo (<= 2, RN-10).
INSERT INTO variante_producto (id_producto, talla, color, sku, stock)
SELECT p.id_producto,
       t.talla,
       c.color,
       p.codigo || '-' || t.talla || '-' ||
           upper(translate(left(c.color, 3), 'áéíóúñÁÉÍÓÚÑ', 'aeiounAEIOUN')),
       (p.id_producto * 7 + t.orden * 5) % 13          -- stock entre 0 y 12
FROM producto p
JOIN (VALUES
        ('P-001', 'Blanco'), ('P-002', 'Rosado'), ('P-003', 'Celeste'), ('P-004', 'Negro'),
        ('P-005', 'Marrón'), ('P-006', 'Gris'),   ('P-007', 'Negro'),   ('P-008', 'Beige'),
        ('P-009', 'Azul'),   ('P-010', 'Celeste'),('P-011', 'Azul'),    ('P-012', 'Negro'),
        ('P-013', 'Verde'),  ('P-014', 'Rojo'),   ('P-015', 'Beige'),   ('P-016', 'Negro'),
        ('P-017', 'Piel'),   ('P-018', 'Negro'),  ('P-019', 'Piel'),    ('P-020', 'Blanco')
     ) AS c (codigo, color) ON c.codigo = p.codigo
CROSS JOIN (VALUES (1, 'XL'), (2, '2XL'), (3, '3XL')) AS t (orden, talla)
ORDER BY p.id_producto, t.orden;


-- =====================================================
-- 4. PERSONAS: 10 clientes + 2 usuarios
-- =====================================================
INSERT INTO persona (nombres, apellidos, dni, telefono, correo, direccion) VALUES
    ('María Elena',  'Castillo Ríos',   '70000001', '987000001', 'maria.castillo@correo.pe', 'Av. España 120, Trujillo'),
    ('Rosa',         'Mendoza Vega',    '70000002', '987000002', NULL,                       'Jr. Pizarro 455, Trujillo'),
    ('Carmen',       'Quispe Lazo',     '70000003', '987000003', 'carmen.q@correo.pe',       NULL),
    ('Lucía',        'Fernández Paz',   '70000004', NULL,        NULL,                       'Urb. La Merced, Trujillo'),
    ('Patricia',     'Rojas Salinas',   '70000005', '987000005', 'paty.rojas@correo.pe',     'Av. Larco 890, Trujillo'),
    ('Ana Sofía',    'Gutiérrez León',  '70000006', '987000006', NULL,                       NULL),
    ('Julia',        'Vásquez Cruz',    '70000007', '987000007', 'julia.vc@correo.pe',       'Calle Bolívar 33, Trujillo'),
    ('Elena',        'Torres Ñique',    '70000008', '987000008', NULL,                       'El Porvenir, Trujillo'),
    ('Gabriela',     'Sánchez Alva',    '70000009', NULL,        'gaby.sa@correo.pe',        NULL),
    ('Teresa',       'Morales Díaz',    '70000010', '987000010', NULL,                       'Víctor Larco, Trujillo'),
    ('Sofía',        'Administradora',  '70000101', '987000101', NULL,                       NULL),
    ('Valeria',      'Vendedora',       '70000102', '987000102', NULL,                       NULL);

-- 4.1 Clientes: se enlazan por DNI (no dependemos del numero de id)
INSERT INTO cliente (id_persona, observaciones)
SELECT p.id_persona, d.observaciones
FROM persona p
JOIN (VALUES
        ('70000001', 'Clienta frecuente'),
        ('70000002', NULL),
        ('70000003', 'Prefiere pagar con Yape'),
        ('70000004', NULL),
        ('70000005', 'Talla habitual 2XL'),
        ('70000006', NULL),
        ('70000007', NULL),
        ('70000008', 'Avisar cuando llegue jean wide leg'),
        ('70000009', NULL),
        ('70000010', NULL)
     ) AS d (dni, observaciones) ON d.dni = p.dni;

-- 4.2 Usuarios (clave_hash generado con Clave.main)
INSERT INTO usuario (id_persona, nombre_usuario, clave_hash, rol)
SELECT p.id_persona, d.nombre_usuario, d.clave_hash, d.rol
FROM persona p
JOIN (VALUES
        ('70000101', 'admin',    '600000:zAl1i4SFxIxdlUhLMccn6w==:2UpsH80/Jn8LirE8gg/AlJkMQlH+ZGqBsdDJw+oZyiU=', 'ADMINISTRADOR'),
        ('70000102', 'vendedor', '600000:xP5Nwbp3mCoy2wXbJ1f1Pg==:7ZNOmwNdjNDymdg85K+k4UEBrqz36XYR1kapLOvUceM=', 'VENDEDOR')
     ) AS d (dni, nombre_usuario, clave_hash, rol) ON d.dni = p.dni;


-- =====================================================
-- 5. COMPROBACION
-- =====================================================
-- Esperado: 5 | 20 | 60 | 10 | 2
SELECT (SELECT count(*) FROM categoria)         AS categorias,
       (SELECT count(*) FROM producto)          AS productos,
       (SELECT count(*) FROM variante_producto) AS variantes,
       (SELECT count(*) FROM cliente)           AS clientes,
       (SELECT count(*) FROM usuario)           AS usuarios;

-- Variantes agotadas o con stock bajo (RN-10)
SELECT p.nombre, v.talla, v.color, v.sku, v.stock
FROM variante_producto v
JOIN producto p ON p.id_producto = v.id_producto
WHERE v.stock <= v.stock_minimo
ORDER BY v.stock, p.nombre;
