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
