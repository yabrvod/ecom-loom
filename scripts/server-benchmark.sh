#!/usr/bin/env bash
# ============================================================
# Benchmark en servidor — mismas condiciones que producción
# Uso: ./scripts/server-benchmark.sh [spring|node|both]
# ============================================================
set -euo pipefail

TARGET=${1:-both}
SCRIPT_DIR="/opt/spring-ecommerce/loadtest"
RESULTS_DIR="/tmp/benchmark-results"
mkdir -p "$RESULTS_DIR"

log() { echo "[$(date '+%H:%M:%S')] $*"; }

run_k6() {
    local label=$1 url=$2 out=$3
    log "k6 → $label ($url)"
    /usr/bin/k6 run -e BASE_URL="$url" \
        --out "json=${RESULTS_DIR}/${out}.json" \
        "$SCRIPT_DIR/k6-ecommerce.js" 2>&1 | \
        grep -E "p\(95\)|avg=|http_reqs|cart_lat|checkout_lat|catalog_lat|errors|✓|✗"
    log "$label completado"
}

warmup() {
    local url=$1 label=$2
    log "Warmup $label (30 requests)..."
    for i in $(seq 1 30); do
        curl -sf "$url/" -o /dev/null 2>/dev/null
        curl -sf "$url/productos/laptop-pro-15" -o /dev/null 2>/dev/null
    done
    log "Warmup $label OK"
}

start_test_container() {
    log "Levantando ecom-test (1 vCPU / 768MB)..."
    docker stop ecom-test 2>/dev/null || true
    docker rm ecom-test   2>/dev/null || true
    docker compose -f /opt/spring-ecommerce/compose.test.yml \
        --env-file /opt/spring-ecommerce/.env up -d ecom-test
    log "Esperando arranque JVM..."
    until curl -sf http://127.0.0.1:9095/ -o /dev/null 2>/dev/null; do sleep 5; done
    log "ecom-test LISTA"
}

stop_test_container() {
    docker stop ecom-test 2>/dev/null || true
    docker rm   ecom-test 2>/dev/null || true
    log "ecom-test eliminado"
}

echo ""
echo "════════════════════════════════════════════════════"
echo " Benchmark en servidor — $(date '+%Y-%m-%d %H:%M')"
echo "════════════════════════════════════════════════════"

if [[ "$TARGET" == "spring" || "$TARGET" == "both" ]]; then
    start_test_container
    warmup "http://127.0.0.1:9095" "Spring"
    echo ""
    echo "━━━ SPRING jOOQ (1 vCPU / 768MB) ━━━"
    run_k6 "Spring" "http://127.0.0.1:9095" "spring"
    stop_test_container
fi

if [[ "$TARGET" == "node" || "$TARGET" == "both" ]]; then
    echo ""
    echo "━━━ NODE.JS (en producción — misma BD) ━━━"
    warmup "http://127.0.0.1:9094" "Node"
    run_k6 "Node" "http://127.0.0.1:9094" "node"
fi

echo ""
echo "════════════════════════════════════════════════════"
echo " Resultados guardados en $RESULTS_DIR"
echo "════════════════════════════════════════════════════"
