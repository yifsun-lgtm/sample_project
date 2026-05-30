# ============================================================
# ProQuip WildFly アプリケーションサーバー
# Stage 1: Maven で EAR をビルド
# Stage 2: WildFly にデプロイ
# ============================================================

# ----- Stage 1: Maven Build -----
FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /build

COPY proquip-parent/pom.xml proquip-parent/pom.xml
COPY proquip-common/pom.xml proquip-common/pom.xml
COPY proquip-ejb/pom.xml proquip-ejb/pom.xml
COPY proquip-web/pom.xml proquip-web/pom.xml
COPY proquip-ear/pom.xml proquip-ear/pom.xml
COPY proquip-db/pom.xml proquip-db/pom.xml

RUN cd proquip-parent && mvn dependency:go-offline -DskipTests -pl ../proquip-ear -am || true

COPY proquip-common/src proquip-common/src
COPY proquip-ejb/src proquip-ejb/src
COPY proquip-web/src proquip-web/src

RUN cd proquip-parent && mvn clean package -DskipTests -pl ../proquip-ear -am

# ----- Stage 2: WildFly -----
FROM quay.io/wildfly/wildfly:30.0.1.Final-jdk17

LABEL maintainer="proquip-dev@example.com"
LABEL description="ProQuip アプリケーションサーバー (WildFly 30)"

USER root

ARG POSTGRESQL_DRIVER_VERSION=42.7.1
ADD --chown=jboss:root \
    https://jdbc.postgresql.org/download/postgresql-${POSTGRESQL_DRIVER_VERSION}.jar \
    /opt/jboss/wildfly/modules/org/postgresql/main/postgresql-jdbc.jar

COPY --chown=jboss:root docker/wildfly/modules/org/postgresql/main/module.xml \
    /opt/jboss/wildfly/modules/org/postgresql/main/module.xml

COPY --chown=jboss:root docker/wildfly/configure-wildfly.cli /tmp/configure-wildfly.cli

USER jboss

RUN /opt/jboss/wildfly/bin/jboss-cli.sh \
    --file=/tmp/configure-wildfly.cli && \
    rm -f /tmp/configure-wildfly.cli

RUN /opt/jboss/wildfly/bin/add-user.sh admin admin123! --silent

COPY --from=builder --chown=jboss:root \
    /build/proquip-ear/target/proquip.ear \
    /opt/jboss/wildfly/standalone/deployments/proquip.ear

USER root
RUN chgrp -R 0 /opt/jboss/wildfly && \
    chmod -R g=u /opt/jboss/wildfly
USER 1001

EXPOSE 8080 9990

CMD ["/opt/jboss/wildfly/bin/standalone.sh", \
     "-c", "standalone-full.xml", \
     "-b", "0.0.0.0", \
     "-bmanagement", "0.0.0.0"]
