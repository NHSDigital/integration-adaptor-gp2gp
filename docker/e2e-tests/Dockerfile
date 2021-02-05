FROM gradle:jdk11

COPY --chown=gradle:gradle e2e-tests /home/gradle/e2e-tests

WORKDIR /home/gradle/e2e-tests

RUN gradle testClasses

# USER 65534

ENTRYPOINT ["gradle", "check"]