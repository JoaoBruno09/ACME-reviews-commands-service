FROM openjdk:17
EXPOSE 8080
ADD target/reviews-service-commands.jar reviews-service-commands.jar
ENTRYPOINT [ "java", "-jar", "/reviews-service-commands.jar"]