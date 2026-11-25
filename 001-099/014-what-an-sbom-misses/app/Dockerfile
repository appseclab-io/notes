FROM node:22.23.4-alpine

# 9. A system package, added to the ones the base image already has
RUN apk add --no-cache curl

# 10. A binary downloaded with curl: no package manager knows it is there
RUN curl -fsSL -o /usr/local/bin/jq \
      https://github.com/jqlang/jq/releases/download/jq-1.7.1/jq-linux-arm64 \
 && chmod +x /usr/local/bin/jq

WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --omit=dev --ignore-scripts
COPY . .

USER node
CMD ["node", "server.js"]
