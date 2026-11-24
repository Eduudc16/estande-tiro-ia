# Imagem para hospedar no Render (ou qualquer servico com Docker). As chaves NAO entram aqui:
# sao cadastradas como variaveis de ambiente no painel do servico.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
COPY assets assets
COPY audio audio
ENV SPRING_PROFILES_ACTIVE=render
EXPOSE 8080
CMD ["java", "-Xmx384m", "-jar", "app.jar"]
