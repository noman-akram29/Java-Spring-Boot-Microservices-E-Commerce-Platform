#!/bin/sh
set -e

# Inject MySQL root password if the secret is mounted (inventory-service, order-service)
if [ -f /run/secrets/mysql_root_password ]; then
    export SPRING_DATASOURCE_PASSWORD="$(cat /run/secrets/mysql_root_password)"
fi

# Inject Mongo root password into the connection URI if the secret is mounted (product-service)
if [ -f /run/secrets/mongo_root_password ] && [ -n "$SPRING_DATA_MONGODB_URI" ]; then
    MONGO_PW="$(cat /run/secrets/mongo_root_password)"
    export SPRING_DATA_MONGODB_URI="$(echo "$SPRING_DATA_MONGODB_URI" | sed "s/__MONGO_PASSWORD__/$MONGO_PW/")"
fi

exec java -XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -jar /app/app.jar