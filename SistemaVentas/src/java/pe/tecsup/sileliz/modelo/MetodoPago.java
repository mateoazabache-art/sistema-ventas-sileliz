/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package pe.tecsup.sileliz.modelo;

/**
 * Metodo de pago de una venta. Los valores coinciden con el CHECK de la
 * columna venta.metodo_pago en la base de datos (seccion 4.1 del Plan).
 */
public enum MetodoPago {
    EFECTIVO,
    YAPE,
    PLIN,
    TARJETA,
    TRANSFERENCIA
}
