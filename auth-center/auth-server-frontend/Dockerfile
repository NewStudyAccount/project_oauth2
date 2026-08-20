# ---- Build Stage ----
FROM node:20-alpine AS build
WORKDIR /app

# 先复制依赖配置，利用 Docker 缓存
COPY package.json package-lock.json ./
RUN npm ci

# 复制源码并构建
COPY . .
RUN npm run build

# ---- Runtime Stage ----
FROM nginx:alpine
WORKDIR /app

# 复制构建产物到 Nginx
COPY --from=build /app/dist /usr/share/nginx/html

# 复制 Nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
