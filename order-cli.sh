#!/usr/bin/env bash
#
# order-cli.sh — friendly wrapper around the MCP order-tools command-line client.
#
# It locates (and, if needed, builds) the mcp-client jar, checks that the MCP
# server is reachable, forwards your command to it over Streamable HTTP, and
# pretty-prints the JSON responses when `jq` is installed.
#
# Examples:
#   ./order-cli.sh tools
#   ./order-cli.sh create alice SKU-1 Widget 2 9.99
#   ./order-cli.sh list
#   ./order-cli.sh list PAID
#   ./order-cli.sh get ORD-1001
#   ./order-cli.sh status ORD-1001 PAID
#   ./order-cli.sh cancel ORD-1001
#
# Point at a different server:
#   ORDER_MCP_URL=http://my-host:9000 ./order-cli.sh tools
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR="$SCRIPT_DIR/mcp-client/target/mcp-client-0.0.1-SNAPSHOT.jar"
SERVER_URL="${ORDER_MCP_URL:-http://localhost:8080}"

build_if_needed() {
  if [[ ! -f "$JAR" ]]; then
    echo "→ client jar not found; building it once (mvn package)…" >&2
    ( cd "$SCRIPT_DIR" && mvn -q -pl mcp-client -am package -DskipTests )
  fi
}

check_server() {
  # Any HTTP response on /mcp proves the port is listening (curl only fails on
  # a connection/timeout error, not on a 4xx status).
  if ! curl -s -o /dev/null --max-time 3 "$SERVER_URL/mcp"; then
    cat >&2 <<EOF
✗ Cannot reach the MCP server at $SERVER_URL

  Start it in another terminal:
      mvn -pl mcp-server spring-boot:run

  …or point this tool at a different server:
      ORDER_MCP_URL=http://host:port $0 $*
EOF
    exit 1
  fi
}

print_help() {
  cat <<EOF

Order MCP client — calls the order-tools server over Streamable HTTP.

Usage: $(basename "$0") <command> [args]

  tools                                          list available tools
  create <customer> <sku> <name> <qty> <price>   create an order
  get <orderId>                                  fetch one order
  list [STATUS]                                  list orders (optional status filter)
  status <orderId> <STATUS>                      update an order's status
  cancel <orderId>                               cancel an order
  help                                           show this help

STATUS is one of: NEW, PAID, SHIPPED, DELIVERED, CANCELLED
Server URL (override): ORDER_MCP_URL=http://host:port $(basename "$0") <command>

Examples:
  $(basename "$0") create alice SKU-1 Widget 2 9.99
  $(basename "$0") list PAID
  $(basename "$0") status ORD-1001 SHIPPED
EOF
}

# Help / no-args is handled locally — no JVM, no server needed.
case "${1:-}" in
  help|-h|--help|"") print_help; exit 0 ;;
esac

build_if_needed
check_server "$@"

run() {
  java -jar "$JAR" \
    --spring.ai.mcp.client.streamable-http.connections.order-server.url="$SERVER_URL" \
    "$@"
}

# Pretty-print JSON output lines when jq is available; pass other lines through.
if command -v jq >/dev/null 2>&1; then
  run "$@" | while IFS= read -r line; do
    if printf '%s' "$line" | jq -e . >/dev/null 2>&1; then
      printf '%s' "$line" | jq .
    else
      printf '%s\n' "$line"
    fi
  done
else
  run "$@"
fi