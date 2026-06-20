FROM eclipse-temurin:21-jre-alpine

ARG JAR_FILE=synapse-gateway-platform/target/synapse-gateway-platform-0.1.0-SNAPSHOT.jar
ARG OCI_SOURCE=""
ARG OCI_REVISION="unknown"
ARG OCI_VERSION="dev"
ARG OCI_CREATED=""

LABEL org.opencontainers.image.source="${OCI_SOURCE}" \
      org.opencontainers.image.revision="${OCI_REVISION}" \
      org.opencontainers.image.version="${OCI_VERSION}" \
      org.opencontainers.image.created="${OCI_CREATED}"

WORKDIR /opt/synapse

# 使用固定 UID/GID 的非 root 账户，便于未来映射 Kubernetes securityContext。
RUN addgroup -S -g 10001 synapse \
    && adduser -S -D -H -u 10001 -G synapse synapse

COPY --chown=synapse:synapse ${JAR_FILE} /opt/synapse/application.jar

USER 10001:10001
EXPOSE 20000

# JAVA_TOOL_OPTIONS 由 JVM 原生解析，无需 shell 展开，SIGTERM 可直接传递给 Java 进程。
ENTRYPOINT ["java", "-jar", "/opt/synapse/application.jar"]
