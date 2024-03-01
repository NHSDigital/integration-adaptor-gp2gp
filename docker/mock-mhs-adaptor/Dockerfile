FROM gradle:8.5-jdk17 AS build

COPY --chown=gradle:gradle mock-mhs-adaptor /home/gradle/mock-mhs-adaptor

WORKDIR /home/gradle/mock-mhs-adaptor

RUN gradle classes

FROM build AS package

RUN gradle --build-cache bootJar

FROM eclipse-temurin:17-jre-focal

EXPOSE 8081

RUN mkdir /app

COPY --from=package /home/gradle/mock-mhs-adaptor/build/libs/mock-mhs-service.jar /app/mock-mhs-service.jar

USER 65534

ENTRYPOINT ["java", "-jar", "/app/mock-mhs-service.jar"]
