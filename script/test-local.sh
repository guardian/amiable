#/bin/bash

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

aws s3 cp \
  "s3://${DIST_BUCKET}/${STACK}/${STAGE}/${APP}/conf/amiable.conf" \
  "$TMP_DIR/amiable.conf"

aws s3 cp \
  "s3://${DIST_BUCKET}/${STACK}/${STAGE}/${APP}/conf/amiable-service-account-cert.json" \
  "$TMP_DIR/amiable-service-account-cert.json"

docker run --rm \
  -p 9000:9000 \
  --mount type=bind,source="$TMP_DIR/amiable.conf",target=/etc/amiable.conf,readonly \
  --mount type=bind,source="$TMP_DIR/amiable-service-account-cert.json",target=/amiable/amiable-service-account-cert.json,readonly \
  -v "$HOME/.aws:/root/.aws:ro" \
  amiable:local

