FROM eclipse-temurin:21-jre

RUN addgroup --system emovieshop && \
    adduser --system --ingroup emovieshop emovieshop

WORKDIR /app

COPY target/*.jar app.jar

USER emovieshop

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
