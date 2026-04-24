FROM eclipse-temurin:17-jre
RUN apt-get update && apt-get install -y --no-install-recommends jq && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY target/quarkus-app/lib/ /app/lib/
COPY target/quarkus-app/*.jar /app/
COPY target/quarkus-app/app/ /app/app/
COPY target/quarkus-app/quarkus/ /app/quarkus/
COPY run.sh /app/run.sh
RUN chmod +x /app/run.sh
EXPOSE 8080
ENTRYPOINT ["/app/run.sh"]
