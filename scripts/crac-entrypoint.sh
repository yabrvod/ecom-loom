#!/bin/bash
# CRaC entrypoint: restaura desde checkpoint si existe, sino arranca normal
# y toma checkpoint después del warmup.

CHECKPOINT_DIR="/app/checkpoint"
JAVA_OPTS="${JAVA_TOOL_OPTIONS:--XX:MaxRAMPercentage=75.0 -XX:+UseG1GC}"

if [ -d "$CHECKPOINT_DIR/core" ] && [ -f "$CHECKPOINT_DIR/core/dump4.img" ]; then
    echo "[CRaC] Restaurando desde checkpoint..."
    exec java $JAVA_OPTS \
        -XX:CRaCRestoreFrom="$CHECKPOINT_DIR" \
        -Dspring.profiles.active="${SPRING_PROFILES_ACTIVE:-prod}"
else
    echo "[CRaC] Primer arranque — iniciando app para crear checkpoint..."
    java $JAVA_OPTS \
        -XX:CRaCCheckpointTo="$CHECKPOINT_DIR" \
        -Dspring.profiles.active="${SPRING_PROFILES_ACTIVE:-prod}" \
        -jar /app/app.jar &

    APP_PID=$!

    # Esperar que la app arranque (usar curl, no wget)
    echo "[CRaC] Esperando arranque..."
    until curl -sf http://127.0.0.1:8091/actuator/health 2>/dev/null | grep -q '"UP"'; do
        sleep 3
    done
    echo "[CRaC] App lista. Calentando JIT..."

    # Warmup: 50 requests para calentar el JIT
    for i in $(seq 1 50); do
        curl -sf http://127.0.0.1:8090/ > /dev/null 2>&1
        curl -sf http://127.0.0.1:8090/productos/laptop-pro-15 > /dev/null 2>&1
        curl -sf http://127.0.0.1:8090/carrito > /dev/null 2>&1
    done
    echo "[CRaC] Warmup completado. Tomando checkpoint..."

    # Tomar checkpoint via jcmd (disponible en Zulu CRaC JDK)
    jcmd $APP_PID JDK.checkpoint

    echo "[CRaC] Checkpoint guardado en $CHECKPOINT_DIR"
    wait $APP_PID
fi
