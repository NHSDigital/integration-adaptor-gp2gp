FROM gradle:jdk11

COPY --chown=gradle:gradle mock-mhs-service /home/gradle/mock-mhs-service

WORKDIR /home/gradle/mock-mhs-service

RUN gradle classes

ENTRYPOINT ["gradle", "check"]
