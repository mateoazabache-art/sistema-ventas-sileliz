# Gestión de Ventas Sileliz.mas

Aplicación web para la tienda **Sileliz.mas** (*Más que una talla*), dedicada a la venta de ropa de tallas grandes para mujer.
Registra ventas con carrito y descuentos, controla el stock por talla y color, administra productos, clientes y usuarios,
emite un comprobante interno imprimible y muestra reportes.

Proyecto del curso de Programación Orientada a Objetos — TECSUP, 2.º ciclo, 2026. Entrega: viernes 20 de noviembre de 2026.

---

## Qué hace

| Módulo | Administrador | Vendedor |
|---|---|---|
| Inicio con las ventas del día, clientes y stock bajo | Sí | Sí |
| Nueva venta (carrito, descuento, método de pago, consumidor final) | Sí | Sí |
| Ventas realizadas (historial paginado con filtro de fechas) | Todas | Solo las propias |
| Anular venta con motivo (repone el stock) | Sí | No |
| Comprobante interno imprimible (serie y correlativo, IGV incluido) | Sí | Sí |
| Productos, tallas y colores, stock bajo, ingreso de mercadería | Crear, editar, desactivar | Solo consulta |
| Clientes (búsqueda por nombre o DNI) | Crear, editar, desactivar | Crear, editar |
| Reportes (ventas por fechas, más vendidos, por vendedor) | Sí | No |
| Usuarios (alta, edición, desactivar, restablecer clave) | Sí | No |
| Ayuda | Sí | Sí |

Reglas principales: el precio incluye IGV (18 %); el stock se controla por variante (producto + talla + color) y solo cambia
con una venta, una anulación o un ingreso de mercadería, siempre con su registro en el kárdex; lo que tiene ventas no se borra,
se desactiva; descuento por cantidad de **15 % desde 3 prendas**; la cuenta se bloquea 15 minutos tras 5 intentos fallidos.

---

## Versiones oficiales

Las 5 computadoras del equipo usan las mismas versiones (detalle en [`docs/entorno.md`](docs/entorno.md)).

| Herramienta | Versión |
|---|---|
| JDK | Eclipse Temurin **17** (LTS) |
| Servidor | Apache **Tomcat 10.1** (Jakarta Servlet 6) |
| IDE | Apache **NetBeans** (proyecto *Java with Ant → Java Web*) |
| Base de datos | **PostgreSQL 18** (también funciona con 16) y pgAdmin 4 |
| Librerías (en `lib/`) | Gson 2.10.1 · driver PostgreSQL JDBC 42.7.14 |
| Pruebas | JUnit 4.13.2 + Hamcrest 1.3 (incluidas en NetBeans) |
| Interfaz | HTML, CSS, JavaScript (`fetch`) y Bootstrap 5.3.3 (local, sin internet) |

---

## Cómo ejecutarlo

### 1. Clonar

```
git clone https://github.com/mateoazabache-art/sistema-ventas-sileliz.git
cd sistema-ventas-sileliz
git checkout main
```

### 2. Configurar la conexión

Copiar `SistemaVentas/src/java/config.example.properties` como `config.properties` **en la misma carpeta**
y escribir en `db.clave` la clave que tendrá el usuario `ventas_app`.
Si PostgreSQL usa otro puerto, cambiarlo en `db.url`.

> `config.properties` está en `.gitignore`: **nunca se sube a GitHub**.

### 3. Crear la base de datos

Con Tomcat detenido y pgAdmin cerrado, desde la carpeta del repositorio:

| Sistema | Comando |
|---|---|
| Windows | `bd\recrear_bd.cmd` |
| Mac / Linux | `bash bd/recrear_bd.sh` |

Pide la clave del usuario `postgres`. Borra y crea `ventas_db` en UTF-8, ejecuta `00_roles` → `01_crear_tablas` →
`02_datos_prueba` → `03_vistas_triggers` y le pone a `ventas_app` la clave de `config.properties`.
Al final debe mostrar:

```
 categorias | productos | variantes | clientes | usuarios | ventas | triggers | vistas
          5 |        20 |        60 |       10 |        2 |      0 |        2 |      3
```

- Con `demo` al final (`bd\recrear_bd.cmd demo`) carga además **14 ventas de ejemplo** para ver Inicio y Reportes con datos.
- Si PostgreSQL está en otro puerto: en PowerShell `$env:PGPORT = "5433"` antes del comando.
- Respaldo: `bd\respaldar_bd.cmd` → `%USERPROFILE%\respaldos-sileliz\ventas_db_AAAA-MM-DD_HHMM.sql`.

### 4. Abrir y ejecutar en NetBeans

1. *File → Open Project* → carpeta `SistemaVentas`.
2. Comprobar que el servidor sea **Tomcat 10.1** y la plataforma **JDK 17** (*Properties → Run / Libraries*).
3. **Clean and Build** (`Shift + F11`) → `BUILD SUCCESSFUL`.
4. **Run** (`F6`) y abrir en el navegador:

```
http://localhost:8080/sileliz/login.html
```

Comprobación rápida: `http://localhost:8080/sileliz/api/salud` debe responder `"bd":"conectada"`.

### 5. Usuarios de prueba (solo desarrollo)

| Usuario | Clave | Rol |
|---|---|---|
| `admin` | `Admin2026` | Administrador |
| `vendedor` | `Venta2026` | Vendedor |

Clientes de prueba: DNI `70000001` a `70000010`. Productos `P-001` a `P-020` (SKU como `P-001-XL-BLA`).

---

## Pruebas

En NetBeans: clic derecho en el proyecto → **Test** (`Alt + F6`).

- Las pruebas de DAO y servicios usan la base `ventas_db` y dejan todo como estaba (rollback).
- Asumen los datos exactos de `02_datos_prueba.sql`: **recrear la base antes de correrlas**
  si se crearon productos, clientes o ventas a mano.
- Los textos rojos `WARNING`/`SEVERE` en la salida son errores provocados a propósito por las pruebas (por ejemplo,
  un DNI repetido), no fallos.

Los casos de prueba funcionales CP-01 a CP-17 están en [`docs/plan-pruebas.md`](docs/plan-pruebas.md).

---

## Arquitectura

```
Navegador (HTML + JS con fetch, Bootstrap)
   │  JSON (UTF-8)
   ▼
SesionFilter ── 401 sin sesión · 403 sin permiso · 415 si no es JSON · cabeceras de seguridad
   ▼
Servlets  /api/*   (solo leen el JSON, llaman al servicio y responden)
   ▼
Servicios          (reglas de negocio y transacciones)
   ▼
DAO                (SQL con PreparedStatement)
   ▼
PostgreSQL ventas_db  (usuario ventas_app sin permisos de DDL)
```

| Carpeta | Contenido |
|---|---|
| `bd/` | Scripts SQL, `recrear_bd`, `respaldar_bd`, ERD |
| `SistemaVentas/src/java/pe/tecsup/sileliz/` | `api` (servlets), `servicio`, `dao`, `modelo`, `descuento`, `filtro`, `util`, `excepcion` |
| `SistemaVentas/test/` | Pruebas JUnit |
| `SistemaVentas/web/` | Páginas HTML, `js/`, `css/`, `img/`, `lib/bootstrap/` |
| `docs/` | Informe, plan de pruebas, entrevista, entorno, guía del DAO, diagrama de clases, bitácora de IA |

Seguridad: claves con PBKDF2 (600 000 iteraciones), solo `PreparedStatement`, datos en pantalla con `textContent`,
sesión con `HttpOnly` y `SameSite=Lax`, errores 500 sin detalles técnicos. Detalle en el informe.

---

## Flujo de trabajo del equipo

- Ramas: `main` (entregas), `develop` (integración) y una rama `feature/...` o `fix/...` por tarea.
- Todo entra a `develop` por **Pull Request** con la aprobación de otra persona (rama protegida).
- Antes del commit: `git status -sb`, agregar solo los archivos propios (nunca `git add .`) y correr las pruebas.

## Equipo

| Integrante | Módulo | Documentación |
|---|---|---|
| Rafael | Base de datos, seguridad, login, reportes, ingreso de mercadería, API de usuarios | Parte de base de datos del informe, ERD, diagrama de clases |
| Cesia | Proyecto base, plantilla web, login, Inicio, usuarios (página), comprobante | Prototipos y capítulos del manual |
| Naomi | Categorías, productos y variantes, validaciones | Manual de usuario consolidado |
| Luz | Ventas: Nueva venta, Ventas realizadas, anulación, descuentos | Diagrama de secuencia y diapositivas |
| Mateo | Repositorio, clientes, gestión del proyecto | Informe de ingeniería, entrevista y plan de pruebas |
