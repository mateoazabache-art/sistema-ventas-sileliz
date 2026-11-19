#!/bin/bash
# =====================================================================
#  recrear_bd.sh  -  Gestion de Ventas Sileliz.mas  (tarea 14 de Rafael)
#  Version Mac / Linux de recrear_bd.cmd: hace exactamente lo mismo.
#
#  Borra ventas_db y la vuelve a crear DESDE CERO (plan 4.5, RNF-09, CP-17).
#  ATENCION: se pierden los datos que tenga la base. Es para desarrollo.
#
#  USO (en la Terminal, desde la carpeta del repo):
#      bash bd/recrear_bd.sh          base limpia con los datos de prueba
#      bash bd/recrear_bd.sh demo     ademas, 14 ventas de ejemplo (Inicio y Reportes)
#
#  Antes: tener SistemaVentas/src/java/config.properties con su clave en db.clave.
#  Pide UNA vez la clave del usuario postgres (la de pgAdmin).
#  Se conecta primero a template1 (existe siempre) para poder borrar ventas_db.
# =====================================================================
cd "$(dirname "$0")" || exit 1

# --- 1. psql: el del PATH, el de Postgres.app o el del instalador de EDB ---
PSQL=""
for p in "$(command -v psql)" \
         /Applications/Postgres.app/Contents/Versions/latest/bin/psql \
         /Library/PostgreSQL/18/bin/psql /Library/PostgreSQL/17/bin/psql /Library/PostgreSQL/16/bin/psql; do
    if [ -n "$p" ] && [ -x "$p" ]; then PSQL="$p"; break; fi
done
if [ -z "$PSQL" ]; then
    echo "[ERROR] No se encontro psql. Instale PostgreSQL o agregue su carpeta bin al PATH."
    exit 1
fi

# --- 2. La clave de ventas_app sale de config.properties (db.clave) ---
CONFIG="../SistemaVentas/src/java/config.properties"
if [ ! -f "$CONFIG" ]; then
    echo "[ERROR] Falta $CONFIG"
    echo "        Copie config.example.properties con el nombre config.properties y ponga su clave en db.clave."
    exit 1
fi
CLAVE_APP="$(grep '^db.clave=' "$CONFIG" | head -1 | cut -d= -f2- | tr -d '\r')"
if [ -z "$CLAVE_APP" ]; then
    echo "[ERROR] config.properties no tiene la linea db.clave=..."
    exit 1
fi
if [ "$CLAVE_APP" = "CAMBIAR_POR_SU_CLAVE" ]; then
    echo "[ERROR] En config.properties cambie CAMBIAR_POR_SU_CLAVE por su clave real."
    exit 1
fi

# --- 3. Con o sin ventas de ejemplo ---
DEMO=false
[ "$1" = "demo" ] && DEMO=true

# --- 4. UTF-8 para las tildes y la n ---
export PGCLIENTENCODING=UTF8

echo
echo "Se BORRARA ventas_db y se creara de nuevo (ventas de ejemplo: $DEMO)."
echo "psql: $PSQL"
echo "Escriba la clave del usuario postgres cuando la pida."
echo
if ! "$PSQL" -X -h localhost -U "${PGUSER:-postgres}" -d template1 \
        -v clave="$CLAVE_APP" -v demo="$DEMO" -f recrear_bd.sql; then
    echo
    echo "[ERROR] La recreacion se detuvo. Lea el mensaje de arriba."
    echo "        Si dice \"being accessed by other users\": detenga Tomcat y cierre pgAdmin."
    exit 1
fi
echo
echo "[OK] ventas_db recreada. Ya puede hacer Run en NetBeans."
