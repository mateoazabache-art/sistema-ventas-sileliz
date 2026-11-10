# Plan de Pruebas - Tarea 11 (SistemaVentas-Sileliz)

**Autor:** Mateo Javier Azabache Valle  
**Institución:** Tecsup  

## Casos de Prueba y Resultados de Integración

| ID Caso | Módulo / Componente | Descripción de la Prueba | Resultado Esperado | Estado Actual |
| :--- | :--- | :--- | :--- | :--- |
| **CP-01** | Modelos (POJOs) | Instanciación de `Categoria`, `Producto` y `VarianteProducto`. | Correcta asignación y encapsulamiento de atributos. | **Aprobado** (PR #15) |
| **CP-02** | Lógica de Dominio | Ejecución de pruebas en `VarianteProductoTest.java`. | Los métodos de validación de stock devuelven los valores esperados. | **Aprobado** (PR #15) |
| **CP-03** | Validaciones (Backend) | Verificación de reglas de DNI, celdas y correos en `Validador.java`. | Retorna true/false de forma estricta según las expresiones regulares. | **Aprobado** (PR #16) |
| **CP-04** | Validaciones (Frontend) | Pruebas de restricciones en formularios mediante `validaciones.js`. | Bloquea el envío si los campos obligatorios o formatos fallan. | **Aprobado** (PR #16) |
| **CP-05** | Gestión de Productos | Carga de categorías asíncronas y manipulación de tabla de variantes. | Renderizado correcto y bloqueo de SKUs duplicados. | **Aprobado** (PR #17) |