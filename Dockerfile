FROM gradle:7.5-jdk17-alpine AS builder
RUN mkdir -p /app/shacl
COPY . /app/shacl
WORKDIR /app/shacljava
RUN gradle build
RUN gradle shadowJar

FROM builder
RUN mkdir -p /app/data
RUN mkdir -p /app/local
COPY --from=builder /app/shacl/build/libs/*-all.jar /app/app.jar

ENTRYPOINT ["java","-jar", "/app/app.jar"]
CMD ["configFile"]
