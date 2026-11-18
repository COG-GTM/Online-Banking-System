#!/usr/bin/env bash
# Usage: parity/run.sh <app.jar> <java_home> <out_dir>
# Resets the OnlineBankingSystem MySQL database, starts the jar, seeds roles,
# replays parity/capture.py and stops the app.
set -euo pipefail
JAR=$1 JHOME=$2 OUT=$3
HERE=$(cd "$(dirname "$0")" && pwd)
MYSQL_ROOT=${MYSQL_ROOT:-"docker exec -i obs-mysql mysql -uroot"}
PORT=${PORT:-8080}
export MYSQL_CMD=${MYSQL_CMD:-"$MYSQL_ROOT OnlineBankingSystem"}

echo "DROP DATABASE IF EXISTS OnlineBankingSystem; CREATE DATABASE OnlineBankingSystem;" | $MYSQL_ROOT
rm -rf "$OUT"; mkdir -p "$OUT"
"$JHOME/bin/java" -jar "$JAR" --server.port="$PORT" > "$OUT.app.log" 2>&1 &
APP=$!
trap 'kill $APP 2>/dev/null; wait $APP 2>/dev/null || true' EXIT
for _ in $(seq 1 90); do
  curl -sf -o /dev/null "http://localhost:$PORT/index" && break
  sleep 1
done
BASE_URL="http://localhost:$PORT" python3 "$HERE/capture.py" "$OUT"
