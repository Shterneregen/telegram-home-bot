FROM openjdk:11-jre-slim-buster

ENV SERVER_PORT=8080
EXPOSE ${SERVER_PORT}

WORKDIR application
COPY build/libs/*.jar app.jar

RUN apt-get update && \
    apt-get -y install fping &&  \
    apt-get -y install iproute2 &&  \
    apt-get clean

ENTRYPOINT ["java", "-jar","./app.jar"]
