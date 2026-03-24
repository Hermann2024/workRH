FROM eclipse-temurin:21-jre
ARG JAR_FILE=target/config-server-1.0.0-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
