# Bitácora de Uso de Inteligencia Artificial - Sileliz.mas

Este registro documenta las consultas y soporte técnico asistido por IA utilizados para acelerar el desarrollo del sistema y garantizar el cumplimiento de los estándares de la arquitectura.

## Registro de Asistencia Técnica

* **Fecha:** 28 de octubre al 09 de noviembre de 2026  
* **Participante:** Mateo Azabache (Módulo Clientes y Gestión)  
* **Herramienta:** Asistente de IA (Gemini)  

### Principales Consultas y Contribuciones Asistidas:
1. **Estructura del Backend (`ClienteServlet.java` y `ClienteService`):** Asistencia en la redacción de los métodos REST (GET con paginación, POST, PUT, DELETE) asegurando la gestión de excepciones y códigos de estado HTTP estándar.
2. **Frontend Seguro (`clientes.html` y `clientes.js`):** Pautas de implementación usando llamadas asíncronas (`fetch`) y manipulación de elementos mediante `textContent` para mitigar vulnerabilidades XSS.
3. **Documentación de Ingeniería:** Estructuración formal de la Tarea 11 (Acta de entrevista, secciones 1 y 2 del informe, plantilla de Pull Request bajo los criterios de seguridad 6.2 y la matriz del plan de pruebas CP-01 al CP-17).

### Errores de la IA y cómo se corrigieron:
- El `clientes.js` que generó la IA llamaba a una función `api()` que no existe (en `api.js` el objeto se llama `API`) y declaraba `let paginaActual`, que choca con la función del mismo nombre de `layout.js`. La página quedaba con la tabla vacía, y así se unió en el PR #20 sin probarla en el navegador. Se corrigió el 11 y 12 de noviembre en el PR #35: `numeroPagina` y `API.get` / `API.post` / `API.put` / `API.delete`.
- `clientes.html` no partía de `plantilla.html` (no salía el menú) y el botón "Nuevo cliente" llamaba a `mostrarFormulario()`, que nunca se escribió. Se rehízo con el formulario de alta y edición, `validaciones.js` y la paginación.
- `ClienteService` no validaba el correo ni el largo de los campos en el servidor; se agregó en el PR #35 usando `Validador`. Desactivar clientes quedó solo para el administrador (PR #34 de Rafael, en `SesionFilter`).
- Los documentos de la tarea 11 (acta, informe, plan de pruebas) quedaron como esqueletos con marcas `[cite]` de la IA; los completó Rafael como suplente el 14 de noviembre.
- Las correcciones del 11 y 12 de noviembre se hicieron junto con Rafael, usando Claude.

### Cómo se comprobó:
- Pruebas JUnit en NetBeans: `ClienteDaoTest` (8) y `ClienteServiceTest` (11). El 12 de noviembre pasaron las 128 pruebas del proyecto en mi Mac, después de recrear la base con los scripts 02 y 03.
- A mano en la página: alta, DNI repetido (409), DNI de 7 dígitos, edición, paginación y desactivar como administrador; como vendedor, sin el botón Desactivar.
- Lección: el revisor debe abrir la pantalla en su computadora antes de aprobar (lista 6.2 del plan).

## Registro de Asistencia Técnica · Luz

* **Fecha:** 5 al 13 de noviembre de 2026  
* **Participante:** Luz (Módulo de Ventas)  
* **Herramienta:** Claude (Anthropic), en dos chats: el primero se llenó de archivos y el segundo continuó con el archivo de contexto del proyecto.  

### Principales consultas y contribuciones asistidas:
1. **Backend de ventas (`VentaService`, `VentaServlet`, DAO):** registrar venta con transacción (todo o nada), consultar por id, historial paginado con filtro de fechas y anular venta con reposición de stock y registro en el kardex.
2. **Nueva venta (`venta-nueva.html`, `venta-nueva.js`):** buscar cliente por DNI, buscar prenda, carrito en JavaScript, descuento, método de pago y vista previa del total.
3. **Ventas realizadas (`ventas.html`, `ventas.js`, `ventas.css`):** filtro por fechas, paginación y botón Anular solo para administradora, usando `textContent` en lugar de `innerHTML`.
4. **Pruebas JUnit de `VentaService` (13 pruebas):** DAO falsos, pasando a la IA el código real de las clases para que no inventara nombres.
5. **Prueba de concurrencia (CP-09):** con una prenda de stock 1 y dos navegadores, una venta se guardó y la otra recibió stock insuficiente. Luego se anularon las ventas de prueba y el stock se repuso.
6. **Git y Pull Requests:** ramas, commits y los PR #44 y #45.

### Errores de la IA y cómo se corrigieron:
- Inventó nombres que no existen (LineaSolicitud, Variante, v.isProductoActivo(), v.getNombre(), venta.getId() y Json.error(resp, 401, "...")). No compilaba. Se corrigió con las clases reales y se creó un archivo de contexto con las firmas exactas.
- Usó javax.servlet en lugar de jakarta.servlet. Tomcat 10.1 solo funciona con jakarta. Se cambió el import.
- Cruzó el orden de parámetros del mismo tipo en MovimientoStock.deVenta. El compilador no avisa y los datos quedaban cruzados. Se corrigió respetando la firma.
- Los colores de EMITIDA y ANULADA no se veían por la prioridad de Bootstrap en las tablas. Se cambió el selector de ventas.css.
- Al cambiar de rama desaparecieron los archivos de la página y salió un 404. Se volvió a la rama feature/ventas-realizadas y se hizo Clean and Build.

### Cómo se comprobó:
- Cada pantalla se probó en mi computadora como administradora y como vendedora antes de subirla.
- Las 13 pruebas JUnit se ejecutaron en NetBeans y pasaron todas.
- Todo el código se revisó y se probó antes de subirlo, y puedo explicar cada parte.

## Registro de Asistencia Técnica · Rafael

* **Fecha:** 24 de octubre al 18 de noviembre de 2026  
* **Participante:** Rafael Alva León (Base de datos, seguridad, login, reportes; suplente de Mateo y de Cesia)  
* **Herramienta:** Claude (Anthropic), conectado a mi computadora y a GitHub. Cada sesión empezaba revisando mi terminal y el repositorio y comparando con el plan v3.1.  

### Principales consultas y contribuciones asistidas:
1. **Base de datos (`bd/`):** tablas, restricciones, datos de prueba, vistas y triggers; `recrear_bd` y `respaldar_bd` para Windows y Mac; informe de la base (normalización, diccionario de 63 columnas, 45 restricciones).
2. **Login y seguridad:** `Clave` con PBKDF2, `AuthService` con bloqueo tras 5 intentos, `SesionFilter` (401, 403 por rol, 415 sin JSON) y la red de seguridad para errores no previstos (500 en JSON, sin la traza de Tomcat). Dos revisiones con la lista de seguridad 6.2.
3. **Funciones de dueño de la tienda:** reportes, ingreso de mercadería (`IngresoService`, kárdex INGRESO), API y página de usuarios, decisiones de la entrevista (15 % desde 3 prendas, tope de 10 % del vendedor, lema, tallas L–4XL) y la paleta negro y fucsia con modo oscuro.
4. **Revisión de los PR del equipo:** antes de aprobar, la IA unía cada rama con `develop` en la nube, corría todas las pruebas JUnit y abría las pantallas con un navegador automático.
5. **Gestión como suplente:** README, acta de la entrevista real, informe v1 y final, plan de pruebas CP-01 a CP-18 con sus resultados, diagrama de clases, de capas y de secuencia, y manual de usuario con capturas reales.

### Errores de la IA y cómo se corrigieron:
- Dijo que `develop` tenía 214 pruebas y eran 227; otra vez dijo 312 cuando ya eran 327. Se corrigió contando las pruebas en NetBeans. **Lección: los números se comprueban, no se recuerdan.**
- Dio comandos de consola con nombres equivocados (`datos` en vez de `items`, `numero` en vez de `comprobante`, la columna `fecha` en vez de `fecha_hora`). Fallaron al probarlos y se corrigieron leyendo las clases y la tabla reales.
- Sumó mal un total esperado (S/ 404.60 en lugar de 404.40). El sistema estaba bien; se detectó comparando con la pantalla.
- Al cambiar de rama en su copia, dejó archivos sin guardar que pasaron a otra rama; se limpió antes de entregar. En mi PC no pasó porque los archivos se copiaban por rama.
- Al copiar archivos a mi PC, a veces llegaba la versión vieja. Desde entonces, la IA vuelve a leer el archivo de mi PC y lo compara antes del commit.

### Errores míos que la IA ayudó a detectar:
- Un `git add` largo se partió en PowerShell y pegó texto dentro de `UsuarioDao.java`; se detectó comparando la rama subida con lo probado (commit `46508d6`). Desde entonces: un `git add` por archivo y nunca `git add .`.
- Escribí `recrear_bd demo` en lugar de `bd\recrear_bd.cmd demo`.

### Cómo se comprobó:
- Cada cambio se probó primero en la nube (pruebas JUnit con una base PostgreSQL temporal y capturas de pantalla automáticas) y luego en mi computadora con NetBeans, Tomcat y pgAdmin.
- Antes de cada commit, la IA comparaba el archivo de mi PC con el que había probado.
- Los casos CP-01 a CP-16 los ejecuté yo en mi PC el 16 de noviembre, con capturas.
- Lo que no entendía, lo pedía explicado con ejemplos sencillos (por ejemplo: "la venta es una caja fuerte: entra todo o no entra nada") y hacía preguntas de práctica para la sustentación.

## Registro de Asistencia Técnica · Cesia

* **Fecha:** 9 al 23 de noviembre de 2026  
* **Participante:** Cesia Chapilliquen (Web, diseño y comprobante)  
* **Herramienta:** Claude (Anthropic). Cada sesión empezaba con mi reporte de avance y el archivo de contexto del proyecto, y yo ejecutaba los comandos en mi terminal.  

### Principales consultas y contribuciones asistidas:
1. **Base de las páginas (`plantilla.html`, `api.js`, `layout.js`):** el cliente de la API con `fetch` (get, post, put, delete y manejo de 401) y el menú según el rol, armado con `createElement` y `textContent`.
2. **Ingreso (`login.html`, `login.js`, `login.css`):** panel dividido, botón para mostrar la clave, validación de campos vacíos y mensajes del servidor (401 y 423).
3. **Estilos base (PR #33):** Bootstrap 5.3.3 local, favicon, logo, `estilos.css` con variables de color, navbar colapsable armada desde `layout.js` y estilos de impresión (`@media print`).
4. **Inicio (`index.html`, `index.js`):** saludo, tarjetas con `GET /api/estadisticas/hoy` y accesos rápidos (Reportes solo para el administrador).
5. **Comprobante (`comprobante.html`, `comprobante.js`, `comprobante.css`, PR #47):** se arma con `GET /api/ventas/{id}`, muestra descuento, operación gravada, IGV y total tal como vienen del servidor, sello ANULADA y botón Imprimir.
6. **Usuarios:** una primera versión de `usuarios.html` y `usuarios.js`, ajustada a la API real. No se subió: se usó la de Rafael como mi suplente (PR #60).
7. **Revisión de Pull Requests (#31, #32, #50, #53, #60 y #64):** pasos de prueba y lista de seguridad 6.2. También una guía de estudio para la sustentación.

### Errores de la IA y cómo se corrigieron:
- La primera versión de `api.js` y `layout.js` tenía 7 problemas: redirigía a `login.jsp`, leía `data.mensaje` en vez de `data.error`, sacaba al usuario también con 403, no tenía PUT ni DELETE, la ruta de estadísticas era otra, el menú no cambiaba por rol y el login se recargaba solo. Los encontré en la revisión y probando en el navegador.
- En `plantilla.html` quedaron repetidos los scripts de `api.js` y `layout.js`, y la consola decía que `API` ya estaba declarada. Borré los repetidos.
- El comprobante decía "Boleta de venta interna", y podía confundirse con una boleta de SUNAT. Lo cambié a "Comprobante interno, sin valor tributario. No válido como boleta ni factura". El lema provisional del encabezado tampoco era el de la tienda; lo cambié a "Más que una talla" cuando Rafael confirmó la decisión del dueño.
- En el comprobante, la línea sobre el TOTAL salía partida y el botón "Ver ventas" parecía desactivado. Se corrigió con `column-gap: 0` y un botón con borde oscuro.
- En `usuarios.js` usó el campo `id`, pero la API devuelve `idPersona`; lo vi en el JSON real. Además, al editar sin reenviar teléfono, correo y dirección, el servidor los dejaba en `null`.
- Varias veces el navegador mostró archivos viejos (HTML nuevo con JavaScript viejo) y daba "Cannot set properties of null". Se arregló vaciando la caché.

### Errores míos que la IA ayudó a detectar:
- Dejé `usuarios.html` en `WEB-INF`, una carpeta privada que Tomcat no sirve, y lo moví fuera del repositorio.
- Pegué dos comandos en una sola línea y Git dijo "unknown switch". Desde entonces pego los comandos uno por uno.

### Cómo se comprobó:
- Clean and Build y las pruebas JUnit en NetBeans; después de traer `main` (v1.0) recreé la base con `recrear_bd.cmd`