FROM openjdk:17-alpine
ARG JAR_FILE=build/libs/*.jar
COPY . .
RUN ./gradlew build
COPY ${JAR_FILE} app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]