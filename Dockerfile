FROM markhobson/maven-chrome:jdk-17
EXPOSE 8080

WORKDIR root/
ARG JAR_FILE=repricer-*.jar
COPY ${JAR_FILE} ./app.jar

ENTRYPOINT ["java", "-server", "-Xms256M", "-Xmx1512M",\
            "-XX:+UnlockDiagnosticVMOptions", "-XX:+UseContainerSupport",\
            "-XX:+HeapDumpOnOutOfMemoryError", "-XX:HeapDumpPath=dump.hprof", "-Djava.security.egd=/dev/zrandom",\
            "-jar", "/root/app.jar"]
