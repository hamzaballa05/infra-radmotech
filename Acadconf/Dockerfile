
# 1
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build 

#5
WORKDIR /app

# 6
COPY pom.xml .

#7
RUN mvn dependency:go-offline -q

#8
COPY src ./src

#9
RUN mvn package -DskipTests -q

###################

# 2
FROM eclipse-temurin:21-jre-alpine 

# 3
WORKDIR /app 

# 4 
RUN mkdir -p /tmp/uploads

#10
COPY --from=build /app/target/acadconf-1.0.0.jar app.jar


EXPOSE 8080
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
