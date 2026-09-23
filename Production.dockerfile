FROM dhi.io/amazoncorretto:21-debian12

COPY target/universal/stage/ /usr/share/amiable/

EXPOSE 9101

ENTRYPOINT [ "java", "-Dpidfile.path=/dev/null", "-Dconfig.file=/etc/amiable.conf", "-XX:MaxRAMPercentage=50.0", "-XX:InitialRAMPercentage=50.0", "-XX:MaxMetaspaceSize=300m", "-Xlog:gc*:stdout", "-cp", "/usr/share/amiable/conf/:/usr/share/amiable/lib/*", "play.core.server.ProdServerStart" ]
