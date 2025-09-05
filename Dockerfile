FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN apt-get update && \
    apt-get -y install --no-install-recommends fping iproute2 net-tools && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

ENTRYPOINT ["java","-jar","/app/app.jar"]
