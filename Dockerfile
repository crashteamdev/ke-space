FROM markhobson/maven-chrome:jdk-17
RUN apt update && \
    apt -y --no-install-recommends --no-install-suggests install libjemalloc2 curl && \
    apt clean && rm -rf /var/lib/apt/lists/*
ENV LD_PRELOAD=/usr/lib/x86_64-linux-gnu/libjemalloc.so.2
EXPOSE 8080

WORKDIR root/
ARG JAR_FILE=repricer-*.jar
COPY ${JAR_FILE} ./app.jar

ENTRYPOINT ["java", "-server", "-Xms256M", "-Xmx412M",\
            "-XX:+UnlockDiagnosticVMOptions", "-XX:+UseContainerSupport",\
            "-XX:+HeapDumpOnOutOfMemoryError", "-XX:HeapDumpPath=dump.hprof", "-Djava.security.egd=/dev/zrandom",\
            "-jar", "/root/app.jar"]
