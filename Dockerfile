# 1. Get a lightweight Linux environment with Java 21 installed
FROM eclipse-temurin:21-jre-alpine

# 2. Add author label (Optional, but must come AFTER the FROM line!)
LABEL authors="rosti"

# 3. Create a folder named /app inside the container
WORKDIR /app

# 4. Copy the .jar file from your PC and rename it to app.jar inside the container
COPY target/*.jar app.jar

# 5. Open port 8080 so we can access the web app
EXPOSE 8080

# 6. Tell the container to run the app when it starts
ENTRYPOINT ["java", "-jar", "app.jar"]