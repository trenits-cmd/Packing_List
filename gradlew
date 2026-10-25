#!/usr/bin/env sh

DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
APP_HOME="$DIR"
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

if [ ! -f "$CLASSPATH" ]; then
  echo "Gradle wrapper JAR not found: $CLASSPATH" >&2
  exit 1
fi

JAVA_CMD=${JAVA_HOME:+$JAVA_HOME/bin/java}
if [ -z "$JAVA_CMD" ]; then
  JAVA_CMD=java
fi

exec "$JAVA_CMD" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
