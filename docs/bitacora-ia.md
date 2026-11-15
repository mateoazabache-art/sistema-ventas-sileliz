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