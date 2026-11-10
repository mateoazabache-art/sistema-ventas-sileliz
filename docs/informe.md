# Informe Técnico del Proyecto - Sistema de Gestión de Ventas Sileliz.mas

## 1. Marco del Proyecto
### 1.1 Problema y Justificación
Sileliz.mas es una tienda dedicada a la venta de ropa plus size. El registro manual de ventas, el control de inventario por talla y color de forma desarticulada y la emisión de comprobantes no integrados generan errores críticos de stock, ventas sin respaldo formal y escasa visibilidad analítica. Este proyecto implementa una aplicación web centralizada para automatizar el catálogo, clientes, ventas transaccionales, comprobantes internos y reportes con control de accesos por roles.

### 1.2 Objetivos
- **Objetivo General:** Desarrollar y sustentar una aplicación web en Java (Servlets y POO) y PostgreSQL que gestione de manera segura las ventas de Sileliz.mas.
- **Objetivos Específicos:** Diseñar una base de datos normalizada en 3FN, implementar una arquitectura en capas e integrar seguridad con PBKDF2.

## 2. Requisitos y Casos de Uso
### 2.1 Requisitos Funcionales (Debe)
- **RF-01 / RF-02:** Iniciar y cerrar sesión con control de roles y bloqueo de cuenta tras 5 intentos fallidos[cite: 7].
- **RF-05:** Gestión integral de productos con variantes (talla, color, stock y stock mínimo)[cite: 7].
- **RF-08:** Gestión de clientes con búsqueda por nombre o DNI[cite: 7].
- **RF-09 / RF-10:** Registro transaccional de ventas con carrito, descuentos, cálculo de IGV (18%) y descuento atómico de stock[cite: 7, 14].
- **RF-11:** Emisión de comprobantes internos con serie y correlativo[cite: 7].

### 2.2 Reglas de Negocio
- **RN-01:** El precio de venta incluye el IGV (18%)[cite: 7, 14].
- **RN-02 / RN-03:** El stock se controla por variante y no se permite sobreventa[cite: 7].
- **RN-05:** Las entidades con historial de ventas no se eliminan, se desactivan (`activo = false`)[cite: 7].

## 3. Arquitectura del Sistema
1. **Frontend:** Páginas HTML, Bootstrap 5.3.3 y JavaScript (`fetch`)[cite: 5, 12] usando `textContent` contra XSS[cite: 22].
2. **Filtro de Seguridad:** `SesionFilter` para validar sesiones y roles[cite: 12, 22].
3. **Servlets API:** Reciben JSON vía Gson y responden códigos HTTP estándar[cite: 11, 13, 19].
4. **Servicios:** Lógica de negocio y transacciones JDBC (`setAutoCommit(false)`)[cite: 12, 13, 19].
5. **DAO:** Consultas SQL parametrizadas con `PreparedStatement` sobre PostgreSQL[cite: 10, 11, 12, 22].
