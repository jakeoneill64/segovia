FROM alpine:3.14

RUN mkdir /opt/segovia
RUN mkdir /var/opt/segovia/
RUN wget https://download.oracle.com/java/17/archive/jdk-17.0.4.1_linux-x64_bin.tar.gz
RUN tar -xvzf jdk-17.0.4.1_linux-x64_bin.tar.gz
RUN mv jdk-17.0.4.1 /opt/java-17


COPY build/libs/segovia.jar /opt/segovia/

ENTRYPOINT ["/opt/java-17/bin/java", "-Dspring.profiles.active=production", "-jar", "segovia.jar"]