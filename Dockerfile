FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# Copier le wrapper Maven
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

RUN chmod +x mvnw

# Télécharger les dépendances
RUN ./mvnw dependency:go-offline

# Copier le code source
COPY src ./src

# Build l'application
RUN ./mvnw clean package -DskipTests

# Exposer le port
EXPOSE 8080

# Lancer l'application
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "target/elec-business.jar"]