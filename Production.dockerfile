FROM dhi.io/eclipse-temurin:21-alpine3.23-dev AS production

USER root

# /opt/amiable is mapped in by ECS using GuCdk

ln -sf /opt/amiable/amiable-service-account-cert.json /amiable/amiable-service-account-cert.json

ln -sf /opt/amiable/amiable.conf /etc/amiable.conf

EXPOSE 9000

ENTRYPOINT ["/usr/share/amiable/bin/amiable"]
