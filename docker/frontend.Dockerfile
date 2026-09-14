FROM node:22-alpine AS build
WORKDIR /build
COPY ai-coding-frontend/package.json ai-coding-frontend/package-lock.json* ./
RUN npm install --registry=https://registry.npmmirror.com
COPY ai-coding-frontend/ .
RUN npm run build

FROM nginx:alpine
COPY --from=build /build/dist /usr/share/nginx/html
COPY ai-coding-frontend/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
