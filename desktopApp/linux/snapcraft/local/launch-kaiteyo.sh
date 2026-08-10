#!/bin/bash
JAR=$(ls "$SNAP"/*.jar 2>/dev/null | head -1)
exec "$SNAP/usr/lib/jvm/java-17-openjdk-amd64/bin/java" -jar "$JAR" -Duser.home=$SNAP_USER_DATA
