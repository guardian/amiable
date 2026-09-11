#/bin/bash

docker \
  run \
  --rm \
  -p 9000:9000 \
  -e AWS_PROFILE=deployTools \
  -e DIST_BUCKET=deploy-tools-dist \
  -e STACK=deploy \
  -e STAGE=CODE \
  -e APP=amiable \
  -v "$HOME/.aws:/root/.aws:ro" \
  amiable:1.0-SNAPSHOT
