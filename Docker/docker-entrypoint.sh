#!/usr/bin/env sh
set -eu

# Start Spring Boot backend in background
echo "Starting Spring Boot backend..."
java $JAVA_OPTS -jar /app/app.jar &
JAVA_PID=$!

# Start Nginx in foreground
echo "Starting Nginx..."
nginx -g 'daemon off;' &
NGINX_PID=$!

trap 'echo "Stopping..."; kill -TERM ${NGINX_PID} ${JAVA_PID}; wait ${JAVA_PID} 2>/dev/null || true; wait ${NGINX_PID} 2>/dev/null || true; exit 0' TERM INT

# Busybox/dash 没有 wait -n，使用轮询保持前台并在任一进程退出时结束
while kill -0 ${NGINX_PID} 2>/dev/null && kill -0 ${JAVA_PID} 2>/dev/null; do
  sleep 1
done

# 等待两者以收集退出码（若已退出则 wait 返回非零或错误，忽略）
wait ${NGINX_PID} 2>/dev/null || true
wait ${JAVA_PID} 2>/dev/null || true
exit 0



