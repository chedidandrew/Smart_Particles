#!/usr/bin/env sh

APP_HOME=$(cd "$(dirname "$0")" && pwd)

DEFAULT_JVM_OPTS='-Xmx64m -Xms64m'

if [ -n "$JAVA_HOME" ] ; then
  JAVA_CMD="$JAVA_HOME/bin/java"
else
  JAVA_CMD="java"
fi

CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

exec "$JAVA_CMD" $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
