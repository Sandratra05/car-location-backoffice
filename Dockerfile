# Utilise Eclipse Temurin JDK 17 comme base
FROM eclipse-temurin:17-jdk

# Définit le répertoire de travail
WORKDIR /app

# Installe Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# Copie tout le projet (pom parent + modules)
COPY pom.xml .
COPY ririnina ririnina
COPY carlocation carlocation

# Installe d'abord ririnina dans le repo local, puis build le projet complet
RUN mvn install -DskipTests

# Expose le port 8080 (par défaut pour Tomcat)
EXPOSE 8080

# Lance l'application
CMD ["java", "-jar", "carlocation/target/carlocation-bo.jar"]