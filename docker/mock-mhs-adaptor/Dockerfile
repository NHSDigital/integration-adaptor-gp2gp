FROM gradle:jdk11

COPY --chown=gradle:gradle mock-mhs-adaptor /home/gradle/mock-mhs-adaptor

WORKDIR /home/gradle/mock-mhs-adaptor

RUN gradle classes

FROM build AS package

RUN gradle --build-cache bootJar

FROM adoptopenjdk/openjdk11:jre

EXPOSE 8081

RUN mkdir /app

COPY --from=package /home/gradle/service/build/libs/mock-mhs-adaptor.jar /app/mock-mhs-adaptor.jar

USER 65534

ENTRYPOINT ["java", "-jar", "/app/mock-mhs-adaptor.jar"]