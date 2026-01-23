# ---------- Build stage ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom first for dependency caching
COPY pom.xml ./
RUN mvn -q -DskipTests dependency:go-offline

# Copy source and build jar
COPY src/ src/
RUN mvn -q -DskipTests package


# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the built jar
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
