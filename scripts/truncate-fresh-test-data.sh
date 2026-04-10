#!/usr/bin/env bash
# Truncate all FitCoach tables except users, coaches, trainees.
# Defaults match src/main/resources/application.properties (override with env vars).

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

PGHOST="${PGHOST:-localhost}"
PGPORT="${PGPORT:-5432}"
PGUSER="${PGUSER:-admin}"
PGDATABASE="${PGDATABASE:-fitcoach_db}"
export PGPASSWORD="${PGPASSWORD:-}"

echo "Truncating (keeping users, coaches, trainees) on ${PGUSER}@${PGHOST}:${PGPORT}/${PGDATABASE} ..."
psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" \
  -v ON_ERROR_STOP=1 \
  -f "$SCRIPT_DIR/truncate-fresh-test-data.sql"
echo "Done. Re-seed exercises/ingredients by restarting the app (DataSeedingService) or run your seed job."
