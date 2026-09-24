#!/bin/sh
set -e

exec java \
  -XX:MaxRAMPercentage=75 \
  -XX:+ExitOnOutOfMemoryError \
  -jar /app/app.jar
