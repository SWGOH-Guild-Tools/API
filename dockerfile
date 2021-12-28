FROM openjdk:17-alpine
COPY /build/libs/SWGraphQL-0.0.1.jar /app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]