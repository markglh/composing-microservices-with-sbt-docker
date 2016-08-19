#!/bin/bash

set -e
set -x

APP_OPTS="-d64 \
          -server \
          -XX:MaxGCPauseMillis=500 \
          -XX:+UseStringDeduplication \
          -Xmx1024m \
          -XX:+UseG1GC \
          -XX:ConcGCThreads=4 -XX:ParallelGCThreads=4 \
          -Dcom.sun.management.jmxremote.port=9999 -Dcom.sun.management.jmxremote.ssl=false \
          -Dcom.sun.management.jmxremote.authenticate=false \
          -Dcom.sun.management.jmxremote.local.only=false \
          -Dcom.sun.management.jmxremote.rmi.port=9999 \
          -Dcom.sun.management.jmxremote=true \
         "

java ${APP_OPTS} -cp ${APP_BASE}/conf -jar ${APP_BASE}/aggregator-service.jar
