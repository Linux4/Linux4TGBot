FROM maven:latest AS build
WORKDIR /src

ADD . /src
RUN mvn clean install

FROM eclipse-temurin:25-jre-alpine
WORKDIR /data

COPY --from=build /src/target/Linux4TGBot.jar /app/

CMD ["java", "-jar", "/app/Linux4TGBot.jar"]
