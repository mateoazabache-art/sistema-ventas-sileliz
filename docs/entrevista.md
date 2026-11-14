# Acta de Entrevista y Requerimientos - Sileliz.mas
**Fecha de entrevista:** 28 de octubre de 2026  
**Entrevistador:** Mateo Azabache (Gestión del Proyecto)  
**Entrevistado (Representante de la tienda):** Rafael (Dueño / Administrador de Sileliz.mas)  

---

### 1. Resumen de Acuerdos y Reglas de Negocio Validadas

* **Sistema de Precios e IGV (RN-01):**  
  Se acordó que los precios de las prendas mostrados en el catálogo **ya incluyen el IGV (18%)**[cite: 7, 14]. El sistema calculará automáticamente la base imponible dividiendo el total entre 1.18 y el IGV restando el total menos la base[cite: 7].

* **Control de Stock (RN-02 y RN-03):**  
  El inventario no se maneja por producto general, sino estrictamente por **variantes** (combinación única de Producto, Talla y Color)[cite: 7]. El stock se descuenta de forma atómica al momento de registrar la venta para evitar sobreventa[cite: 6, 7].

* **Métodos de Pago:**  
  Los medios de pago aceptados en la tienda física para las ventas de mostrador son: **EFECTIVO, YAPE, PLIN, TARJETA y TRANSFERENCIA**[cite: 14].

* **Descuentos y Promociones:**  
  Se implementará la interfaz de polimorfismo *Descuento* con dos variantes principales: *SinDescuento* y *DescuentoPorcentaje*[cite: 19]. Como propuesta de tienda, se validó aplicar un **10% de descuento a partir de la tercera prenda** en el carrito (*DescuentoPorCantidad*)[cite: 19].

* **Comprobantes y Validez Tributaria (RN-07):**  
  La tienda **no emite facturación electrónica ante SUNAT** dentro de este sistema[cite: 4, 7]. Se emite un **comprobante interno (boleta de venta interna)** con serie y correlativo (ej. `B001-000015`) que lleva impreso obligatoriamente el rótulo: *"Comprobante interno, sin valor tributario"*[cite: 7].

* **Gestión de Clientes (RN-06):**  
  Se permite realizar ventas rápidas a clientes anónimos o "Consumidor Final". En el comprobante y el registro de venta, si no se selecciona un cliente, aparecerá por defecto como **"Cliente: Varios"**[cite: 7, 14].

* **Seguridad y Roles (RN-05 y RN-11):**  
  El sistema operará bajo dos roles estrictos: **Administrador** (control total de usuarios, anulación de ventas con motivo, reportes y catálogo) y **Vendedor** (registro de ventas, gestión de clientes y consulta de catálogo)[cite: 6, 7]. Las claves de acceso se almacenan cifradas con **PBKDF2** y la cuenta se bloquea por 15 minutos tras 5 intentos fallidos[cite: 6, 7, 22].
