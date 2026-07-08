FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN ./mvnw -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
ENV JAVA_OPTS="-Dquarkus.http.host=0.0.0.0"
COPY --from=build /app/target/quarkus-app/ /app/
EXPOSE 8080
CMD ["sh", "-c", "java $JAVA_OPTS -jar /app/quarkus-run.jar"]
