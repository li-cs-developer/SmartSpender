# =====================================================================
# SmartSpender Frontend — multi-stage build
#
# Stage 1: build the Vue app with Node
# Stage 2: serve the static output with Nginx
# =====================================================================

# ---------- Build stage ----------
FROM node:20-alpine AS build

WORKDIR /app

# Copy manifests first for better layer caching
COPY package.json package-lock.json* ./
RUN npm ci

# Copy source and build
COPY . .
RUN npm run build

# ---------- Runtime stage ----------
FROM nginx:1.27-alpine

# Our custom nginx config handles the SPA fallback + API proxy
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy the built app from the previous stage
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]