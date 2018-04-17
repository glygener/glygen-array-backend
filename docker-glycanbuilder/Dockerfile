FROM tomcat:8.0
MAINTAINER Nobuyuki Aoki "aokinobu@gmail.com"

RUN apt-get update && apt-get install -y wget && rm -rf /var/lib/apt/lists/*
ADD /server.xml $CATALINA_HOME/conf/server.xml
RUN wget --quiet http://glycoinfo.org/GlycanBuilder.war -O $CATALINA_HOME/webapps/GlycanBuilder.war

EXPOSE 80
