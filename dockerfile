FROM openjdk:17-alpine
ARG JAR_NAME
COPY ./$JAR_NAME /app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]