#!/bin/bash
cd -- "$(dirname "$BASH_SOURCE")"
VISIBLE_DIR="$PWD"
BASE_DIR="$PWD/backend"
LOG_FILE="$BASE_DIR/.hub_output.log"
INFO_FILE="$VISIBLE_DIR/CONNECTION_INFO.txt"

clear
echo "------------------------------------------------"
echo "        AETHERSHELL HUB SYSTEM STARTUP          "
echo "------------------------------------------------"

# 1. Reset & Cleanup
echo "[0/2] Cleaning up background processes..."
chmod +x ./backend/vision_engine
chmod +x ./backend/ngrok
chmod +x ./backend/cloudflared
pkill -f "hub.jar"
pkill -f "vision_engine"
pkill -f "cloudflared"
pkill -f "ngrok"
# This clears the port if a previous crash left it stuck
lsof -ti:5001 | xargs kill -9 > /dev/null 2>&1
> "$LOG_FILE"
echo "Initializing System..." > "$INFO_FILE"

# 2. Launch Vision Engine Binary
echo "[1/2] Launching Vision AI Worker..."
chmod +x "$BASE_DIR/vision_engine"
"$BASE_DIR/vision_engine" >> "$LOG_FILE" 2>&1 &
PYTHON_PID=$!

# 3. Start Java Hub
echo "[2/2] Booting Java Hub..."
cd "$BASE_DIR"
java -Dspring.web.resources.static-locations="file:$BASE_DIR/static/" \
     -jar "hub.jar" > "$LOG_FILE" 2>&1 &
JAVA_PID=$!

# 4. Wait for Tunnel Connections
cd "$VISIBLE_DIR"
echo "[SYSTEM] Establishing Tunnels..."
# Wait until BOTH Cloudflare and Ngrok have printed their URLs
while ! grep -q "trycloudflare.com" "$LOG_FILE" || ! grep -q "ngrok-free.app" "$LOG_FILE"; do
    if ! kill -0 $JAVA_PID 2>/dev/null; then
        echo "CRITICAL ERROR: Java Hub failed to start. Check $LOG_FILE"
        exit 1
    fi
    sleep 1
done

# 5. Generate Connection Dashboard
{
echo "================================================="
echo "          AETHERSHELL CONNECTION INFO            "
echo "          $(date)                                "
echo "================================================="
echo ""
echo "--- [ NDS CONFIGURATION ] ---"
echo "Paste these lines into your config.txt:"
echo "-------------------------------------------------"
sed -n '/SERVER_HOST/,/SERVER_PORT/p' "$LOG_FILE"
echo "-------------------------------------------------"
echo ""
echo "--- [ IPHONE / IPAD LINK ] ---"
CLOUDFLARE_URL=$(grep -o "https://.*\.trycloudflare\.com" "$LOG_FILE" | head -1)
echo "URL: $CLOUDFLARE_URL"
echo "ID:  $(echo $CLOUDFLARE_URL | sed 's|https://||' | sed 's|\.trycloudflare\.com||')"
echo ""
echo "--- [ QR CODE ] ---"
sed -n '/\[ SCAN FOR IPHONE \]/,/ID:/p' "$LOG_FILE" | grep "█"
echo ""
echo "================================================="
} > "$INFO_FILE"

clear
echo "------------------------------------------------"
echo "        AETHERSHELL IS NOW ONLINE               "
echo "------------------------------------------------"
echo "  > Connection info saved to: CONNECTION_INFO.txt"
echo "  > Tunnel: $CLOUDFLARE_URL"
echo "------------------------------------------------"
echo "  PRESS CTRL+C TO SHUT DOWN ALL SYSTEMS         "
echo "------------------------------------------------"

open "$INFO_FILE"

trap "echo 'Shutting down...'; kill $JAVA_PID $PYTHON_PID; pkill -f cloudflared; exit" SIGINT SIGTERM
wait $JAVA_PID