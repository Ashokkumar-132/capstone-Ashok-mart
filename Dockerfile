# Build and run AshokMart without changing its Maven WAR/Tomcat architecture.
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B clean package

FROM tomcat:9.0-jdk17-temurin
ENV CATALINA_OPTS="-Djava.awt.headless=true"
RUN rm -rf /usr/local/tomcat/webapps/ROOT \
    && mkdir -p /opt/ashokmart-data
COPY --from=build /build/target/AshokMart.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080
CMD ["catalina.sh", "run"]
