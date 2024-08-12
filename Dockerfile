FROM openjdk:21-slim

# Set the working directory in the container
WORKDIR /app

# Copy the jar file into the container
COPY target/*.jar app.jar

# Run the jar file
ENTRYPOINT ["java","-jar","app.jar"]