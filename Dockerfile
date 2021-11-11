FROM gradle:6.9.1-jdk11-alpine AS builder
RUN mkdir -p /app/shacl
COPY . /app/shacl
WORKDIR /app/shacl
RUN gradle build
RUN gradle shadowJar

FROM builder
RUN mkdir -p /app/data
RUN mkdir -p /app/local
COPY --from=builder /app/shacl/build/libs/*-all.jar /app/app.jar

ENTRYPOINT ["java","-jar", "/app/app.jar"]
CMD ["configFile"]
