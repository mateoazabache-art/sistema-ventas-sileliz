-- =====================================================
-- 01_crear_tablas.sql
-- Las 9 tablas del sistema y la secuencia de comprobantes
-- Base de datos: ventas_db
-- Autor: Rafael
-- Contrato: seccion 4.3 del plan (diccionario de datos)
-- Ejecutar conectado como postgres, DESPUES de 00_roles.sql.
-- =====================================================
-- ATENCION: este script BORRA y vuelve a crear las tablas.
-- Se pierden los datos que tengan. Es para desarrollo.
-- =====================================================

-- 0. Borrar en orden inverso (los hijos antes que los padres) --
DROP TABLE IF EXISTS movimiento_stock  CASCADE;
DROP TABLE IF EXISTS detalle_venta     CASCADE;
DROP TABLE IF EXISTS venta             CASCADE;
DROP TABLE IF EXISTS variante_producto CASCADE;
DROP TABLE IF EXISTS producto          CASCADE;
DROP TABLE IF EXISTS categoria         CASCADE;
DROP TABLE IF EXISTS cliente           CASCADE;
DROP TABLE IF EXISTS usuario           CASCADE;
DROP TABLE IF EXISTS persona           CASCADE;
DROP SEQUENCE IF EXISTS seq_correlativo;


-- =====================================================
-- BLOQUE 1: PERSONAS (herencia Persona -> Cliente / Usuario)
-- =====================================================

-- 1.1 persona: datos comunes de clientes y usuarios (decision 1)
CREATE TABLE persona (
    id_persona      INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombres         VARCHAR(80)  NOT NULL,
    apellidos       VARCHAR(80)  NOT NULL,
    dni             CHAR(8)      NOT NULL UNIQUE
                    CHECK (dni ~ '^[0-9]{8}$'),                 -- RN-04: 8 digitos
    telefono        VARCHAR(9)
                    CHECK (telefono ~ '^9[0-9]{8}$'),           -- RN-04: 9 digitos, empieza con 9
    correo          VARCHAR(120),
    direccion       VARCHAR(150),
    activo          BOOLEAN      NOT NULL DEFAULT true,         -- RN-05: se desactiva, no se borra
    fecha_registro  TIMESTAMP    NOT NULL DEFAULT now()
);

-- 1.2 usuario: hija de persona (comparte id_persona)
CREATE TABLE usuario (
    id_persona         INT          PRIMARY KEY
                       REFERENCES persona (id_persona),
    nombre_usuario     VARCHAR(30)  NOT NULL UNIQUE,
    clave_hash         VARCHAR(200) NOT NULL,                   -- PBKDF2: iteraciones:sal:hash
    rol                VARCHAR(15)  NOT NULL
                       CHECK (rol IN ('ADMINISTRADOR', 'VENDEDOR')),
    intentos_fallidos  INT          NOT NULL DEFAULT 0
                       CHECK (intentos_fallidos >= 0),
    bloqueado_hasta    TIMESTAMP                                -- RN-11: nulo = no bloqueado
);

-- 1.3 cliente: hija de persona (comparte id_persona)
CREATE TABLE cliente (
    id_persona     INT          PRIMARY KEY
                   REFERENCES persona (id_persona),
    observaciones  VARCHAR(200)
);


-- =====================================================
-- BLOQUE 2: CATALOGO
-- =====================================================

-- 2.1 categoria
CREATE TABLE categoria (
    id_categoria  INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre        VARCHAR(60) NOT NULL UNIQUE,
    activo        BOOLEAN     NOT NULL DEFAULT true
);

-- 2.2 producto: nombre y precio se escriben una sola vez (decision 2)
CREATE TABLE producto (
    id_producto   INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo        VARCHAR(20)   NOT NULL UNIQUE,
    nombre        VARCHAR(100)  NOT NULL,
    descripcion   VARCHAR(250),
    id_categoria  INT           NOT NULL
                  REFERENCES categoria (id_categoria),
    precio_venta  NUMERIC(10,2) NOT NULL CHECK (precio_venta > 0),   -- RN-01: incluye IGV
    activo        BOOLEAN       NOT NULL DEFAULT true
);

-- 2.3 variante_producto: producto + talla + color; aqui vive el stock (RN-03)
CREATE TABLE variante_producto (
    id_variante   INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_producto   INT          NOT NULL
                  REFERENCES producto (id_producto),
    talla         VARCHAR(10)  NOT NULL,                        -- texto validado (decision 3)
    color         VARCHAR(30)  NOT NULL,
    sku           VARCHAR(30)  NOT NULL UNIQUE,
    stock         INT          NOT NULL DEFAULT 0
                  CHECK (stock >= 0),                           -- ultima barrera (seccion 4.4)
    stock_minimo  INT          NOT NULL DEFAULT 2
                  CHECK (stock_minimo >= 0),                    -- RN-10
    activo        BOOLEAN      NOT NULL DEFAULT true,
    CONSTRAINT uq_variante_producto_talla_color
        UNIQUE (id_producto, talla, color)                      -- no hay dos "XL Negro" del mismo producto
);


-- =====================================================
-- BLOQUE 3: VENTAS
-- =====================================================

-- 3.1 Secuencia del correlativo (decision 8)
--     Es atomica: dos ventas al mismo tiempo nunca reciben el mismo numero.
--     Puede tener saltos tras un rollback (aceptable en comprobante interno).
CREATE SEQUENCE seq_correlativo START WITH 1;

-- 3.2 venta
CREATE TABLE venta (
    id_venta          INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    serie             CHAR(4)       NOT NULL DEFAULT 'B001',
    numero            INT           NOT NULL DEFAULT nextval('seq_correlativo'),
    fecha_hora        TIMESTAMP     NOT NULL DEFAULT now(),
    id_cliente        INT
                      REFERENCES cliente (id_persona),          -- RN-06: nulo = consumidor final
    id_usuario        INT           NOT NULL
                      REFERENCES usuario (id_persona),
    base_imponible    NUMERIC(10,2) NOT NULL CHECK (base_imponible >= 0),
    descuento         NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (descuento >= 0),
    igv               NUMERIC(10,2) NOT NULL CHECK (igv >= 0),
    total             NUMERIC(10,2) NOT NULL CHECK (total >= 0),
    metodo_pago       VARCHAR(15)   NOT NULL
                      CHECK (metodo_pago IN ('EFECTIVO', 'YAPE', 'PLIN', 'TARJETA', 'TRANSFERENCIA')),
    estado            VARCHAR(10)   NOT NULL DEFAULT 'EMITIDA'
                      CHECK (estado IN ('EMITIDA', 'ANULADA')),
    motivo_anulacion  VARCHAR(200),
    CONSTRAINT uq_venta_serie_numero UNIQUE (serie, numero),    -- RN-07: B001-000015 no se repite
    CONSTRAINT ck_venta_motivo_anulacion                        -- RN-08: anular exige motivo
        CHECK (estado <> 'ANULADA'
               OR (motivo_anulacion IS NOT NULL AND length(trim(motivo_anulacion)) > 0))
);

-- La secuencia "pertenece" a la columna: si se borra la tabla, se borra con ella
ALTER SEQUENCE seq_correlativo OWNED BY venta.numero;

-- 3.3 detalle_venta: guarda precio y descripcion del momento (RN-09)
--     Sin ON DELETE CASCADE: las ventas no se borran.
CREATE TABLE detalle_venta (
    id_detalle       INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_venta         INT           NOT NULL
                     REFERENCES venta (id_venta),
    id_variante      INT           NOT NULL
                     REFERENCES variante_producto (id_variante),
    descripcion      VARCHAR(150)  NOT NULL,
    cantidad         INT           NOT NULL CHECK (cantidad > 0),
    precio_unitario  NUMERIC(10,2) NOT NULL CHECK (precio_unitario > 0),
    subtotal         NUMERIC(10,2) NOT NULL
);

-- 3.4 movimiento_stock: kardex, una fila por cada cambio de stock (decision 10)
CREATE TABLE movimiento_stock (
    id_movimiento  INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_variante    INT          NOT NULL
                   REFERENCES variante_producto (id_variante),
    id_venta       INT
                   REFERENCES venta (id_venta),                 -- nulo en INGRESO y AJUSTE
    id_usuario     INT          NOT NULL
                   REFERENCES usuario (id_persona),
    tipo           VARCHAR(10)  NOT NULL
                   CHECK (tipo IN ('VENTA', 'ANULACION', 'INGRESO', 'AJUSTE')),
    cantidad       INT          NOT NULL
                   CHECK (cantidad <> 0),                       -- negativa si el stock baja
    fecha_hora     TIMESTAMP    NOT NULL DEFAULT now(),
    motivo         VARCHAR(200)
);


-- =====================================================
-- BLOQUE 4: INDICES (RNF-08: respuesta rapida con muchos datos)
-- =====================================================
-- PostgreSQL ya crea indice para PRIMARY KEY y UNIQUE.
-- Estos aceleran las busquedas por clave foranea y por fecha.
CREATE INDEX idx_producto_categoria     ON producto (id_categoria);
CREATE INDEX idx_venta_fecha_hora       ON venta (fecha_hora);
CREATE INDEX idx_venta_usuario          ON venta (id_usuario);
CREATE INDEX idx_venta_cliente          ON venta (id_cliente);
CREATE INDEX idx_detalle_venta_venta    ON detalle_venta (id_venta);
CREATE INDEX idx_detalle_venta_variante ON detalle_venta (id_variante);
CREATE INDEX idx_movimiento_variante    ON movimiento_stock (id_variante);


-- =====================================================
-- BLOQUE 5: COMPROBACION
-- =====================================================
-- Debe listar las 9 tablas (mas practica_producto si existe)
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
ORDER BY table_name;
