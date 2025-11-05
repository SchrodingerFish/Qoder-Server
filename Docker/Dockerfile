# ============================================================
# Multi-stage build for frontend (Vite) and backend (Spring Boot)
# Final image runs Nginx to serve frontend and reverse-proxy to Java backend
# ============================================================

# ---------- Frontend build (Vite React) ----------
FROM node:20-alpine AS frontend-builder
WORKDIR /app/frontend

# Install deps & build
COPY Qoder/package.json Qoder/package-lock.json ./
RUN npm ci
COPY Qoder/ ./

# Allow overriding API base at build time, default to /api for nginx proxy
ARG VITE_API_BASE_URL=/api
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
RUN npm run build


# ---------- Backend build (Spring Boot) ----------
FROM maven:3.9-eclipse-temurin-17 AS backend-builder
WORKDIR /app/backend
COPY Qoder-Server/pom.xml ./
RUN mvn -B -q -e -DskipTests dependency:go-offline
COPY Qoder-Server/ ./
RUN mvn -B -q -DskipTests clean package


# ---------- Runtime image (Debian + Nginx + JRE 17) ----------
FROM debian:bookworm-slim AS runtime
ENV TZ=Asia/Shanghai
RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
    nginx \
    openjdk-17-jre-headless \
    ca-certificates \
    dumb-init \
    tzdata \
    && ln -snf /usr/share/zoneinfo/$TZ /etc/localtime \
    && echo $TZ > /etc/timezone \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy frontend build
COPY --from=frontend-builder /app/frontend/dist/ /usr/share/nginx/html/

# Copy backend jar
COPY --from=backend-builder /app/backend/target/*.jar /app/app.jar

# Copy nginx conf and entry script
COPY nginx.conf /etc/nginx/nginx.conf
COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN chmod +x /usr/local/bin/docker-entrypoint.sh \
    && mkdir -p /var/cache/nginx /var/run/nginx

ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8
ENV JAVA_OPTS="-Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai -Xms256m -Xmx512m"

EXPOSE 80

ENTRYPOINT ["/usr/bin/dumb-init", "--"]
CMD ["/usr/local/bin/docker-entrypoint.sh"]



