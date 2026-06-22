FROM eclipse-temurin:21.0.11_10-jre-jammy@sha256:199aebeb3adcde4910695cdebfe782ada38dadb6cc8013159b58d3724451befd

ARG JAR_FILE=application.jar
ARG BUILD_VERSION=dev
ARG BUILD_REVISION=unknown
ARG BUILD_CREATED=""
ARG BUILD_SOURCE=""

LABEL org.opencontainers.image.title="Synapse Gateway" \
      org.opencontainers.image.description="Synapse Platform reactive API gateway" \
      org.opencontainers.image.version="${BUILD_VERSION}" \
      org.opencontainers.image.revision="${BUILD_REVISION}" \
      org.opencontainers.image.created="${BUILD_CREATED}" \
      org.opencontainers.image.source="${BUILD_SOURCE}"

WORKDIR /opt/synapse

# 精确版本的 Jammy/glibc JRE 已包含 curl，避免 Alpine/musl 差异和额外软件安装层。
RUN groupadd --gid 10001 synapse \
    && useradd --uid 10001 --gid 10001 --no-create-home --home-dir /opt/synapse \
        --shell /usr/sbin/nologin synapse \
    && chown 10001:10001 /opt/synapse

COPY --chown=10001:10001 ${JAR_FILE} /opt/synapse/application.jar

USER 10001:10001
EXPOSE 20000

# JAVA_TOOL_OPTIONS 由 JVM 原生解析；exec form 确保 SIGTERM 直接到达 Java 进程。
ENTRYPOINT ["java", "-jar", "/opt/synapse/application.jar"]
