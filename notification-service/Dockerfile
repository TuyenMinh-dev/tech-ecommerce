FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
ARG JAR_FILE=target/*-runner.jar
COPY ${JAR_FILE} app.jar
ENV JAVA_OPTS="-Xms64m -Xmx128m"
EXPOSE 8084
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
