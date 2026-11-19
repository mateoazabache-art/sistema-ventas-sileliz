# Guía de Entorno de Desarrollo - Sistema Ventas Sileliz

Este documento detalla los pasos para configurar el entorno unificado del proyecto y ejecutar la prueba de humo.

## 1. Versiones Oficiales Instaladas
- **JDK:** Eclipse Temurin 17 (LTS) en `C:\Program Files\Java\jdk-17`
- **Servidor Web:** Apache Tomcat 10.1.61 en `C:\Tomcat\apache-tomcat-10.1.61`
- **IDE:** Apache NetBeans 31
- **Base de Datos:** PostgreSQL 18 (UTF-8) con pgAdmin 4
- **Control de Versiones:** Git 2.40+ (con `core.autocrlf = false`)

## 2. Pasos para la Prueba de Humo (Domingo 25 de Octubre)
Cada integrante debe ejecutar las siguientes verificaciones:
1. Confirmar Java 17 en CMD: `"%JAVA_HOME%\bin\java" -version`.
2. Crear `ventas_db` (UTF-8) y ejecutar `bd/00_practica.sql` en pgAdmin.
3. Hacer **Clean and Build** (`Shift + F11`) en NetBeans sobre `SistemaVentas`.
4. Desplegar en Tomcat 10.1 y abrir `http://localhost:8080/sileliz/`.
5. Verificar el endpoint de salud: `http://localhost:8080/sileliz/api/salud`.
6. Probar el diseño responsive desde un navegador móvil o emulador a 360px.
7. Publicar captura de pantalla en su Issue correspondiente.
