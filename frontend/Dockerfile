# ==============================================================================
# Phase 1: Build Stage (Node.js 20 Alpine)
# ==============================================================================
FROM node:20-alpine AS builder

WORKDIR /app

COPY package*.json ./
RUN npm ci || npm install

COPY . .
RUN npm run build

# ==============================================================================
# Phase 2: Web Server Stage (Nginx Alpine)
# ==============================================================================
FROM nginx:alpine

# কাস্টম রিভার্স প্রক্সি কনফিগ কপি
COPY nginx.conf /etc/nginx/conf.d/default.conf

# বিল্ডার থেকে বিল্ট স্ট্যাটিক এইচটিএমএল/জেএস কপি
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
