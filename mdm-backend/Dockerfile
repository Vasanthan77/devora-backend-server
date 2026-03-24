FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /build

COPY pom.xml ./
COPY .mvn ./.mvn
COPY mvnw ./
RUN chmod +x mvnw

COPY src ./src
RUN ./mvnw -B -DskipTests clean package
RUN cp target/*.jar app.jar

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=builder /build/app.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["/bin/sh", "-c", "java -Dserver.port=${PORT:-8080} ${JAVA_OPTS} -jar /app/app.jar"]
