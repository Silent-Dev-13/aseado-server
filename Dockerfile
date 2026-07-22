# ── Build stage ──
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Cache dependencies separately from source so `docker build` doesn't
# re-download the internet every time a .java file changes.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ── Run stage ──
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S aseado && adduser -S aseado -G aseado

COPY --from=build /build/target/aseado-server.jar app.jar

USER aseado
EXPOSE 8081

# DATABASE_URL / DATABASE_USERNAME / DATABASE_PASSWORD and ADMIN_KEY must
# be overridden at deploy time — application.yaml's defaults are only
# there so the container doesn't refuse to boot in local testing.
ENV PORT=8081

ENTRYPOINT ["java", "-jar", "app.jar"]
