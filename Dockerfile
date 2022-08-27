FROM amazoncorretto:17-alpine-jdk

RUN mkdir /opt/segovia
RUN mkdir /var/opt/segovia/

COPY build/libs/segovia.jar /opt/segovia/

ENTRYPOINT ["/opt/java-17/bin/java", "-Dspring.profiles.active=production", "-jar", "segovia.jar"]