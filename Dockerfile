FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN apt-get update && \
    apt-get -y install --no-install-recommends curl fping iproute2 net-tools libssl3 zlib1g libstdc++6 libseccomp2 python3 nftables && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

COPY deploy/telegram-entrypoint.py /usr/local/lib/thb/entrypoint.py
ENTRYPOINT ["python3", "/usr/local/lib/thb/entrypoint.py"]
