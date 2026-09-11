#!/bin/sh
set -e

aws --region eu-west-1 \
  s3 cp \
  "s3://${DIST_BUCKET}/${STACK}/${STAGE}/${APP}/conf/amiable-service-account-cert.json" \
  /amiable/

aws --region eu-west-1 \
  s3 cp \
  "s3://${DIST_BUCKET}/${STACK}/${STAGE}/${APP}/conf/amiable.conf" \
  /etc/

AMIABLE_BIN="/opt/docker/bin/amiable"
if [ ! -x "$AMIABLE_BIN" ]; then
  AMIABLE_BIN="/usr/share/amiable/bin/amiable"
fi

exec "$AMIABLE_BIN"
