# syntax=docker/dockerfile:1

FROM maven:3.9.9-eclipse-temurin-8 AS build
ARG MAVEN_OPTS="-Xmx256m"
ENV MAVEN_OPTS=$MAVEN_OPTS
WORKDIR /workspace
COPY . .
RUN mvn -s /workspace/settings.xml -DskipTests package

FROM eclipse-temurin:8-jre-jammy
WORKDIR /app
ENV TZ=Asia/Shanghai
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /workspace/target/*.jar /app/app.jar
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
