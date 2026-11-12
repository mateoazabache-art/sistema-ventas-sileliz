# Plan de Pruebas y Casos de Validación - Sileliz.mas

Este documento detalla los casos de prueba funcionales, de seguridad y de integración que garantizan el correcto funcionamiento del sistema bajo los requerimientos del plan de trabajo v3.1.

## Matriz de Casos de Prueba

| ID | Prueba | Resultado Esperado | Requisito | Responsable |
|---|---|---|---|---|
| **CP-01** | Ingresar con usuario y clave correctos | Entra a Inicio; la clave no aparece en el log ni en la respuesta | RF-01[cite: 7, 32] | Cesia[cite: 32] |
| **CP-02** | Ingresar con clave incorrecta | Mensaje genérico y se suma un intento fallido | RF-01[cite: 7, 32] | Cesia[cite: 32] |
| **CP-03** | Cinco intentos fallidos seguidos | Cuenta bloqueada por 15 minutos, con aviso HTTP 423 | RF-02[cite: 7, 32] | Rafael[cite: 32] |
| **CP-04** | Vendedor llama a `/api/usuarios` | Responde HTTP 403 y la página no muestra datos | RF-03[cite: 7, 32] | Rafael[cite: 32] |
| **CP-05** | Crear producto con precio 0 o stock negativo | Rechazado en JavaScript y en el servidor con HTTP 400 | RF-05[cite: 7, 32] | Naomi[cite: 32] |
| **CP-06** | Registrar cliente con DNI repetido o de 7 dígitos | Mensaje claro (HTTP 409 o 400) y no se guarda | RF-08[cite: 7, 32] | Mateo[cite: 32] |
| **CP-07** | Vender 2 prendas con IGV incluido | Base, IGV y total correctos; el stock baja; aparece en Inicio y Reportes | RF-09, 14, 15[cite: 7, 32] | Luz[cite: 32] |
| **CP-08** | Vender más que el stock disponible | Aviso de error; no se guarda nada y el stock no cambia | RF-10[cite: 7, 32] | Luz[cite: 32] |
| **CP-09** | Dos navegadores venden la última unidad al mismo tiempo | Solo una venta se guarda de forma atómica | RF-10[cite: 7, 32] | Luz[cite: 32] |
| **CP-10** | Anular una venta como Administrador | Estado pasa a ANULADA y el stock se repone con su movimiento | RF-13[cite: 7, 32] | Luz[cite: 32] |
| **CP-11** | Paginar productos, clientes y ventas, buscar y filtrar | Navegación fluida sin errores al cambiar de página | RF-04, 06, 07, 12[cite: 7, 32] | Naomi y Mateo[cite: 32] |
| **CP-12** | Imprimir el comprobante | Se visualiza completo en A4 con la leyenda "Comprobante interno" | RF-11, 16[cite: 7, 32] | Cesia[cite: 32] |
| **CP-13** | Validar diseño en diferentes resoluciones | Sin desbordes y con menú colapsable (360, 768 y 1280 px) | RNF-05[cite: 8, 32] | Cesia[cite: 32] |
| **CP-14** | Inyección SQL (`' OR '1'='1`) en buscadores y login | No altera la consulta ni permite el ingreso al sistema | RNF-02[cite: 8, 32] | Rafael[cite: 32] |
| **CP-15** | Ataque XSS (`<script>alert(1)</script>`) en nombres | Se muestra puramente como texto plano y no se ejecuta | RNF-02[cite: 8, 32] | Cesia[cite: 32] |
| **CP-16** | Enviar POST a la API con tipo formulario incorrecto | Rechazado de inmediato por el filtro con HTTP 415 | RNF-02[cite: 8, 32] | Rafael[cite: 32] |
| **CP-17** | Recrear la base con `recrear_bd.cmd` | Mantiene la misma estructura e integridad en las 5 PCs | RNF-09[cite: 8, 32] | Rafael[cite: 32] |
