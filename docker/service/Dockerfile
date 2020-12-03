FROM gradle:jdk14 AS build

COPY --chown=gradle:gradle ./service /home/gradle/service

WORKDIR /home/gradle/service

RUN ./gradlew check

RUN ./gradlew test --build-cache bootJar

FROM adoptopenjdk/openjdk14:jre

EXPOSE 8080

RUN mkdir /app

COPY --from=build /home/gradle/service/build/libs/gp2gp.jar /app/gp2gp.jar

USER 65534

ENTRYPOINT ["java", "-jar", "/app/gp2gp.jar", "--port", "8080"]
