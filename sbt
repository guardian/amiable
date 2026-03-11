#!/usr/bin/env bash

# Debug option
DEBUG_PARAMS=""
for arg in "$@"
do
    if [ "$arg" == "--debug" ]; then
      echo "setting java process as debuggable"
      DEBUG_PARAMS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=1056"
      shift
    fi
done

java $DEBUG_PARAMS \
    -Xms1024M -Xmx2048M -Xss1M \
    -Dconfig.file=$HOME/.gu/amiable/amiable.local.conf \
    -jar sbt-launch.jar "$@"
