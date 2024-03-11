FROM gradle:8.5-jdk21

COPY --chown=gradle:gradle wiremock /home/gradle/wiremock

WORKDIR /home/gradle/wiremock

RUN ["gradle", "jar"]

ENTRYPOINT ["java", "-jar", "build/libs/wiremock.jar"]
