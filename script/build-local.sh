#!/bin/bash

export BUILD_NUMBER=LOCAL
docker buildx build -t amiable:local --platform linux/arm64 -f Production.dockerfile .
