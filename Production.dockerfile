FROM dhi.io/eclipse-temurin:21-alpine3.23-dev AS production

USER root

# Runtime tools needed by the startup script / sbt-native-packager launcher.
RUN apk add --no-cache aws-cli bash

# Bake the staged application into the image.
COPY target/universal/stage/ /usr/share/amiable/

COPY docker/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

EXPOSE 9000

ENTRYPOINT ["/entrypoint.sh"]