# Acta de entrevista con la tienda — Sileliz.mas

| Dato | Detalle |
|---|---|
| Fecha | Sábado 14 de noviembre de 2026 |
| Modalidad | Cuestionario escrito de 13 preguntas, respondido por el dueño |
| Entrevistado | Rafael Alva, dueño y administrador de Sileliz.mas |
| Preparó el acta | Rafael Alva, como suplente de Mateo en la gestión del proyecto (plan 7.1) |
| Compartida con | Cesia, Naomi, Luz y Mateo |

> **Nota sobre la fecha.** El plan preveía la entrevista para el 28 de octubre (tarea 5 de Mateo), pero no llegó a hacerse.
> Hasta hoy el equipo trabajó con los **supuestos** del plan (riesgo R7: "la tienda no responde a tiempo" → supuestos
> documentados y valores como constantes fáciles de cambiar). Esta acta confirma o corrige esos supuestos.
> Las correcciones ya se aplicaron al sistema (PR #50 y #51).

---

## 1. Situación actual de la tienda

| Pregunta | Respuesta del dueño |
|---|---|
| ¿Cómo registran hoy las ventas y el stock? | Con un sistema de ventas comercial llamado **Keyfácil**. |
| ¿Cuál es el problema que más molesta? | **No saber qué prendas se venden más.** |
| ¿Quiénes trabajan en la tienda? | El dueño administra; hay **dos vendedores**. |
| ¿Cada cuánto llega mercadería y de dónde? | **Todas las semanas**, de **Lima y de China**. |
| ¿Qué tallas manejan? | **Desde L hasta 4XL** (L, XL, 2XL, 3XL, 4XL). |

## 2. Respuestas que afectan al sistema

| N.º | Tema | Respuesta del dueño | Supuesto del plan | Decisión para el sistema |
|---|---|---|---|---|
| 1 | IGV | Hoy la tienda está en el **NRUS** y en trámite para pasar a **EIRL**. | RN-01: precio con IGV 18 % incluido | **Se mantiene RN-01** (IGV 18 % incluido; base = total / 1,18). Es lo trabajado en clase y deja el sistema listo para el régimen de la EIRL. `IGV_FACTOR` es una constante. |
| 2 | Métodos de pago | Yape, Plin, tarjetas, transferencia, efectivo y **pago link**. | Efectivo, Yape, Plin, Tarjeta, Transferencia | Se mantienen los 5. **Pago link queda para la versión 2** (cambia la tabla `venta`, `MetodoPago` y la página, con el código congelado). Mientras tanto se registra como Tarjeta. |
| 3 | Descuento por cantidad | **15 % desde 3 prendas.** | 10 % desde 3 prendas | **Cambiado** a 15 % en `DescuentoFactory` y en la vista previa de `venta-nueva.js` (PR #51). |
| 4 | Descuento especial | Durante la venta, el vendedor puede descontar **hasta 10 % por prenda**. | Cualquier porcentaje de 0 a 100 % | **Nueva regla:** el vendedor puede dar como máximo 10 % de descuento especial; el administrador no tiene tope (`VentaService.TOPE_PORCENTAJE_VENDEDOR`, PR #51). Como el porcentaje se aplica igual a todas las prendas, equivale a "10 % por prenda". |
| 5 | Stock mínimo | **2 unidades** para avisar stock bajo. | 2 por defecto (RN-10) | **Se confirma.** |
| 6 | Anulaciones | La tienda **no hace devoluciones**. Se anula una venta cuando la clienta pide su **boleta con DNI** (se anula y se emite otra con sus datos). | Solo el admin anula, con motivo (RN-08) | **Se confirma RN-08.** Motivo típico: "Cliente pidió boleta con DNI". |
| 7 | Reportes | Faltan: prendas que **más se venden**, las que **dejan mejor ganancia** y las que **se venden más rápido**. | Ventas por fechas, más vendidos, por vendedor | "Más vendidos" ya existe. **Ganancia y rotación quedan para la versión 2**: necesitan el costo de compra y la fecha de ingreso de cada lote, que el sistema no guarda. |
| 8 | Usuarios | Un administrador (el dueño) y dos vendedores. | Roles ADMINISTRADOR y VENDEDOR | **Se confirma.** La API de usuarios está en el PR #50; la página, a cargo de Cesia. |
| 9 | Fuera de alcance | La facturación electrónica ante SUNAT requiere un proveedor y tiene costo; no corresponde a un proyecto del instituto. | Comprobante interno sin valor tributario (RN-07) | **Se confirma RN-07.** |
| 10 | Lema de la tienda | **"Más que una talla"** | "Moda en tallas grandes" | **Cambiado** en `login.html` (PR #51) y en el comprobante (Cesia, PR #47). |

## 3. Mejoras para una versión 2 (no entran en la entrega del 20 de noviembre)

1. Método de pago **Pago link**.
2. Reporte de **ganancia por prenda** (requiere guardar el costo de compra en el ingreso de mercadería).
3. Reporte de **rotación**: prendas que se venden más rápido (días entre el ingreso y la venta).
4. Facturación electrónica ante SUNAT, mediante un proveedor autorizado.

## 4. Conformidad

El dueño revisó estas respuestas y las decisiones de la columna "Decisión para el sistema".
Cualquier cambio posterior se registra en este documento con fecha.
