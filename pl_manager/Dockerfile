# we use the image from maven to install the application dependencies
FROM maven:3.8.4-openjdk-17-slim as maven

WORKDIR /usr/application
COPY / .

# build jar
ENV MAVEN_OPTS=-Xss10M
RUN mvn package -Dmaven.test.skip

# we create the real image with only java (alpine is not totally supported yet)
FROM openjdk:17

# define and create working directory
WORKDIR /usr/application

# we copy the dependencies from the previous image
COPY --from=maven /usr/application/target/pl_manager-*-jar-with-dependencies.jar pl_manager.jar

# change directory permissions to allow access for not root users
RUN chmod -R a+rwX .

# specify command to start the container
ENTRYPOINT ["java", "-jar", "pl_manager.jar"]
