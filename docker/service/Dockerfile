FROM gradle:jdk11 AS build

COPY --chown=gradle:gradle service /home/gradle/service

WORKDIR /home/gradle/service

RUN ./gradlew compileJava

FROM build AS package

RUN ./gradlew --build-cache bootJar

FROM adoptopenjdk/openjdk11:jre

EXPOSE 8080

RUN mkdir /app

COPY --from=package /home/gradle/service/build/libs/gp2gp.jar /app/gp2gp.jar

USER 65534

ENTRYPOINT ["java", "-jar", "/app/gp2gp.jar"]
