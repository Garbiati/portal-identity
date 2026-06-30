# Imagem de PRODUÇÃO do portal-identity (Keycloak) — multi-stage.
#
# ⚠️ ESQUELETO p/ a fatia de deploy (P-006). NÃO é usado no DEV (que roda via docker-compose +
#    start-dev). Aqui empacotamos providers + tema + driver do Cloud SQL e fazemos `kc.sh build`
#    para rodar `start --optimized` no Cloud Run. Detalhes (versões, realm) confirmados no deploy.
#
# Contexto de build = a raiz deste repo (services/portal-identity).

# ---- Stage 1: compila os authenticators (CPF/telefone + OTP) -----------------------------------
FROM maven:3-eclipse-temurin-21 AS providers
WORKDIR /work
COPY providers/login-cpf-telefone/ ./login-cpf-telefone/
RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -f login-cpf-telefone/pom.xml -DskipTests package

# ---- Stage 2: kc.sh build (otimiza p/ Postgres + providers + tema) ------------------------------
FROM quay.io/keycloak/keycloak:26.0 AS builder
ENV KC_DB=postgres
ENV KC_HEALTH_ENABLED=true

# Authenticators próprios.
COPY --from=providers /work/login-cpf-telefone/target/login-cpf-telefone.jar /opt/keycloak/providers/
# Tema de login (identidade Doctor-Hub).
COPY themes/ /opt/keycloak/themes/
# Driver/SocketFactory do Cloud SQL (JDBC sem IP público). ⚠️ confirmar a versão no deploy.
ADD https://repo1.maven.org/maven2/com/google/cloud/sql/postgres-socket-factory/1.21.0/postgres-socket-factory-1.21.0-jar-with-dependencies.jar \
    /opt/keycloak/providers/postgres-socket-factory.jar

RUN /opt/keycloak/bin/kc.sh build

# ---- Stage 3: runtime ---------------------------------------------------------------------------
FROM quay.io/keycloak/keycloak:26.0
COPY --from=builder /opt/keycloak/ /opt/keycloak/
# start/optimized + parâmetros de runtime vêm do Cloud Run (KC_DB_URL, KC_HOSTNAME, secrets…).
ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start", "--optimized"]
