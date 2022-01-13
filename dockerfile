FROM openjdk:17-alpine
ARG JAR_NAME
RUN echo $JAR_NAME
RUN ls
COPY ./$JAR_NAME /app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]