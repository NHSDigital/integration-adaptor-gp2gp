FROM gradle:jdk11 AS build

COPY --chown=gradle:gradle service /home/gradle/service

WORKDIR /home/gradle/service

RUN gradle classes

FROM build AS package

RUN gradle --build-cache bootJar

FROM adoptopenjdk/openjdk11:jre

RUN apt-get -qq update && \
    apt-get -qq install -y --no-install-recommends \
        apt=2.0.2ubuntu0.2 \
        curl=7.68.0-1ubuntu2.4 \
        libcurl4=7.68.0-1ubuntu2.4 \
        libssl1.1=1.1.1f-1ubuntu2.1 \
        openssl=1.1.1f-1ubuntu2.1 \
        libp11-kit0=0.23.20-1ubuntu0.1 \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

EXPOSE 8080

RUN mkdir /app

COPY --from=package /home/gradle/service/build/libs/gp2gp.jar /app/gp2gp.jar

USER 65534

ENTRYPOINT ["java", "-jar", "/app/gp2gp.jar"]
