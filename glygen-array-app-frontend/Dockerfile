#FROM java:8
#EXPOSE 8080
#ADD /target/demo.jar demo.jar
#ENTRYPOINT ["java","-jar","demo.jar"]

FROM openjdk:8-jre
MAINTAINER Nobuyuki Aoki <aoki@sparqlite.com>

ENTRYPOINT ["/usr/bin/java", "-jar", "/usr/share/myservice/myservice.jar"]

# Add Maven dependencies (not shaded into the artifact; Docker-cached)
#ADD target/lib           /usr/share/myservice/lib
# Add the service itself
ARG JAR_FILE
ADD target/${JAR_FILE} /usr/share/service.jar
ENTRYPOINT ["java","-jar","/usr/share/service.jar"]
