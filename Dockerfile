FROM gradle:jdk11 as cache
RUN mkdir -p /home/gradle/cache_home
ENV GRADLE_USER_HOME /home/gradle/cache_home
COPY build.gradle /home/gradle/src/
WORKDIR /home/gradle/src
RUN gradle -b build.gradle clean build -i --stacktrace

FROM gradle:jdk11 AS build
COPY --from=cache /home/gradle/cache_home /home/gradle/.gradle
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle --no-daemon -b build.gradle bootJar -i --stacktrace

FROM adoptopenjdk/openjdk11-openj9:jre

EXPOSE 8080

RUN mkdir /app
RUN mkdir /truststore

COPY --from=build /home/gradle/src/build/libs/*.jar /app/integration-adaptor-gp2gp.jar

ENTRYPOINT ["java", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCGroupMemoryLimitForHeap", "-Djava.security.egd=file:/dev/./urandom","-jar","/app/integration-adaptor-gp2gp.jar"]