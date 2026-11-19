## Lista de Seguridad 6.2 (El revisor debe marcar todo antes de aprobar)
- [ ] Todo SQL usa PreparedStatement con ? y ninguna concatenación de texto del usuario.
- [ ] Ningún archivo JavaScript usa innerHTML con un dato; se usa textContent.
- [ ] Todo endpoint nuevo pasa por SesionFilter y valida el rol cuando corresponde.
- [ ] El servidor no confía en precios, totales ni stock enviados por el navegador.
- [ ] Los mensajes de error no muestran datos técnicos, rutas ni consultas.
- [ ] No hay claves, tokens ni rutas personales en el código ni en los archivos subidos.
- [ ] Hay una prueba nueva o actualizada para lo que cambió.
- [ ] Clean and Build termina en BUILD SUCCESSFUL y el revisor ejecutó la pantalla en su computadora.
