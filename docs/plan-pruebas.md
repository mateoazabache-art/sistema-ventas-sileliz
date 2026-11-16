# Plan de pruebas — Gestión de Ventas Sileliz.mas

Casos de prueba funcionales, de seguridad y de recuperación (CP-01 a CP-18), con sus datos, pasos y resultado esperado,
más la matriz de trazabilidad con los requisitos. Se ejecutan el **lunes 16 de noviembre** (tarea 15 de Mateo, con Cesia)
y se repiten el día del ensayo con el proyecto recién bajado de `main`.

## Cómo preparar cada prueba

1. Detener Tomcat y ejecutar `bd\recrear_bd.cmd demo` (o `bash bd/recrear_bd.sh demo`). Debe salir
   `5 | 20 | 60 | 10 | 2 | 14 | 2 | 3`.
2. Clean and Build y Run en NetBeans. Abrir `http://localhost:8080/sileliz/login.html`.
3. Usuarios: `admin` / `Admin2026` (Administrador) y `vendedor` / `Venta2026` (Vendedor).
4. Para las pruebas que usan la consola: F12 → *Console* (si Chrome lo pide, escribir `allow pasting`).
5. Anotar el resultado (✅ / ❌), quién lo probó y la evidencia (captura en el Issue o en `docs/evidencias/`).
6. Al terminar, volver a ejecutar `recrear_bd` para dejar la base limpia.

## Casos de prueba

| ID | Prueba | Datos y pasos | Resultado esperado | Requisito | Responsable | Resultado | Evidencia |
|---|---|---|---|---|---|---|---|
| CP-01 | Ingresar con usuario y clave correctos | `admin` / `Admin2026` → Ingresar | Entra a Inicio; el menú muestra 9 opciones (con Usuarios y Reportes). La clave no aparece en la respuesta de `/api/login` (F12 → Network) | RF-01 | Cesia | | |
| CP-02 | Ingresar con clave incorrecta | `vendedor` / `Clave2026` | Mensaje "Usuario o clave incorrectos" (no dice cuál de los dos falló); HTTP 401 | RF-01 | Cesia | | |
| CP-03 | Cinco intentos fallidos seguidos | `vendedor` con clave errada 5 veces; luego la clave correcta | Al quinto intento, HTTP 423 y aviso de bloqueo por 15 minutos; con la clave correcta sigue bloqueado. Desbloquear: restablecer la clave desde Usuarios o `UPDATE usuario SET intentos_fallidos = 0, bloqueado_hasta = NULL WHERE nombre_usuario = 'vendedor';` | RF-02 | Rafael | | |
| CP-04 | El vendedor intenta entrar a Usuarios y Reportes | Ingresar como `vendedor`; abrir `usuarios.html` y en la consola `fetch('/sileliz/api/usuarios').then(r=>console.log(r.status))` | El menú no muestra Usuarios ni Reportes; la API responde **403** "No tiene permiso para esta acción." | RF-03 | Rafael | | |
| CP-05 | Crear producto con precio 0 | Productos → Nuevo producto, precio `0` | Aviso en el formulario (JavaScript); si se fuerza por consola, el servidor responde **400** y no se guarda | RF-05 | Naomi | | |
| CP-06 | Registrar cliente con DNI repetido o de 7 dígitos | Clientes → Nuevo: DNI `70000001` (ya existe); luego `7000001` | Repetido: **409** "Ya existe una persona con ese DNI."; 7 dígitos: aviso en el formulario y **400** en el servidor. No se guarda nada | RF-08 | Mateo | | |
| CP-07 | Vender 2 prendas con IGV incluido | Nueva venta, consumidor final, 2 unidades de `P-001-3XL-BLA` (Blusa manga larga, S/ 59.90), sin descuento, Efectivo | Total = precio × 2; base = total / 1,18; IGV = total − base (59.90 × 2 = 119.80 → base 101.53, IGV 18.27). El stock de la talla baja en 2; la venta aparece en Inicio (ventas del día) y en Reportes | RF-09, RF-14, RF-15 | Luz | | |
| CP-07b | Descuento por cantidad | 3 unidades de `P-001-3XL-BLA` (S/ 59.90) con "15 % desde 3 prendas" | Descuento 26.96, total 152.74 (vista previa = servidor) | RN-13 | Luz | ✅ 14 nov (Rafael, B001-000091) | Captura en el chat del 14 nov |
| CP-07c | Tope del vendedor | Como `vendedor`, una prenda de S/ 54.90 con "Porcentaje…" 15; luego 10 | Con 15: aviso "Como vendedor puede dar como máximo 10 %…" y no se guarda; con 10: total 49.41 | RN-14 | Luz | ✅ 14 nov (Rafael) | Captura en el chat del 14 nov |
| CP-08 | Vender más que el stock disponible | Agregar al carrito una talla con stock 1 (ej. `P-005-XL-MAR`) y cambiar la cantidad a 2 | La página no deja pasar del stock; si se fuerza por consola, **409** y no se guarda nada (stock igual) | RF-10 | Luz | | |
| CP-09 | Dos navegadores venden la última unidad al mismo tiempo | Dos navegadores (uno en incógnito) con la misma talla de stock 1 en el carrito; registrar casi a la vez | Solo una venta se guarda; la otra recibe stock insuficiente (409) | RF-10 | Luz | ✅ (Luz, ver `bitacora-ia.md`) | |
| CP-10 | Anular una venta como administrador | Ventas realizadas → Anular una venta EMITIDA, motivo "Cliente pidió boleta con DNI" | Estado ANULADA; el stock de cada prenda vuelve; en `movimiento_stock` aparece una fila ANULACION por línea. Anularla otra vez → 400 | RF-13 | Luz | | |
| CP-11 | Paginar, buscar y filtrar | Productos (buscar "blusa", filtrar por categoría, cambiar de página), Clientes (buscar por DNI) y Ventas realizadas (filtro de fechas) | Cada lista muestra 10 filas, Anterior/números/Siguiente funcionan y los filtros se respetan al cambiar de página; productos con stock ≤ 2 marcados "Stock bajo" | RF-04, RF-06, RF-07, RF-12 | Naomi y Mateo | | |
| CP-12 | Imprimir el comprobante | Desde una venta: Ver comprobante → Imprimir (vista previa de impresión, A4) | Datos de la tienda, lema, cliente o "Varios", detalle, base, IGV, total, serie-correlativo y la leyenda "Comprobante interno, sin valor tributario" | RF-11, RF-16 | Cesia | | |
| CP-13 | Diseño en 360, 768 y 1280 px | F12 → modo dispositivo en cada página | Sin desplazamiento horizontal; el menú se colapsa en el botón ☰ en celular | RNF-05 | Cesia | | |
| CP-14 | Inyección SQL | Login con usuario `' OR '1'='1`; buscadores de productos y clientes con `' OR '1'='1` | No entra al sistema; los buscadores no muestran todo ni dan error 500 (el texto se busca literalmente) | RNF-02 | Rafael | | |
| CP-15 | Ataque XSS | Crear un cliente con nombre `<script>alert(1)</script>` y verlo en la lista y en una venta | Se ve como texto; no aparece ninguna alerta | RNF-02 | Cesia | | |
| CP-16 | POST sin JSON | Consola: `fetch('/sileliz/api/clientes',{method:'POST',body:'a=1'}).then(r=>console.log(r.status))` | **415** "La petición debe enviarse en formato JSON."; las respuestas traen `X-Frame-Options: DENY` y `X-Content-Type-Options: nosniff` | RNF-02 | Rafael | | |
| CP-17 | Recrear la base desde cero | `bd\recrear_bd.cmd` en cada computadora | `5 \| 20 \| 60 \| 10 \| 2 \| 0 \| 2 \| 3` y todas las pruebas JUnit pasan, en las 5 computadoras | RNF-09 | Rafael | | |
| CP-18 | Ingreso de mercadería | Como admin: Productos → Tallas de `P-006` → Ingresar en `P-006-2XL-GRI` (stock 0), cantidad 10, motivo "Compra semanal". Repetir con cantidad 1000. Como vendedor, abrir la misma pantalla | Stock pasa de 0 a 10 y hay una fila INGRESO en `movimiento_stock`; 1000 → aviso (1 a 999); el vendedor no ve el botón Ingresar y la API le responde 403 | RF-19 | Rafael | ✅ 14 nov en la PC de Rafael (antes de unir el #49) | |

## Matriz de trazabilidad

| Requisito | CP | Pruebas JUnit relacionadas |
|---|---|---|
| RF-01, RF-02 | CP-01, CP-02, CP-03 | `AuthServiceTest`, `ClaveTest`, `UsuarioDaoTest` |
| RF-03 | CP-04 | `UsuarioServiceTest`, `SesionFilterTest` |
| RF-04 a RF-07 | CP-05, CP-11 | `ProductoServiceTest`, `ProductoDaoTest`, `CategoriaDaoTest`, `VarianteDaoTest`, `VarianteProductoTest` |
| RF-08 | CP-06, CP-11 | `ClienteServiceTest`, `ClienteDaoTest` |
| RF-09, RF-10 | CP-07, CP-07b, CP-07c, CP-08, CP-09 | `VentaServiceTest`, `VentaTest`, `DescuentoTest`, `VarianteDaoTest` |
| RF-11, RF-16 | CP-12 | — (página) |
| RF-12, RF-13 | CP-10, CP-11 | `VentaServiceTest` |
| RF-14, RF-15 | CP-07 | `ReporteServiceTest`, `ReporteDaoTest`, `EstadisticaDaoTest` |
| RF-19 | CP-18 | `IngresoServiceTest`, `MovimientoStockTest` |
| RNF-02 (seguridad) | CP-14, CP-15, CP-16 | `SesionFilterTest`, `JsonTest` |
| RNF-05 (responsive) | CP-13 | — |
| RNF-09 (recuperación) | CP-17 | todas (se corren después de recrear) |

## Pruebas automáticas (JUnit)

| Fecha | Dónde | Resultado |
|---|---|---|
| 14 nov | `develop` (`4fd88f2`) | 227/227 |
| 14 nov | `develop` + PR #49, #50 y #51 | 253/253 (base limpia y con las 14 ventas de ejemplo) |
| 16 nov | `develop` (`2c76666`, con #47, #49, #54, #57, #58 y #59) | 312/312 |
| 16 nov | `develop` + PR #50, #51, #52 y #53 | **330/330** |

Nivel unitario (sin base): `Clave`, `Validador`, `Venta` (totales), `Descuento`, `VarianteProducto`, servicios con DAO falso.
Nivel de integración (con `ventas_db`, rollback al terminar): DAO y transacciones de venta e ingreso.
