# Stage 1: Build Spring Boot executable JAR
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY backend/pom.xml backend/pom.xml

RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

COPY backend/src backend/src
RUN MVNW_VERBOSE=true ./mvnw -pl backend -am package -DskipTests -B

# Stage 2: Minimal JRE runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /app/backend/target/*.jar /app/app.jar

RUN mkdir -p /root/kizuna-data/avatars /root/kizuna-data/verification-docs

EXPOSE 8085

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS:--Xmx512m} -jar /app/app.jar"]
