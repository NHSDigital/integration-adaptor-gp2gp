FROM gradle:jdk11-hotspot AS build

COPY --chown=gradle:gradle service /home/gradle/service

WORKDIR /home/gradle/service

RUN gradle classes

FROM build AS package

RUN gradle --build-cache bootJar

FROM adoptopenjdk:11-jre-hotspot

EXPOSE 8080 5005

RUN mkdir /app

COPY --from=package /home/gradle/service/build/libs/gp2gp.jar /app/gp2gp.jar

USER 65534

ENTRYPOINT ["java", "-cp", "/app/gp2gp.jar", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005", "-Dloader.main=uk.nhs.adaptors.gp2gp.Gp2gpApplication", "org.springframework.boot.loader.PropertiesLauncher"]

