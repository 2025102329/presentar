# Primera etapa: compila el proyecto dentro de una imagen con Maven.
FROM maven:3.9.16-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

# Segunda etapa: deja una imagen pequeña solo para ejecutar el sistema.
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/fragancias-ia-1.0.0.jar app.jar
EXPOSE 10000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
