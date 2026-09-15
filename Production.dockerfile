FROM dhi.io/eclipse-temurin:21-alpine3.23-dev AS production

USER root

EXPOSE 9000

ENTRYPOINT ["/usr/share/amiable/bin/amiable"]
