#!/bin/bash
# =====================================================================
#  respaldar_bd.sh  -  Gestion de Ventas Sileliz.mas  (tareas 14 y 17 de Rafael)
#  Version Mac / Linux de respaldar_bd.cmd.
#
#  Guarda una copia completa de ventas_db (estructura + datos) con pg_dump en:
#      ~/respaldos-sileliz/ventas_db_AAAA-MM-DD_HHMM.sql   (fuera del repositorio)
#
#  RESTAURAR (borra lo que tenga ventas_db):
#      1. bash bd/recrear_bd.sh
#      2. psql -U postgres -d ventas_db -f ~/respaldos-sileliz/ARCHIVO.sql
# =====================================================================
PGDUMP=""
for p in "$(command -v pg_dump)" \
         /Applications/Postgres.app/Contents/Versions/latest/bin/pg_dump \
         /Library/PostgreSQL/18/bin/pg_dump /Library/PostgreSQL/17/bin/pg_dump /Library/PostgreSQL/16/bin/pg_dump; do
    if [ -n "$p" ] && [ -x "$p" ]; then PGDUMP="$p"; break; fi
done
if [ -z "$PGDUMP" ]; then
    echo "[ERROR] No se encontro pg_dump. Instale PostgreSQL o agregue su carpeta bin al PATH."
    exit 1
fi

CARPETA="$HOME/respaldos-sileliz"
mkdir -p "$CARPETA"
ARCHIVO="$CARPETA/ventas_db_$(date +%Y-%m-%d_%H%M).sql"
export PGCLIENTENCODING=UTF8

echo
echo "Respaldando ventas_db en:"
echo "    $ARCHIVO"
echo "Escriba la clave del usuario postgres cuando la pida."
echo
if ! "$PGDUMP" -h localhost -U "${PGUSER:-postgres}" -d ventas_db --clean --if-exists --no-owner -f "$ARCHIVO"; then
    echo
    echo "[ERROR] No se pudo respaldar. Lea el mensaje de arriba."
    exit 1
fi
echo
echo "[OK] Respaldo listo:"
ls -lh "$ARCHIVO"
